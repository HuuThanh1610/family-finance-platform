package com.familyfinance.payment.messaging;

import com.familyfinance.payment.entity.InboxEvent;
import com.familyfinance.payment.repository.InboxEventRepository;
import com.familyfinance.payment.service.PaymentOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Completes top-up saga after wallet ledger confirms credit.
 */
@Component
@ConditionalOnProperty(name = "familyfinance.kafka.enabled", havingValue = "true")
public class WalletCreditPostedConsumer {

    private static final Logger log = LoggerFactory.getLogger(WalletCreditPostedConsumer.class);

    private final InboxEventRepository inboxEventRepository;
    private final PaymentOrderService paymentOrderService;
    private final ObjectMapper objectMapper;

    public WalletCreditPostedConsumer(
            InboxEventRepository inboxEventRepository,
            PaymentOrderService paymentOrderService,
            ObjectMapper objectMapper
    ) {
        this.inboxEventRepository = inboxEventRepository;
        this.paymentOrderService = paymentOrderService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${familyfinance.kafka.topics.wallet-ledger:wallet.ledger.events}",
            groupId = "${spring.kafka.consumer.group-id:payment-service}"
    )
    @Transactional
    public void onMessage(@Payload String raw) throws Exception {
        DomainEventMessage event = objectMapper.readValue(raw, DomainEventMessage.class);
        if (!"WalletCreditPosted".equals(event.eventType())) {
            return;
        }
        if (inboxEventRepository.existsByEventId(event.eventId())) {
            log.info("Duplicate WalletCreditPosted skipped eventId={}", event.eventId());
            return;
        }

        InboxEvent inbox = new InboxEvent();
        inbox.setEventId(event.eventId());
        inbox.setEventType(event.eventType());
        inbox.setAggregateType(event.aggregateType());
        inbox.setAggregateId(event.aggregateId());
        inbox.setPayload(event.payload());
        inbox.setReceivedAt(Instant.now());
        inbox.setStatus("PENDING");
        inboxEventRepository.save(inbox);

        Map<String, Object> payload = event.payload();
        Object ref = payload != null ? payload.get("referenceId") : null;
        if (ref == null || !ref.toString().startsWith("payment:")) {
            inbox.setStatus("SKIPPED");
            inbox.setProcessedAt(Instant.now());
            inboxEventRepository.save(inbox);
            return;
        }
        UUID paymentOrderId = UUID.fromString(ref.toString().substring("payment:".length()));
        paymentOrderService.markCompleted(paymentOrderId);
        inbox.setStatus("PROCESSED");
        inbox.setProcessedAt(Instant.now());
        inboxEventRepository.save(inbox);
    }
}
