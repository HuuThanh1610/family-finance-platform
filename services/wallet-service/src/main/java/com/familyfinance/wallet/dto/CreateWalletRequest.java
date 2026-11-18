package com.familyfinance.wallet.dto;

import com.familyfinance.wallet.domain.WalletType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateWalletRequest(
        @NotNull UUID familyId,
        UUID ownerMemberId,
        @NotBlank String name,
        @NotNull WalletType walletType
) {
}
