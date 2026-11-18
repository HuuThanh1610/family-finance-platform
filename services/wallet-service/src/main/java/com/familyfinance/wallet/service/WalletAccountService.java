package com.familyfinance.wallet.service;

import com.familyfinance.wallet.dto.WalletAccountResponse;
import com.familyfinance.wallet.entity.WalletAccount;
import com.familyfinance.wallet.exception.ResourceNotFoundException;
import com.familyfinance.wallet.repository.WalletAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class WalletAccountService {

    private final WalletAccountRepository repository;

    public WalletAccountService(WalletAccountRepository repository) {
        this.repository = repository;
    }

    public List<WalletAccountResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public WalletAccountResponse findById(UUID id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("WalletAccount", id));
    }

        private WalletAccountResponse toResponse(WalletAccount entity) {
        return new WalletAccountResponse(
                entity.getId(),
                entity.getFamilyId(),
                entity.getOwnerMemberId(),
                entity.getName(),
                entity.getCurrencyCode(),
                entity.getWalletType(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
