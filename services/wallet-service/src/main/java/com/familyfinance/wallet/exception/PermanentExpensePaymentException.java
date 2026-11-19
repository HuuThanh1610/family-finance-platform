package com.familyfinance.wallet.exception;

import org.springframework.http.HttpStatus;

/** Permanent expense payment failure — do not Kafka-retry as transient. */
public class PermanentExpensePaymentException extends BusinessException {

    private final String errorCode;

    public PermanentExpensePaymentException(String errorCode, String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
