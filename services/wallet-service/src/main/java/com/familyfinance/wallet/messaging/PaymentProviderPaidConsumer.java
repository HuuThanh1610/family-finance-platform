package com.familyfinance.wallet.messaging;

import com.familyfinance.wallet.dto.TopUpCreditRequest;
import com.familyfinance.wallet.entity.InboxEvent;
import com.familyfinance.wallet.repository.InboxEventRepository;
import com.familyfinance.wallet.service.WalletApplicationService;
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
 * Idempotent consumer: PaymentProviderPaid → ledger top-up credit.
 * Publishes WalletCreditPosted via ledger outbox in the same DB transaction path.
 */
@Component
@ConditionalOnProperty(name = "familyfinance.kafka.enabled", havingValue = "true")
public class PaymentProviderPaidConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentProviderPaidConsumer.class);
    public static final String EVENT_TYPE = "PaymentProviderPaid";

    private final InboxEventRepository inboxEventRepository;
    private final WalletApplicationService walletApplicationService;
    private final ObjectMapper objectMapper;

    public PaymentProviderPaidConsumer(
            InboxEventRepository inboxEventRepository,
            WalletApplicationService walletApplicationService,
            ObjectMapper objectMapper
    ) {
        this.inboxEventRepository = inboxEventRepository;
        this.walletApplicationService = walletApplicationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${familyfinance.kafka.topics.payment-events:payment.provider.events}",
            groupId = "${spring.kafka.consumer.group-id:wallet-service}"
    )
    @Transactional
    public void onMessage(@Payload String raw) {
        DomainEventMessage event;
        try {
            event = objectMapper.readValue(raw, DomainEventMessage.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid PaymentProviderPaid payload", ex);
        }
        if (!EVENT_TYPE.equals(event.eventType())) {
            return;
        }
        if (inboxEventRepository.existsByEventId(event.eventId())) {
            log.info("Duplicate PaymentProviderPaid skipped eventId={}", event.eventId());
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

        try {
            Map<String, Object> p = event.payload();
            UUID walletId = UUID.fromString(String.valueOf(p.get("walletId")));
            long amount = Long.parseLong(String.valueOf(p.get("amount")));
            String paymentOrderId = String.valueOf(p.get("paymentOrderId"));
            String idempotencyKey = "pay-credit:" + paymentOrderId;

            walletApplicationService.creditFromPayment(new TopUpCreditRequest(
                    walletId,
                    amount,
                    "payment:" + paymentOrderId,
                    idempotencyKey,
                    "MoMo sandbox top-up"
            ));

            inbox.setStatus("PROCESSED");
            inbox.setProcessedAt(Instant.now());
            inboxEventRepository.save(inbox);
        } catch (RuntimeException ex) {
            inbox.setStatus("FAILED");
            inboxEventRepository.save(inbox);
            throw ex;
        }
    }
}
