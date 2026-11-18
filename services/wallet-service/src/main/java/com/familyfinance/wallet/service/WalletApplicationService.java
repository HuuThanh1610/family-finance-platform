package com.familyfinance.wallet.service;

import com.familyfinance.wallet.domain.EntryType;
import com.familyfinance.wallet.domain.WalletType;
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
import com.familyfinance.wallet.entity.WalletAccount;
import com.familyfinance.wallet.entity.WalletBalance;
import com.familyfinance.wallet.exception.BusinessException;
import com.familyfinance.wallet.exception.ResourceNotFoundException;
import com.familyfinance.wallet.repository.LedgerAccountRepository;
import com.familyfinance.wallet.repository.LedgerEntryRepository;
import com.familyfinance.wallet.repository.LedgerTransactionRepository;
import com.familyfinance.wallet.repository.WalletAccountRepository;
import com.familyfinance.wallet.repository.WalletBalanceRepository;
import com.familyfinance.wallet.security.FamilyAuthorizationClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class WalletApplicationService {

    public static final String LIABILITY_CODE = "WALLET_LIABILITY";
    public static final String PSP_CLEARING_CODE = "PSP_CLEARING";

    private final WalletAccountRepository walletAccountRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final WalletBalanceRepository walletBalanceRepository;
    private final LedgerTransactionRepository ledgerTransactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerPostingService ledgerPostingService;
    private final FamilyAuthorizationClient familyAuthorizationClient;

    public WalletApplicationService(
            WalletAccountRepository walletAccountRepository,
            LedgerAccountRepository ledgerAccountRepository,
            WalletBalanceRepository walletBalanceRepository,
            LedgerTransactionRepository ledgerTransactionRepository,
            LedgerEntryRepository ledgerEntryRepository,
            LedgerPostingService ledgerPostingService,
            FamilyAuthorizationClient familyAuthorizationClient
    ) {
        this.walletAccountRepository = walletAccountRepository;
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.walletBalanceRepository = walletBalanceRepository;
        this.ledgerTransactionRepository = ledgerTransactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.ledgerPostingService = ledgerPostingService;
        this.familyAuthorizationClient = familyAuthorizationClient;
    }

    @Transactional
    public WalletResponse createWallet(CreateWalletRequest request, UUID userProfileId) {
        familyAuthorizationClient.requireMember(request.familyId(), userProfileId);
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
        familyAuthorizationClient.requireMember(source.getFamilyId(), userProfileId);
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
