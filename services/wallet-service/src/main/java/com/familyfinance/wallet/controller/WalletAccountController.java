package com.familyfinance.wallet.controller;

import com.familyfinance.wallet.dto.WalletAccountResponse;
import com.familyfinance.wallet.service.WalletAccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/wallet-accounts")
public class WalletAccountController {

    private final WalletAccountService service;

    public WalletAccountController(WalletAccountService service) {
        this.service = service;
    }

    @GetMapping
    public List<WalletAccountResponse> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public WalletAccountResponse get(@PathVariable UUID id) {
        return service.findById(id);
    }
}
