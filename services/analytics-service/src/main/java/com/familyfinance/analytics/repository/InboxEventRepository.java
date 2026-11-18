package com.familyfinance.analytics.repository;

import com.familyfinance.analytics.entity.InboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InboxEventRepository extends JpaRepository<InboxEvent, UUID> {

    boolean existsByEventId(UUID eventId);
}