package com.familyfinance.payment.repository;

import com.familyfinance.payment.entity.ProviderTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProviderTransactionRepository extends JpaRepository<ProviderTransaction, UUID> {
}