package com.familyfinance.payment.exception;

import org.springframework.http.HttpStatus;

public class PaymentConfigurationException extends BusinessException {

    public PaymentConfigurationException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}