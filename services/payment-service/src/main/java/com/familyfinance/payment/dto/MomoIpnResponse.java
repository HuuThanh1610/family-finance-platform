package com.familyfinance.payment.dto;

public record MomoIpnResponse(int resultCode, String message) {
    public static MomoIpnResponse success() {
        return new MomoIpnResponse(0, "Success");
    }
}