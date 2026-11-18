package com.familyfinance.wallet.controller;

import com.familyfinance.wallet.dto.CreateWalletRequest;
import com.familyfinance.wallet.dto.LedgerTransactionResponse;
import com.familyfinance.wallet.dto.TopUpCreditRequest;
import com.familyfinance.wallet.dto.WalletBalanceResponse;
import com.familyfinance.wallet.dto.WalletResponse;
import com.familyfinance.wallet.dto.WalletTransferRequest;
import com.familyfinance.wallet.service.WalletApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class WalletController {

    private final WalletApplicationService walletApplicationService;

    public WalletController(WalletApplicationService walletApplicationService) {
        this.walletApplicationService = walletApplicationService;
    }

    @PostMapping("/wallets")
    @ResponseStatus(HttpStatus.CREATED)
    public WalletResponse create(@Valid @RequestBody CreateWalletRequest request) {
        return walletApplicationService.createWallet(request);
    }

    @GetMapping("/wallets/{walletId}")
    public WalletResponse get(@PathVariable UUID walletId) {
        return walletApplicationService.getWallet(walletId);
    }

    @GetMapping("/wallets/{walletId}/balance")
    public WalletBalanceResponse balance(@PathVariable UUID walletId) {
        return walletApplicationService.getBalance(walletId);
    }

    @GetMapping("/wallets/{walletId}/transactions")
    public List<LedgerTransactionResponse> transactions(@PathVariable UUID walletId) {
        return walletApplicationService.listTransactions(walletId);
    }

    @PostMapping("/wallet-transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public LedgerTransactionResponse transfer(@Valid @RequestBody WalletTransferRequest request) {
        return walletApplicationService.transfer(request);
    }

    /** Internal/payment callback credit — protect with service auth in production. */
    @PostMapping("/internal/wallet-credits")
    @ResponseStatus(HttpStatus.CREATED)
    public LedgerTransactionResponse credit(@Valid @RequestBody TopUpCreditRequest request) {
        return walletApplicationService.creditFromPayment(request);
    }
}
