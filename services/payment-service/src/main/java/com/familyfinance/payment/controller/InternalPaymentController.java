package com.familyfinance.payment.controller;

import com.familyfinance.payment.dto.PaymentOrderResponse;
import com.familyfinance.payment.service.PaymentOrderService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/payments")
public class InternalPaymentController {

    private final PaymentOrderService service;

    public InternalPaymentController(PaymentOrderService service) {
        this.service = service;
    }

    @PostMapping("/{id}/mark-completed")
    public PaymentOrderResponse markCompleted(@PathVariable UUID id) {
        return service.markCompleted(id);
    }
}