package com.familyfinance.payment.exception;

import org.springframework.http.HttpStatus;

public class PaymentVerificationException extends BusinessException {

    public PaymentVerificationException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}