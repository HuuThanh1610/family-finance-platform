package com.familyfinance.payment.exception;

import org.springframework.http.HttpStatus;

public class InvalidPaymentStateException extends BusinessException {

    public InvalidPaymentStateException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}