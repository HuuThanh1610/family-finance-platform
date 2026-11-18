package com.familyfinance.payment.dto;

public record FakePaymentIpnResponse(int resultCode, String message) {
  public static FakePaymentIpnResponse success() {
    return new FakePaymentIpnResponse(0, "Success");
  }
}