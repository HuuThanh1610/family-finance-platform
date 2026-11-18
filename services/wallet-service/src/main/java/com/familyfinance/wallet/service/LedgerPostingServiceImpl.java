package com.familyfinance.wallet.service;

import com.familyfinance.wallet.domain.EntryType;
import com.familyfinance.wallet.entity.LedgerAccount;
import com.familyfinance.wallet.entity.LedgerEntry;
import com.familyfinance.wallet.entity.LedgerTransaction;
import com.familyfinance.wallet.entity.OutboxEvent;
import com.familyfinance.wallet.entity.WalletBalance;
import com.familyfinance.wallet.exception.BusinessException;
import com.familyfinance.wallet.exception.IdempotencyConflictException;
import com.familyfinance.wallet.exception.InsufficientBalanceException;
import com.familyfinance.wallet.exception.ResourceNotFoundException;
import com.familyfinance.wallet.repository.LedgerAccountRepository;
import com.familyfinance.wallet.repository.LedgerEntryRepository;
import com.familyfinance.wallet.repository.LedgerTransactionRepository;
import com.familyfinance.wallet.repository.OutboxEventRepository;
import com.familyfinance.wallet.repository.WalletBalanceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class LedgerPostingServiceImpl implements LedgerPostingService {

    private final LedgerTransactionRepository transactionRepository;
    private final LedgerEntryRepository entryRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final WalletBalanceRepository balanceRepository;
    private final OutboxEventRepository outboxEventRepository;

    public LedgerPostingServiceImpl(
            LedgerTransactionRepository transactionRepository,
            LedgerEntryRepository entryRepository,
            LedgerAccountRepository ledgerAccountRepository,
            WalletBalanceRepository balanceRepository,
            OutboxEventRepository outboxEventRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.entryRepository = entryRepository;
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.balanceRepository = balanceRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Override
    @Transactional
    public LedgerTransaction postBalancedTransaction(PostCommand command) {
        validateCommand(command);

        var existing = transactionRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            var sameRef = existing.get().getReferenceId().equals(command.referenceId());
            if (!sameRef) {
                throw new IdempotencyConflictException("Idempotency key already used with different reference");
            }
            return existing.get();
        }
        if (transactionRepository.findByReferenceId(command.referenceId()).isPresent()) {
            throw new IdempotencyConflictException("Reference id already used: " + command.referenceId());
        }

        long debitSum = command.entries().stream()
                .filter(e -> e.entryType() == EntryType.DEBIT)
                .mapToLong(EntryLine::amount)
                .sum();
        long creditSum = command.entries().stream()
                .filter(e -> e.entryType() == EntryType.CREDIT)
                .mapToLong(EntryLine::amount)
                .sum();
        if (debitSum != creditSum) {
            throw new BusinessException(
                    "Unbalanced journal: debit=" + debitSum + " credit=" + creditSum,
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }

        // Lock balances in deterministic ledger-account id order to reduce deadlocks
        List<UUID> accountIds = command.entries().stream()
                .map(EntryLine::ledgerAccountId)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();

        Map<UUID, LedgerAccount> accounts = new HashMap<>();
        Map<UUID, WalletBalance> lockedBalances = new HashMap<>();
        for (UUID accountId : accountIds) {
            LedgerAccount account = ledgerAccountRepository.findById(accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("LedgerAccount", accountId));
            accounts.put(accountId, account);
            if ("WALLET_LIABILITY".equals(account.getAccountCode())) {
                WalletBalance balance = balanceRepository.findByLedgerAccountIdForUpdate(accountId)
                        .orElseThrow(() -> new ResourceNotFoundException("WalletBalance for account " + accountId));
                lockedBalances.put(accountId, balance);
            }
        }

        // Apply projected liability changes and reject negative balances
        Map<UUID, Long> delta = new HashMap<>();
        for (EntryLine line : command.entries()) {
            LedgerAccount account = accounts.get(line.ledgerAccountId());
            if (!"WALLET_LIABILITY".equals(account.getAccountCode())) {
                continue;
            }
            // LIABILITY: CREDIT increases balance, DEBIT decreases
            long change = line.entryType() == EntryType.CREDIT ? line.amount() : -line.amount();
            delta.merge(line.ledgerAccountId(), change, Long::sum);
        }
        for (Map.Entry<UUID, Long> e : delta.entrySet()) {
            WalletBalance balance = lockedBalances.get(e.getKey());
            long next = balance.getBalanceAmount() + e.getValue();
            if (next < 0) {
                throw new InsufficientBalanceException(
                        "Insufficient balance on ledger account " + e.getKey()
                );
            }
        }

        Instant now = Instant.now();
        LedgerTransaction tx = new LedgerTransaction();
        tx.setFamilyId(command.familyId());
        if (command.primaryWalletId() != null) {
            accounts.values().stream()
                    .filter(a -> a.getWalletAccount() != null && command.primaryWalletId().equals(a.getWalletAccount().getId()))
                    .findFirst()
                    .ifPresent(a -> tx.setWalletAccount(a.getWalletAccount()));
        }
        tx.setReferenceId(command.referenceId());
        tx.setIdempotencyKey(command.idempotencyKey());
        tx.setDescription(command.description());
        tx.setTransactionType(command.transactionType());
        tx.setPostedAt(now);
        tx.setCreatedAt(now);

        for (EntryLine line : command.entries()) {
            if (line.amount() <= 0) {
                throw new BusinessException("Entry amount must be positive", HttpStatus.BAD_REQUEST);
            }
            LedgerEntry entry = new LedgerEntry();
            entry.setTransaction(tx);
            entry.setLedgerAccount(accounts.get(line.ledgerAccountId()));
            entry.setEntryType(line.entryType().name());
            entry.setAmount(line.amount());
            entry.setMemo(line.memo());
            entry.setCreatedAt(now);
            tx.getEntries().add(entry);
        }

        LedgerTransaction saved = transactionRepository.save(tx);

        for (Map.Entry<UUID, Long> e : delta.entrySet()) {
            WalletBalance balance = lockedBalances.get(e.getKey());
            balance.setBalanceAmount(balance.getBalanceAmount() + e.getValue());
            balance.setRecalculatedAt(now);
            balance.setUpdatedAt(now);
            balanceRepository.save(balance);
        }

        OutboxEvent outbox = new OutboxEvent();
        outbox.setEventType("wallet.ledger.posted");
        outbox.setAggregateType("ledger_transaction");
        outbox.setAggregateId(saved.getId());
        outbox.setPayload(Map.of(
                "transactionId", saved.getId().toString(),
                "familyId", command.familyId().toString(),
                "referenceId", command.referenceId(),
                "transactionType", command.transactionType(),
                "amountDebit", debitSum
        ));
        outbox.setCreatedAt(now);
        outbox.setStatus("PENDING");
        outboxEventRepository.save(outbox);

        return saved;
    }

    @Override
    @Transactional
    public LedgerTransaction reverseTransaction(UUID transactionId, String reversalReferenceId, String idempotencyKey) {
        LedgerTransaction original = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("LedgerTransaction", transactionId));
        List<LedgerEntry> originalEntries = entryRepository.findByTransactionId(transactionId);
        List<EntryLine> reversalLines = originalEntries.stream()
                .map(e -> new EntryLine(
                        e.getLedgerAccount().getId(),
                        EntryType.valueOf(e.getEntryType()) == EntryType.DEBIT ? EntryType.CREDIT : EntryType.DEBIT,
                        e.getAmount(),
                        "Reversal of " + original.getReferenceId()
                ))
                .toList();

        return postBalancedTransaction(new PostCommand(
                original.getFamilyId(),
                original.getWalletAccount() != null ? original.getWalletAccount().getId() : null,
                reversalReferenceId,
                idempotencyKey,
                "REVERSAL",
                "Reversal of " + original.getId(),
                reversalLines
        ));
    }

    private void validateCommand(PostCommand command) {
        if (command.entries() == null || command.entries().size() < 2) {
            throw new BusinessException("At least two journal entries required", HttpStatus.BAD_REQUEST);
        }
        Set<String> seen = new HashSet<>();
        for (EntryLine line : command.entries()) {
            if (line.amount() <= 0) {
                throw new BusinessException("Amount must be > 0", HttpStatus.BAD_REQUEST);
            }
            seen.add(line.ledgerAccountId() + ":" + line.entryType());
        }
        if (seen.isEmpty()) {
            throw new BusinessException("Invalid entries", HttpStatus.BAD_REQUEST);
        }
    }
}
