package com.familyfinance.payment.controller;

import com.familyfinance.payment.dto.InitiatePaymentRequest;
import com.familyfinance.payment.dto.InitiatePaymentResponse;
import com.familyfinance.payment.dto.PaymentOrderResponse;
import com.familyfinance.payment.service.PaymentOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment-orders")
public class PaymentOrderController {

    private final PaymentOrderService service;

    public PaymentOrderController(PaymentOrderService service) {
        this.service = service;
    }

    @GetMapping
    public List<PaymentOrderResponse> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public PaymentOrderResponse get(@PathVariable UUID id) {
        return service.findById(id);
    }

    /**
     * TODO: Create payment order + dispatch to PaymentProvider — reserved learning topic.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public PaymentOrderResponse create(@RequestBody InitiatePaymentRequest request) {
        throw new UnsupportedOperationException("Payment order creation not implemented in scaffold");
    }

    /**
     * TODO: Provider callback/webhook ingestion via webhook_inbox — reserved learning topic.
     */
    @PostMapping("/{id}/initiate")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public InitiatePaymentResponse initiate(@PathVariable UUID id, @RequestBody InitiatePaymentRequest request) {
        throw new UnsupportedOperationException("Payment initiation not implemented in scaffold");
    }
}
