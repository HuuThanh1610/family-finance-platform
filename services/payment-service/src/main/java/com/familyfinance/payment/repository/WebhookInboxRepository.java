package com.familyfinance.payment.repository;

import com.familyfinance.payment.entity.WebhookInbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WebhookInboxRepository extends JpaRepository<WebhookInbox, UUID> {

    Optional<WebhookInbox> findByProviderCodeAndIdempotencyKey(String providerCode, String idempotencyKey);
}