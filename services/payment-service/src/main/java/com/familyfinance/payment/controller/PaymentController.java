package com.familyfinance.payment.controller;

import com.familyfinance.payment.dto.CreatePaymentRequest;
import com.familyfinance.payment.dto.CreatePaymentResponse;
import com.familyfinance.payment.dto.PaymentOrderResponse;
import com.familyfinance.payment.service.PaymentOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentOrderService service;

    public PaymentController(PaymentOrderService service) {
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatePaymentResponse create(@Valid @RequestBody CreatePaymentRequest request) {
        return service.create(request);
    }
}