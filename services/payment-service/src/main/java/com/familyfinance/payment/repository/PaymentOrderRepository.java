package com.familyfinance.payment.repository;

import com.familyfinance.payment.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, UUID> {
}
