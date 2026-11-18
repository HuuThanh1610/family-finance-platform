package com.familyfinance.wallet.repository;

import com.familyfinance.wallet.entity.InboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InboxEventRepository extends JpaRepository<InboxEvent, UUID> {

    Optional<InboxEvent> findByEventId(UUID eventId);

    boolean existsByEventId(UUID eventId);
}
