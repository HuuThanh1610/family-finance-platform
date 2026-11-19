package com.familyfinance.wallet.service;

import com.familyfinance.wallet.domain.EntryType;
import com.familyfinance.wallet.domain.WalletType;
import com.familyfinance.wallet.dto.AllowanceDisbursementCommand;
import com.familyfinance.wallet.dto.CreateWalletRequest;
import com.familyfinance.wallet.dto.LedgerEntryResponse;
import com.familyfinance.wallet.dto.LedgerTransactionResponse;
import com.familyfinance.wallet.dto.TopUpCreditRequest;
import com.familyfinance.wallet.dto.WalletBalanceResponse;
import com.familyfinance.wallet.dto.WalletResponse;
import com.familyfinance.wallet.dto.WalletTransferRequest;
import com.familyfinance.wallet.entity.LedgerAccount;
import com.familyfinance.wallet.entity.LedgerEntry;
import com.familyfinance.wallet.entity.LedgerTransaction;
import com.familyfinance.wallet.entity.OutboxEvent;
import com.familyfinance.wallet.entity.WalletAccount;
import com.familyfinance.wallet.entity.WalletBalance;
import com.familyfinance.wallet.exception.BusinessException;
import com.familyfinance.wallet.exception.InsufficientBalanceException;
import com.familyfinance.wallet.exception.PermanentAllowanceDisbursementException;
import com.familyfinance.wallet.exception.ResourceNotFoundException;
import com.familyfinance.wallet.repository.LedgerAccountRepository;
import com.familyfinance.wallet.repository.LedgerEntryRepository;
import com.familyfinance.wallet.repository.LedgerTransactionRepository;
import com.familyfinance.wallet.repository.OutboxEventRepository;
import com.familyfinance.wallet.repository.WalletAccountRepository;
import com.familyfinance.wallet.repository.WalletBalanceRepository;
import com.familyfinance.wallet.security.FamilyAuthorizationClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WalletApplicationService {

    public static final String LIABILITY_CODE = "WALLET_LIABILITY";
    public static final String PSP_CLEARING_CODE = "PSP_CLEARING";
    public static final String EVENT_ALLOWANCE_COMPLETED = "AllowanceDisbursementCompleted";
    public static final String EVENT_ALLOWANCE_FAILED = "AllowanceDisbursementFailed";
    public static final String IDEMPOTENCY_ALLOWANCE_PREFIX = "allowance-disbursement:";

    private final WalletAccountRepository walletAccountRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final WalletBalanceRepository walletBalanceRepository;
    private final LedgerTransactionRepository ledgerTransactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerPostingService ledgerPostingService;
    private final FamilyAuthorizationClient familyAuthorizationClient;
    private final OutboxEventRepository outboxEventRepository;

    public WalletApplicationService(
            WalletAccountRepository walletAccountRepository,
            LedgerAccountRepository ledgerAccountRepository,
            WalletBalanceRepository walletBalanceRepository,
            LedgerTransactionRepository ledgerTransactionRepository,
            LedgerEntryRepository ledgerEntryRepository,
            LedgerPostingService ledgerPostingService,
            FamilyAuthorizationClient familyAuthorizationClient,
            OutboxEventRepository outboxEventRepository
    ) {
        this.walletAccountRepository = walletAccountRepository;
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.walletBalanceRepository = walletBalanceRepository;
        this.ledgerTransactionRepository = ledgerTransactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.ledgerPostingService = ledgerPostingService;
        this.familyAuthorizationClient = familyAuthorizationClient;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public WalletResponse createWallet(CreateWalletRequest request, UUID userProfileId) {
        familyAuthorizationClient.requirePermission(request.familyId(), userProfileId, "wallet.transfer");
        if (request.walletType() == WalletType.MEMBER_WALLET && request.ownerMemberId() == null) {
            throw new BusinessException("ownerMemberId required for MEMBER_WALLET", HttpStatus.BAD_REQUEST);
        }
        Instant now = Instant.now();
        WalletAccount wallet = new WalletAccount();
        wallet.setFamilyId(request.familyId());
        wallet.setOwnerMemberId(request.ownerMemberId());
        wallet.setName(request.name());
        wallet.setCurrencyCode("VND");
        wallet.setWalletType(request.walletType().name());
        wallet.setStatus("ACTIVE");
        wallet.setCreatedAt(now);
        wallet.setUpdatedAt(now);
        wallet = walletAccountRepository.save(wallet);

        LedgerAccount liability = new LedgerAccount();
        liability.setWalletAccount(wallet);
        liability.setFamilyId(request.familyId());
        liability.setAccountCode(LIABILITY_CODE);
        liability.setName("Số dư ví (liability)");
        liability.setAccountKind("LIABILITY");
        liability.setNormalBalance("CREDIT");
        liability.setCreatedAt(now);
        liability.setUpdatedAt(now);
        liability = ledgerAccountRepository.save(liability);

        WalletBalance balance = new WalletBalance();
        balance.setLedgerAccount(liability);
        balance.setBalanceAmount(0L);
        balance.setVersion(0);
        balance.setRecalculatedAt(now);
        balance.setUpdatedAt(now);
        walletBalanceRepository.save(balance);

        ensurePspClearing(request.familyId(), now);

        return toWalletResponse(wallet, 0L);
    }

    @Transactional(readOnly = true)
    public List<WalletResponse> listWallets(UUID familyId, UUID userProfileId) {
        familyAuthorizationClient.requireMember(familyId, userProfileId);
        return walletAccountRepository.findByFamilyId(familyId).stream()
                .sorted(Comparator.comparing(WalletAccount::getCreatedAt))
                .map(wallet -> toWalletResponse(wallet, availableBalance(wallet.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(UUID walletId, UUID userProfileId) {
        WalletAccount wallet = requireWallet(walletId);
        familyAuthorizationClient.requireMember(wallet.getFamilyId(), userProfileId);
        return toWalletResponse(wallet, availableBalance(walletId));
    }

    @Transactional(readOnly = true)
    public WalletBalanceResponse getBalance(UUID walletId, UUID userProfileId) {
        WalletAccount wallet = requireWallet(walletId);
        familyAuthorizationClient.requireMember(wallet.getFamilyId(), userProfileId);
        LedgerAccount liability = requireLiability(walletId);
        WalletBalance balance = walletBalanceRepository.findByLedgerAccountId(liability.getId())
                .orElseThrow(() -> new ResourceNotFoundException("WalletBalance for " + walletId));
        return new WalletBalanceResponse(wallet.getId(), wallet.getCurrencyCode(), balance.getBalanceAmount(), balance.getVersion());
    }

    @Transactional(readOnly = true)
    public List<LedgerTransactionResponse> listTransactions(UUID walletId, UUID userProfileId) {
        WalletAccount wallet = requireWallet(walletId);
        familyAuthorizationClient.requireMember(wallet.getFamilyId(), userProfileId);
        return ledgerTransactionRepository.findByWalletAccountIdOrderByPostedAtDesc(walletId).stream()
                .map(this::toTransactionResponse)
                .toList();
    }

    @Transactional
    public LedgerTransactionResponse transfer(WalletTransferRequest request, UUID userProfileId) {
        if (request.sourceWalletId().equals(request.destinationWalletId())) {
            throw new BusinessException("Source and destination must differ", HttpStatus.BAD_REQUEST);
        }
        if (request.amount() <= 0) {
            throw new BusinessException("Amount must be positive", HttpStatus.BAD_REQUEST);
        }

        // Lock wallets in deterministic order
        List<UUID> ordered = List.of(request.sourceWalletId(), request.destinationWalletId()).stream()
                .sorted(Comparator.naturalOrder())
                .toList();
        for (UUID id : ordered) {
            walletAccountRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new ResourceNotFoundException("WalletAccount", id));
        }

        WalletAccount source = requireWallet(request.sourceWalletId());
        WalletAccount dest = requireWallet(request.destinationWalletId());
        familyAuthorizationClient.requirePermission(source.getFamilyId(), userProfileId, "wallet.transfer");
        if (!source.getFamilyId().equals(dest.getFamilyId())) {
            throw new BusinessException("Cross-family transfer forbidden", HttpStatus.FORBIDDEN);
        }
        if (!"ACTIVE".equals(source.getStatus()) || !"ACTIVE".equals(dest.getStatus())) {
            throw new BusinessException("Wallet not active", HttpStatus.CONFLICT);
        }

        LedgerAccount sourceLiability = requireLiability(source.getId());
        LedgerAccount destLiability = requireLiability(dest.getId());

        LedgerTransaction tx = ledgerPostingService.postBalancedTransaction(new LedgerPostingService.PostCommand(
                source.getFamilyId(),
                source.getId(),
                request.referenceId(),
                request.idempotencyKey(),
                "INTERNAL_TRANSFER",
                request.description() != null ? request.description() : "Internal transfer",
                List.of(
                        new LedgerPostingService.EntryLine(
                                sourceLiability.getId(), EntryType.DEBIT, request.amount(), "Transfer out"),
                        new LedgerPostingService.EntryLine(
                                destLiability.getId(), EntryType.CREDIT, request.amount(), "Transfer in")
                )
        ));
        return toTransactionResponse(tx);
    }

    /**
     * Saga step: FAMILY_POOL → MEMBER_WALLET for the requester member.
     * Idempotency key {@code allowance-disbursement:{requestId}} prevents double posting.
     * Completion outbox is written in the same DB transaction as the ledger post.
     * Uses REQUIRES_NEW so permanent failures do not poison the Kafka inbox transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LedgerTransactionResponse disburseAllowance(AllowanceDisbursementCommand command) {
        if (command.allowanceRequestId() == null || command.familyId() == null || command.requesterMemberId() == null) {
            throw new PermanentAllowanceDisbursementException("INVALID_PAYLOAD", "Missing required allowance identifiers");
        }
        if (command.amount() <= 0) {
            throw new PermanentAllowanceDisbursementException("INVALID_AMOUNT", "Amount must be positive");
        }
        String currency = command.currencyCode() == null || command.currencyCode().isBlank()
                ? "VND"
                : command.currencyCode();
        if (!"VND".equalsIgnoreCase(currency)) {
            throw new PermanentAllowanceDisbursementException("UNSUPPORTED_CURRENCY", "Only VND is supported");
        }

        WalletAccount source = resolveFamilyPool(command.familyId());
        WalletAccount dest = resolveMemberWallet(command.familyId(), command.requesterMemberId());

        if (!source.getFamilyId().equals(command.familyId()) || !dest.getFamilyId().equals(command.familyId())) {
            throw new PermanentAllowanceDisbursementException("FAMILY_MISMATCH", "Wallet family does not match allowance family");
        }
        if (!source.getFamilyId().equals(dest.getFamilyId())) {
            throw new PermanentAllowanceDisbursementException("FAMILY_MISMATCH", "Cross-family transfer forbidden");
        }
        if (!WalletType.FAMILY_POOL.name().equals(source.getWalletType())) {
            throw new PermanentAllowanceDisbursementException("INVALID_SOURCE_WALLET", "Source must be FAMILY_POOL");
        }
        if (!WalletType.MEMBER_WALLET.name().equals(dest.getWalletType())) {
            throw new PermanentAllowanceDisbursementException("INVALID_DESTINATION_WALLET", "Destination must be MEMBER_WALLET");
        }
        if (dest.getOwnerMemberId() == null || !dest.getOwnerMemberId().equals(command.requesterMemberId())) {
            throw new PermanentAllowanceDisbursementException(
                    "OWNERSHIP_MISMATCH", "Destination wallet is not owned by requester member");
        }
        if (!"ACTIVE".equals(source.getStatus()) || !"ACTIVE".equals(dest.getStatus())) {
            throw new PermanentAllowanceDisbursementException("WALLET_INACTIVE", "Wallet not active");
        }
        if (!"VND".equalsIgnoreCase(source.getCurrencyCode()) || !"VND".equalsIgnoreCase(dest.getCurrencyCode())) {
            throw new PermanentAllowanceDisbursementException("UNSUPPORTED_CURRENCY", "Wallet currency must be VND");
        }

        List<UUID> ordered = List.of(source.getId(), dest.getId()).stream()
                .sorted(Comparator.naturalOrder())
                .toList();
        for (UUID id : ordered) {
            walletAccountRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new PermanentAllowanceDisbursementException(
                            "WALLET_NOT_FOUND", "Wallet not found during lock: " + id));
        }

        LedgerAccount sourceLiability = requireLiability(source.getId());
        LedgerAccount destLiability = requireLiability(dest.getId());
        String referenceId = "allowance:" + command.allowanceRequestId();
        String idempotencyKey = IDEMPOTENCY_ALLOWANCE_PREFIX + command.allowanceRequestId();

        LedgerTransaction tx;
        try {
            tx = ledgerPostingService.postBalancedTransaction(new LedgerPostingService.PostCommand(
                    source.getFamilyId(),
                    source.getId(),
                    referenceId,
                    idempotencyKey,
                    "INTERNAL_TRANSFER",
                    "Allowance disbursement " + command.allowanceRequestId(),
                    List.of(
                            new LedgerPostingService.EntryLine(
                                    sourceLiability.getId(), EntryType.DEBIT, command.amount(), "Allowance out"),
                            new LedgerPostingService.EntryLine(
                                    destLiability.getId(), EntryType.CREDIT, command.amount(), "Allowance in")
                    )
            ));
        } catch (InsufficientBalanceException ex) {
            throw new PermanentAllowanceDisbursementException("INSUFFICIENT_FUNDS", ex.getMessage());
        }

        ensureAllowanceCompletionOutbox(command, source, dest, tx);
        return toTransactionResponse(tx);
    }

    @Transactional
    public void recordAllowanceDisbursementFailure(AllowanceDisbursementCommand command, String errorCode, String message) {
        if (command.allowanceRequestId() == null) {
            return;
        }
        if (outboxEventRepository.existsByEventTypeAndAggregateId(EVENT_ALLOWANCE_COMPLETED, command.allowanceRequestId())) {
            return;
        }
        if (outboxEventRepository.existsByEventTypeAndAggregateId(EVENT_ALLOWANCE_FAILED, command.allowanceRequestId())) {
            return;
        }
        Instant now = Instant.now();
        OutboxEvent outbox = new OutboxEvent();
        outbox.setEventType(EVENT_ALLOWANCE_FAILED);
        outbox.setAggregateType("allowance_request");
        outbox.setAggregateId(command.allowanceRequestId());
        Map<String, Object> payload = new HashMap<>();
        payload.put("requestId", command.allowanceRequestId().toString());
        payload.put("familyId", command.familyId() != null ? command.familyId().toString() : null);
        payload.put("errorCode", errorCode);
        payload.put("message", message != null ? message : errorCode);
        outbox.setPayload(payload);
        outbox.setCreatedAt(now);
        outbox.setStatus("PENDING");
        outboxEventRepository.save(outbox);
    }

    private void ensureAllowanceCompletionOutbox(
            AllowanceDisbursementCommand command,
            WalletAccount source,
            WalletAccount dest,
            LedgerTransaction tx
    ) {
        if (outboxEventRepository.existsByEventTypeAndAggregateId(EVENT_ALLOWANCE_COMPLETED, command.allowanceRequestId())) {
            return;
        }
        Instant now = Instant.now();
        OutboxEvent outbox = new OutboxEvent();
        outbox.setEventType(EVENT_ALLOWANCE_COMPLETED);
        outbox.setAggregateType("allowance_request");
        outbox.setAggregateId(command.allowanceRequestId());
        Map<String, Object> payload = new HashMap<>();
        payload.put("requestId", command.allowanceRequestId().toString());
        payload.put("familyId", command.familyId().toString());
        payload.put("requesterMemberId", command.requesterMemberId().toString());
        payload.put("sourceWalletId", source.getId().toString());
        payload.put("destinationWalletId", dest.getId().toString());
        payload.put("transactionId", tx.getId().toString());
        payload.put("amount", command.amount());
        payload.put("currencyCode", "VND");
        payload.put("referenceId", tx.getReferenceId());
        outbox.setPayload(payload);
        outbox.setCreatedAt(now);
        outbox.setStatus("PENDING");
        outboxEventRepository.save(outbox);
    }

    private WalletAccount resolveFamilyPool(UUID familyId) {
        List<WalletAccount> pools = walletAccountRepository
                .findByFamilyIdAndWalletTypeAndStatus(familyId, WalletType.FAMILY_POOL.name(), "ACTIVE");
        if (pools.isEmpty()) {
            throw new PermanentAllowanceDisbursementException(
                    "SOURCE_WALLET_NOT_FOUND", "No ACTIVE FAMILY_POOL for family " + familyId);
        }
        if (pools.size() > 1) {
            throw new PermanentAllowanceDisbursementException(
                    "AMBIGUOUS_SOURCE_WALLET", "Multiple ACTIVE FAMILY_POOL wallets for family");
        }
        return pools.getFirst();
    }

    private WalletAccount resolveMemberWallet(UUID familyId, UUID requesterMemberId) {
        List<WalletAccount> wallets = walletAccountRepository.findByFamilyIdAndOwnerMemberIdAndWalletTypeAndStatus(
                familyId, requesterMemberId, WalletType.MEMBER_WALLET.name(), "ACTIVE");
        if (wallets.isEmpty()) {
            throw new PermanentAllowanceDisbursementException(
                    "DESTINATION_WALLET_NOT_FOUND",
                    "No ACTIVE MEMBER_WALLET for member " + requesterMemberId);
        }
        if (wallets.size() > 1) {
            throw new PermanentAllowanceDisbursementException(
                    "AMBIGUOUS_DESTINATION_WALLET",
                    "Multiple ACTIVE MEMBER_WALLET rows for member");
        }
        return wallets.getFirst();
    }

    @Transactional
    public LedgerTransactionResponse creditFromPayment(TopUpCreditRequest request) {
        WalletAccount wallet = requireWallet(request.walletId());
        if (!"ACTIVE".equals(wallet.getStatus())) {
            throw new BusinessException("Wallet not active", HttpStatus.CONFLICT);
        }
        LedgerAccount liability = requireLiability(wallet.getId());
        LedgerAccount clearing = ensurePspClearing(wallet.getFamilyId(), Instant.now());

        LedgerTransaction tx = ledgerPostingService.postBalancedTransaction(new LedgerPostingService.PostCommand(
                wallet.getFamilyId(),
                wallet.getId(),
                request.referenceId(),
                request.idempotencyKey(),
                "PAYMENT_TOPUP",
                request.description() != null ? request.description() : "Sandbox top-up credit",
                List.of(
                        new LedgerPostingService.EntryLine(
                                clearing.getId(), EntryType.DEBIT, request.amount(), "PSP clearing"),
                        new LedgerPostingService.EntryLine(
                                liability.getId(), EntryType.CREDIT, request.amount(), "Wallet credit")
                )
        ));
        return toTransactionResponse(tx);
    }

    @Transactional(readOnly = true)
    public LedgerTransactionResponse getTransaction(UUID transactionId) {
        LedgerTransaction tx = ledgerTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("LedgerTransaction", transactionId));
        return toTransactionResponse(tx);
    }

    @Transactional
    public LedgerTransactionResponse reverse(UUID transactionId, String referenceId, String idempotencyKey) {
        LedgerTransaction tx = ledgerPostingService.reverseTransaction(transactionId, referenceId, idempotencyKey);
        return toTransactionResponse(tx);
    }

    private WalletAccount requireWallet(UUID walletId) {
        return walletAccountRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("WalletAccount", walletId));
    }

    private LedgerAccount requireLiability(UUID walletId) {
        return ledgerAccountRepository.findByWalletAccountIdAndAccountCode(walletId, LIABILITY_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("WALLET_LIABILITY for wallet " + walletId));
    }

    private long availableBalance(UUID walletId) {
        LedgerAccount liability = requireLiability(walletId);
        return walletBalanceRepository.findByLedgerAccountId(liability.getId())
                .map(WalletBalance::getBalanceAmount)
                .orElse(0L);
    }

    private LedgerAccount ensurePspClearing(UUID familyId, Instant now) {
        return ledgerAccountRepository.findByFamilyIdAndAccountCodeAndWalletAccountIsNull(familyId, PSP_CLEARING_CODE)
                .orElseGet(() -> {
                    LedgerAccount clearing = new LedgerAccount();
                    clearing.setFamilyId(familyId);
                    clearing.setAccountCode(PSP_CLEARING_CODE);
                    clearing.setName("PSP clearing / receivable");
                    clearing.setAccountKind("ASSET");
                    clearing.setNormalBalance("DEBIT");
                    clearing.setCreatedAt(now);
                    clearing.setUpdatedAt(now);
                    return ledgerAccountRepository.save(clearing);
                });
    }

    private WalletResponse toWalletResponse(WalletAccount wallet, long balance) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getFamilyId(),
                wallet.getOwnerMemberId(),
                wallet.getName(),
                wallet.getCurrencyCode(),
                wallet.getWalletType(),
                wallet.getStatus(),
                balance,
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }

    private LedgerTransactionResponse toTransactionResponse(LedgerTransaction tx) {
        List<LedgerEntry> entries = tx.getEntries() != null && !tx.getEntries().isEmpty()
                ? tx.getEntries()
                : ledgerEntryRepository.findByTransactionId(tx.getId());
        return new LedgerTransactionResponse(
                tx.getId(),
                tx.getFamilyId(),
                tx.getReferenceId(),
                tx.getIdempotencyKey(),
                tx.getDescription(),
                tx.getTransactionType(),
                tx.getPostedAt(),
                entries.stream()
                        .map(e -> new LedgerEntryResponse(
                                e.getId(),
                                e.getLedgerAccount().getId(),
                                e.getEntryType(),
                                e.getAmount(),
                                e.getMemo()
                        ))
                        .toList()
        );
    }
}
