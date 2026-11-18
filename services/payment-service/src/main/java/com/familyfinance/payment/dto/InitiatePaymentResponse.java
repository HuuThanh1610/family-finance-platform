package com.familyfinance.payment.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.UUID;

public record InitiatePaymentResponse(
        UUID paymentOrderId,
        String status,
        String redirectUrl
) {
}
