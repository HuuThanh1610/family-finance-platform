package com.familyfinance.wallet.exception;

import org.springframework.http.HttpStatus;

/**
 * Permanent business failure for allowance disbursement — do not Kafka-retry as transient.
 */
public class PermanentAllowanceDisbursementException extends BusinessException {

    private final String errorCode;

    public PermanentAllowanceDisbursementException(String errorCode, String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
