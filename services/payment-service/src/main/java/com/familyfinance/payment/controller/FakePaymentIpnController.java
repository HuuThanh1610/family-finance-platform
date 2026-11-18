package com.familyfinance.payment.controller;

import com.familyfinance.payment.dto.FakePaymentIpnRequest;
import com.familyfinance.payment.dto.FakePaymentIpnResponse;
import com.familyfinance.payment.service.FakePaymentIpnService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/fake")
public class FakePaymentIpnController {

  private final FakePaymentIpnService fakePaymentIpnService;

  public FakePaymentIpnController(FakePaymentIpnService fakePaymentIpnService) {
    this.fakePaymentIpnService = fakePaymentIpnService;
  }

  /** Simulates provider success without live MoMo (runtime E2E / CI). */
  @PostMapping("/ipn")
  @ResponseStatus(HttpStatus.OK)
  public FakePaymentIpnResponse ipn(@Valid @RequestBody FakePaymentIpnRequest request) {
    return fakePaymentIpnService.simulatePaid(request);
  }
}