package com.familyfinance.payment.controller;

import com.familyfinance.payment.dto.PaymentOrderResponse;
import com.familyfinance.payment.entity.PaymentOrder;
import com.familyfinance.payment.service.PaymentOrderService;
import com.familyfinance.payment.service.PaymentRecoveryService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/payments")
public class InternalPaymentController {

    private final PaymentOrderService service;
    private final PaymentRecoveryService recoveryService;

    public InternalPaymentController(PaymentOrderService service, PaymentRecoveryService recoveryService) {
        this.service = service;
        this.recoveryService = recoveryService;
    }

    @PostMapping("/{id}/mark-completed")
    public PaymentOrderResponse markCompleted(@PathVariable UUID id) {
        return service.markCompleted(id);
    }

    @PostMapping("/{id}/replay-outbox")
    public void replayPaymentProviderPaidOutbox(@PathVariable UUID id) {
        service.ensurePaymentProviderPaidOutbox(id);
    }

    @PostMapping("/{id}/reconcile-provider")
    public PaymentOrderResponse reconcileProvider(@PathVariable UUID id) {
        PaymentOrder order = recoveryService.reconcileWithProvider(id);
        return service.findById(order.getId());
    }

    @PostMapping("/webhooks/{inboxId}/reprocess")
    public void reprocessWebhook(@PathVariable UUID inboxId) {
        recoveryService.reprocessWebhook(inboxId);
    }
}