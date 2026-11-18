package com.familyfinance.payment.controller;

import com.familyfinance.payment.dto.MomoIpnRequest;
import com.familyfinance.payment.dto.MomoIpnResponse;
import com.familyfinance.payment.service.MomoIpnService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/momo")
public class MomoIpnController {

    private final MomoIpnService momoIpnService;

    public MomoIpnController(MomoIpnService momoIpnService) {
        this.momoIpnService = momoIpnService;
    }

    @PostMapping("/ipn")
    public MomoIpnResponse ipn(@RequestBody MomoIpnRequest request) {
        return momoIpnService.handle(request);
    }
}