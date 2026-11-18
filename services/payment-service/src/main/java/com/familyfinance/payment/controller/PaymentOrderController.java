package com.familyfinance.payment.controller;

import com.familyfinance.payment.dto.PaymentOrderResponse;
import com.familyfinance.payment.service.PaymentOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * @deprecated Use {@link PaymentController} at /api/v1/payments.
 */
@Deprecated
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
}