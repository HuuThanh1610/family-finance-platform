package com.familyfinance.payment.service;

import com.familyfinance.payment.domain.PaymentStatus;
import com.familyfinance.payment.entity.PaymentOrder;
import com.familyfinance.payment.entity.ProviderTransaction;
import com.familyfinance.payment.entity.WebhookInbox;
import com.familyfinance.payment.exception.BusinessException;
import com.familyfinance.payment.exception.ResourceNotFoundException;
import com.familyfinance.payment.provider.MoMoSandboxAdapter;
import com.familyfinance.payment.provider.ProviderQueryResult;
import com.familyfinance.payment.repository.PaymentOrderRepository;
import com.familyfinance.payment.repository.ProviderTransactionRepository;
import com.familyfinance.payment.repository.WebhookInboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(PaymentRecoveryService.class);

    private final WebhookInboxRepository webhookInboxRepository;
    private final PaymentOrderService paymentOrderService;
    private final PaymentOrderRepository paymentOrderRepository;
    private final ProviderTransactionRepository providerTransactionRepository;
    private final MoMoSandboxAdapter momoSandboxAdapter;

    public PaymentRecoveryService(
            WebhookInboxRepository webhookInboxRepository,
            PaymentOrderService paymentOrderService,
            PaymentOrderRepository paymentOrderRepository,
            ProviderTransactionRepository providerTransactionRepository,
            MoMoSandboxAdapter momoSandboxAdapter
    ) {
        this.webhookInboxRepository = webhookInboxRepository;
        this.paymentOrderService = paymentOrderService;
        this.paymentOrderRepository = paymentOrderRepository;
        this.providerTransactionRepository = providerTransactionRepository;
        this.momoSandboxAdapter = momoSandboxAdapter;
    }

    @Scheduled(fixedDelayString = "${familyfinance.payment.recovery.webhook-poll-ms:5000}")
    @Transactional
    public void reprocessPendingWebhooks() {
        List<WebhookInbox> batch = webhookInboxRepository.findTop50ByStatusOrderByReceivedAtAsc("PENDING");
        for (WebhookInbox inbox : batch) {
            if (!"MOMO_IPN".equals(inbox.getEventType())) {
                continue;
            }
            try {
                reprocessWebhook(inbox);
            } catch (Exception ex) {
                log.warn("Webhook recovery failed inboxId={}: {}", inbox.getId(), ex.toString());
                inbox.setStatus("FAILED");
                webhookInboxRepository.save(inbox);
            }
        }
    }

    @Transactional
    public void reprocessWebhook(UUID inboxId) {
        WebhookInbox inbox = webhookInboxRepository.findById(inboxId)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookInbox", inboxId));
        if (!"PENDING".equals(inbox.getStatus()) && !"FAILED".equals(inbox.getStatus())) {
            throw new BusinessException("Webhook inbox is not reprocessable: " + inbox.getStatus(),
                    HttpStatus.CONFLICT);
        }
        reprocessWebhook(inbox);
    }

    @Transactional
    public PaymentOrder reconcileWithProvider(UUID orderId) {
        PaymentOrder order = paymentOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentOrder", orderId));
        PaymentStatus current = PaymentStatus.fromDbValue(order.getStatus());
        if (current != PaymentStatus.PENDING_PROVIDER) {
            throw new BusinessException(
                    "Provider reconciliation only for PENDING_PROVIDER, was " + current,
                    HttpStatus.CONFLICT);
        }

        ProviderTransaction initiated = providerTransactionRepository
                .findTopByPaymentAttempt_PaymentOrder_IdAndStatusOrderByRecordedAtDesc(orderId, "INITIATED")
                .orElseThrow(() -> new BusinessException(
                        "No INITIATED provider transaction for order " + orderId, HttpStatus.CONFLICT));

        ProviderQueryResult query = momoSandboxAdapter.queryPaymentStatus(
                orderId.toString(),
                initiated.getProviderTxnId());

        if (!query.paid()) {
            throw new BusinessException(
                    "MoMo query not paid for order " + orderId + " resultCode=" + query.resultCode(),
                    HttpStatus.CONFLICT);
        }
        if (query.amount() > 0 && query.amount() != order.getAmount()) {
            throw new BusinessException(
                    "MoMo query amount mismatch for order " + orderId, HttpStatus.BAD_REQUEST);
        }

        String providerTxnId = query.providerTxnId() != null && !query.providerTxnId().isBlank()
                ? query.providerTxnId()
                : initiated.getProviderTxnId();

        paymentOrderService.applyProviderPaid(
                orderId,
                order.getAmount(),
                MoMoSandboxAdapter.PROVIDER_CODE,
                providerTxnId,
                query.rawResponse()
        );
        return paymentOrderRepository.findById(orderId).orElseThrow();
    }

    private void reprocessWebhook(WebhookInbox inbox) {
        Map<String, Object> payload = inbox.getPayload();
        Object orderIdRaw = payload.get("orderId");
        Object amountRaw = payload.get("amount");
        if (orderIdRaw == null || amountRaw == null) {
            throw new BusinessException("Webhook payload missing orderId/amount", HttpStatus.BAD_REQUEST);
        }

        UUID orderId = UUID.fromString(orderIdRaw.toString());
        long amount = Long.parseLong(amountRaw.toString());
        paymentOrderService.applyProviderPaid(
                orderId,
                amount,
                inbox.getProviderCode(),
                inbox.getIdempotencyKey(),
                payload
        );
        inbox.setStatus("PROCESSED");
        inbox.setProcessedAt(Instant.now());
        webhookInboxRepository.save(inbox);
    }
}