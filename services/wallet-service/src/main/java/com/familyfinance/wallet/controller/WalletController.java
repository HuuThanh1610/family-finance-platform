package com.familyfinance.wallet.controller;

import com.familyfinance.wallet.dto.CreateWalletRequest;
import com.familyfinance.wallet.dto.LedgerTransactionResponse;
import com.familyfinance.wallet.dto.TopUpCreditRequest;
import com.familyfinance.wallet.dto.WalletBalanceResponse;
import com.familyfinance.wallet.dto.WalletResponse;
import com.familyfinance.wallet.dto.WalletTransferRequest;
import com.familyfinance.wallet.service.WalletApplicationService;
import com.familyfinance.wallet.support.CurrentUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class WalletController {

    private final WalletApplicationService walletApplicationService;
    private final CurrentUserResolver currentUserResolver;

    public WalletController(
            WalletApplicationService walletApplicationService,
            CurrentUserResolver currentUserResolver
    ) {
        this.walletApplicationService = walletApplicationService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/wallets")
    @ResponseStatus(HttpStatus.CREATED)
    public WalletResponse create(@Valid @RequestBody CreateWalletRequest request, HttpServletRequest httpRequest) {
        UUID userId = currentUserResolver.requireUserProfileId(httpRequest);
        return walletApplicationService.createWallet(request, userId);
    }

    @GetMapping("/wallets")
    public List<WalletResponse> list(@RequestParam UUID familyId, HttpServletRequest httpRequest) {
        UUID userId = currentUserResolver.requireUserProfileId(httpRequest);
        return walletApplicationService.listWallets(familyId, userId);
    }

    @GetMapping("/wallets/{walletId}")
    public WalletResponse get(@PathVariable UUID walletId, HttpServletRequest httpRequest) {
        UUID userId = currentUserResolver.requireUserProfileId(httpRequest);
        return walletApplicationService.getWallet(walletId, userId);
    }

    @GetMapping("/wallets/{walletId}/balance")
    public WalletBalanceResponse balance(@PathVariable UUID walletId, HttpServletRequest httpRequest) {
        UUID userId = currentUserResolver.requireUserProfileId(httpRequest);
        return walletApplicationService.getBalance(walletId, userId);
    }

    @GetMapping("/wallets/{walletId}/transactions")
    public List<LedgerTransactionResponse> transactions(
            @PathVariable UUID walletId, HttpServletRequest httpRequest) {
        UUID userId = currentUserResolver.requireUserProfileId(httpRequest);
        return walletApplicationService.listTransactions(walletId, userId);
    }

    @PostMapping("/wallet-transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public LedgerTransactionResponse transfer(
            @Valid @RequestBody WalletTransferRequest request, HttpServletRequest httpRequest) {
        UUID userId = currentUserResolver.requireUserProfileId(httpRequest);
        return walletApplicationService.transfer(request, userId);
    }

    /** Payment-service callback — authenticated via X-Internal-Api-Key when security enabled. */
    @PostMapping("/internal/wallet-credits")
    @ResponseStatus(HttpStatus.CREATED)
    public LedgerTransactionResponse credit(@Valid @RequestBody TopUpCreditRequest request) {
        return walletApplicationService.creditFromPayment(request);
    }
}
