package com.familyfinance.payment.service;

import com.familyfinance.payment.dto.PaymentOrderResponse;
import com.familyfinance.payment.entity.PaymentOrder;
import com.familyfinance.payment.exception.ResourceNotFoundException;
import com.familyfinance.payment.repository.PaymentOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PaymentOrderService {

    private final PaymentOrderRepository repository;

    public PaymentOrderService(PaymentOrderRepository repository) {
        this.repository = repository;
    }

    public List<PaymentOrderResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public PaymentOrderResponse findById(UUID id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentOrder", id));
    }

        private PaymentOrderResponse toResponse(PaymentOrder entity) {
        return new PaymentOrderResponse(
                entity.getId(),
                entity.getFamilyId(),
                entity.getPayerMemberId(),
                entity.getAmount(),
                entity.getCurrencyCode(),
                entity.getPurpose(),
                entity.getExternalReference(),
                entity.getStatus(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
