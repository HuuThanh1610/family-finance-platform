package com.familyfinance.payment.provider;

import com.familyfinance.payment.config.MomoProperties;
import com.familyfinance.payment.entity.PaymentAttempt;
import com.familyfinance.payment.entity.PaymentOrder;
import com.familyfinance.payment.exception.BusinessException;
import com.familyfinance.payment.exception.PaymentConfigurationException;
import com.familyfinance.payment.provider.momo.MomoSignatureService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class MoMoSandboxAdapter implements PaymentProvider {

    public static final String PROVIDER_CODE = "MOMO_SANDBOX";

    private static final String CREATE_PATH = "/v2/gateway/api/create";
    private static final String QUERY_PATH = "/v2/gateway/api/query";
    private static final String REQUEST_TYPE = "captureWallet";

    private final MomoProperties properties;
    private final RestClient restClient;
    private final MomoSignatureService signatureService;
    private final ObjectMapper objectMapper;

    public MoMoSandboxAdapter(MomoProperties properties, RestClient.Builder restClientBuilder,
                              MomoSignatureService signatureService) {
        this.properties = properties;
        this.signatureService = signatureService;
        this.objectMapper = new ObjectMapper();
        this.restClient = restClientBuilder
                .baseUrl(trimTrailingSlash(properties.getBaseUrl()))
                .build();
    }

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public ProviderInitiateResult initiate(PaymentOrder order, PaymentAttempt attempt) {
        try {
            properties.requireConfigured();
        } catch (PaymentConfigurationException e) {
            throw e;
        }

        String requestId = UUID.randomUUID().toString();
        String orderId = order.getId().toString();
        String orderInfo = order.getPurpose();
        String extraData = "";
        long amount = order.getAmount();

        String signature = signatureService.signCreatePayment(
                properties.getAccessKey(),
                amount,
                extraData,
                properties.getIpnUrl(),
                orderId,
                orderInfo,
                properties.getPartnerCode(),
                properties.getRedirectUrl(),
                requestId,
                REQUEST_TYPE
        );

        Map<String, Object> body = new HashMap<>();
        body.put("partnerCode", properties.getPartnerCode());
        body.put("accessKey", properties.getAccessKey());
        body.put("requestId", requestId);
        body.put("amount", String.valueOf(amount));
        body.put("orderId", orderId);
        body.put("orderInfo", orderInfo);
        body.put("redirectUrl", properties.getRedirectUrl());
        body.put("ipnUrl", properties.getIpnUrl());
        body.put("extraData", extraData);
        body.put("requestType", REQUEST_TYPE);
        body.put("signature", signature);
        body.put("lang", "vi");

        try {
            String responseBody = restClient.post()
                    .uri(CREATE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            int resultCode = root.path("resultCode").asInt(-1);
            if (resultCode != 0) {
                throw new BusinessException(
                        "MoMo create payment failed: resultCode=" + resultCode
                                + " message=" + root.path("message").asText(),
                        org.springframework.http.HttpStatus.BAD_GATEWAY);
            }
            String payUrl = root.path("payUrl").asText(null);
            String providerTxnId = root.path("transId").asText(requestId);
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = objectMapper.convertValue(root, Map.class);
            return new ProviderInitiateResult(payUrl, requestId, providerTxnId, raw);
        } catch (RestClientException e) {
            throw new BusinessException("MoMo HTTP call failed: " + e.getMessage(),
                    org.springframework.http.HttpStatus.BAD_GATEWAY);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("MoMo response parse failed: " + e.getMessage(),
                    org.springframework.http.HttpStatus.BAD_GATEWAY);
        }
    }

    public ProviderQueryResult queryPaymentStatus(String orderId, String requestId) {
        try {
            properties.requireConfigured();
        } catch (PaymentConfigurationException e) {
            throw e;
        }

        String queryRequestId = requestId != null && !requestId.isBlank()
                ? requestId
                : UUID.randomUUID().toString();
        String signature = signatureService.signQuery(
                properties.getAccessKey(),
                orderId,
                properties.getPartnerCode(),
                queryRequestId
        );

        Map<String, Object> body = new HashMap<>();
        body.put("partnerCode", properties.getPartnerCode());
        body.put("requestId", queryRequestId);
        body.put("orderId", orderId);
        body.put("lang", "vi");
        body.put("signature", signature);

        try {
            String responseBody = restClient.post()
                    .uri(QUERY_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            int resultCode = root.path("resultCode").asInt(-1);
            long amount = root.path("amount").asLong(0);
            String transId = root.path("transId").asText("");
            boolean paid = resultCode == 0;
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = objectMapper.convertValue(root, Map.class);
            return new ProviderQueryResult(paid, amount, transId, resultCode, raw);
        } catch (RestClientException e) {
            throw new BusinessException("MoMo query HTTP failed: " + e.getMessage(),
                    org.springframework.http.HttpStatus.BAD_GATEWAY);
        } catch (Exception e) {
            throw new BusinessException("MoMo query parse failed: " + e.getMessage(),
                    org.springframework.http.HttpStatus.BAD_GATEWAY);
        }
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://test-payment.momo.vn";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}