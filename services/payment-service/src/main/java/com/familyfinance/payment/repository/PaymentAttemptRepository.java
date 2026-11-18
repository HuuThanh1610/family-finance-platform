package com.familyfinance.payment.repository;

import com.familyfinance.payment.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {

    Optional<PaymentAttempt> findTopByPaymentOrder_IdOrderByAttemptNoDesc(UUID paymentOrderId);
}