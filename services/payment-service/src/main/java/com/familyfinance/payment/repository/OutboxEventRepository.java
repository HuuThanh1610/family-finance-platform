package com.familyfinance.payment.repository;

import com.familyfinance.payment.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
}