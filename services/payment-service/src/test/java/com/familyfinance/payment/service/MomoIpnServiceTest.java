package com.familyfinance.payment.service;

import com.familyfinance.payment.config.MomoProperties;
import com.familyfinance.payment.dto.MomoIpnRequest;
import com.familyfinance.payment.dto.MomoIpnResponse;
import com.familyfinance.payment.entity.WebhookInbox;
import com.familyfinance.payment.exception.PaymentVerificationException;
import com.familyfinance.payment.provider.momo.MomoSignatureService;
import com.familyfinance.payment.repository.WebhookInboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MomoIpnServiceTest {

    private static final String SECRET = "s3cr3tK3y";
    private static final UUID ORDER_ID = UUID.fromString("11000000-0000-4000-8000-000000000099");

    @Mock
    private WebhookInboxRepository webhookInboxRepository;

    @Mock
    private PaymentOrderService paymentOrderService;

    private MomoIpnService momoIpnService;
    private MomoSignatureService signatureService;
    private MomoProperties momoProperties;

    @BeforeEach
    void setUp() {
        momoProperties = new MomoProperties();
        momoProperties.setPartnerCode("MOMO");
        momoProperties.setSecretKey(SECRET);
        signatureService = new MomoSignatureService(SECRET);
        momoIpnService = new MomoIpnService(
                momoProperties,
                signatureService,
                webhookInboxRepository,
                paymentOrderService,
                new ObjectMapper()
        );
    }

    @Test
    void handle_rejectsInvalidSignature() {
        MomoIpnRequest ipn = baseIpn("bad-signature");
        assertThrows(PaymentVerificationException.class, () -> momoIpnService.handle(ipn));
        verify(webhookInboxRepository, never()).save(any());
    }

    @Test
    void handle_duplicateIpn_isIdempotent() {
        MomoIpnRequest ipn = signedIpn();
        WebhookInbox processed = new WebhookInbox();
        processed.setStatus("PROCESSED");
        when(webhookInboxRepository.findByProviderCodeAndIdempotencyKey("MOMO_SANDBOX", "999888777"))
                .thenReturn(Optional.of(processed));

        MomoIpnResponse response = momoIpnService.handle(ipn);

        assertEquals(0, response.resultCode());
        verify(paymentOrderService, never()).applyProviderPaid(any(), any(Long.class), any(), any(), any());
    }

    @Test
    void handle_pendingInbox_retriesApplyProviderPaid() {
        MomoIpnRequest ipn = signedIpn();
        WebhookInbox pending = new WebhookInbox();
        pending.setStatus("PENDING");
        when(webhookInboxRepository.findByProviderCodeAndIdempotencyKey("MOMO_SANDBOX", "999888777"))
                .thenReturn(Optional.of(pending));
        when(webhookInboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MomoIpnResponse response = momoIpnService.handle(ipn);

        assertEquals(0, response.resultCode());
        verify(paymentOrderService).applyProviderPaid(
                eq(ORDER_ID), eq(150_000L), eq("MOMO_SANDBOX"), eq("999888777"), any());
        verify(webhookInboxRepository).save(pending);
        assertEquals("PROCESSED", pending.getStatus());
    }

    @Test
    void handle_validIpn_persistsInboxAndAppliesProviderPaid() {
        MomoIpnRequest ipn = signedIpn();
        when(webhookInboxRepository.findByProviderCodeAndIdempotencyKey("MOMO_SANDBOX", "999888777"))
                .thenReturn(Optional.empty());
        when(webhookInboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MomoIpnResponse response = momoIpnService.handle(ipn);

        assertEquals(0, response.resultCode());
        ArgumentCaptor<WebhookInbox> inboxCaptor = ArgumentCaptor.forClass(WebhookInbox.class);
        verify(webhookInboxRepository, times(2)).save(inboxCaptor.capture());
        assertEquals("PROCESSED", inboxCaptor.getAllValues().get(1).getStatus());

        verify(paymentOrderService).applyProviderPaid(
                eq(ORDER_ID),
                eq(150_000L),
                eq("MOMO_SANDBOX"),
                eq("999888777"),
                any()
        );
    }

    private MomoIpnRequest signedIpn() {
        MomoIpnRequest unsigned = baseIpn(null);
        String signature = signatureService.signIpn(unsigned);
        return new MomoIpnRequest(
                unsigned.partnerCode(), unsigned.accessKey(), unsigned.amount(), unsigned.orderId(),
                unsigned.orderInfo(), unsigned.orderType(), unsigned.transId(), unsigned.resultCode(),
                unsigned.message(), unsigned.payType(), unsigned.responseTime(), unsigned.requestId(),
                unsigned.extraData(), signature
        );
    }

    private static MomoIpnRequest baseIpn(String signature) {
        return new MomoIpnRequest(
                "MOMO",
                "access01",
                150_000L,
                ORDER_ID.toString(),
                "TOPUP",
                "momo_wallet",
                "999888777",
                0L,
                "Success",
                "qr",
                "1700000000000",
                "req-ipn-1",
                "",
                signature
        );
    }
}