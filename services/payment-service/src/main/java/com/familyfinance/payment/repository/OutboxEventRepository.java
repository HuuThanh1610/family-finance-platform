package com.familyfinance.payment.repository;

import com.familyfinance.payment.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(String status);

    List<OutboxEvent> findTop50ByStatusInOrderByCreatedAtAsc(Collection<String> statuses);

    boolean existsByAggregateIdAndEventType(UUID aggregateId, String eventType);
}