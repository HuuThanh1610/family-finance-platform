package com.familyfinance.payment.provider;

import com.familyfinance.payment.config.MomoProperties;
import com.familyfinance.payment.entity.PaymentAttempt;
import com.familyfinance.payment.entity.PaymentOrder;
import com.familyfinance.payment.exception.PaymentConfigurationException;
import com.familyfinance.payment.provider.momo.MomoSignatureService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoMoSandboxAdapterTest {

    private MockWebServer server;
    private MoMoSandboxAdapter adapter;
    private MomoProperties properties;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        properties = new MomoProperties();
        properties.setPartnerCode("MOMO");
        properties.setAccessKey("access01");
        properties.setSecretKey("s3cr3tK3y");
        properties.setRedirectUrl("https://example.com/ok");
        properties.setIpnUrl("https://example.com/ipn");
        properties.setBaseUrl(server.url("/").toString().replaceAll("/$", ""));

        adapter = new MoMoSandboxAdapter(properties, RestClient.builder(), new MomoSignatureService("s3cr3tK3y"));
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void initiate_missingCredentials_throwsConfigurationError() {
        properties.setSecretKey("");
        PaymentOrder order = sampleOrder();
        PaymentAttempt attempt = sampleAttempt(order);

        assertThrows(PaymentConfigurationException.class, () -> adapter.initiate(order, attempt));
    }

    @Test
    void queryPaymentStatus_parsesPaidResponse() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"resultCode":0,"amount":100000,"transId":"momo-paid-9","message":"Success"}
                        """));

        var result = adapter.queryPaymentStatus(
                "f47ac10b-58cc-4372-a567-0e02b2c3d479",
                "req-query-1");

        assertEquals(true, result.paid());
        assertEquals(100_000L, result.amount());
        assertEquals("momo-paid-9", result.providerTxnId());
    }

    @Test
    void initiate_happyPath_parsesPayUrl() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"resultCode":0,"message":"Success","payUrl":"https://test.momo.vn/pay/abc","transId":"momo-txn-1"}
                        """));

        ProviderInitiateResult result = adapter.initiate(sampleOrder(), sampleAttempt(sampleOrder()));

        assertEquals("https://test.momo.vn/pay/abc", result.payUrl());
        assertEquals("momo-txn-1", result.providerTxnId());
        assertTrue(result.rawResponse().containsKey("payUrl"));
    }

    private static PaymentOrder sampleOrder() {
        PaymentOrder order = new PaymentOrder();
        order.setId(UUID.randomUUID());
        order.setAmount(100_000L);
        order.setPurpose("TOPUP");
        return order;
    }

    private static PaymentAttempt sampleAttempt(PaymentOrder order) {
        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setId(UUID.randomUUID());
        attempt.setPaymentOrder(order);
        attempt.setInitiatedAt(Instant.now());
        return attempt;
    }
}