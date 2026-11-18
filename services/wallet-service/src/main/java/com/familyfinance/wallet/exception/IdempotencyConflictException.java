package com.familyfinance.wallet.exception;

import org.springframework.http.HttpStatus;

public class IdempotencyConflictException extends BusinessException {

    public IdempotencyConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
