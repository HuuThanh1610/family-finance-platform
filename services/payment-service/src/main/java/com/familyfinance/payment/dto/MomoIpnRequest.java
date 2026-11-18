package com.familyfinance.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MomoIpnRequest(
        String partnerCode,
        String accessKey,
        Long amount,
        String orderId,
        String orderInfo,
        String orderType,
        String transId,
        Long resultCode,
        String message,
        String payType,
        String responseTime,
        String requestId,
        String extraData,
        String signature
) {
}