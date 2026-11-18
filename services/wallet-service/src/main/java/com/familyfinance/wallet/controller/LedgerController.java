package com.familyfinance.wallet.controller;

import com.familyfinance.wallet.dto.LedgerTransactionResponse;
import com.familyfinance.wallet.service.WalletApplicationService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ledger")
@Validated
public class LedgerController {

    private final WalletApplicationService walletApplicationService;

    public LedgerController(WalletApplicationService walletApplicationService) {
        this.walletApplicationService = walletApplicationService;
    }

    @GetMapping("/transactions/{transactionId}")
    public LedgerTransactionResponse get(@PathVariable UUID transactionId) {
        return walletApplicationService.getTransaction(transactionId);
    }

    @PostMapping("/transactions/{transactionId}/reversal")
    @ResponseStatus(HttpStatus.CREATED)
    public LedgerTransactionResponse reverse(
            @PathVariable UUID transactionId,
            @RequestParam @NotBlank String referenceId,
            @RequestParam @NotBlank String idempotencyKey
    ) {
        return walletApplicationService.reverse(transactionId, referenceId, idempotencyKey);
    }
}
