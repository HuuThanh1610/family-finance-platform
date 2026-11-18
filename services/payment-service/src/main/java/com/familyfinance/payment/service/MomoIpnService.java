package com.familyfinance.payment.service;

import com.familyfinance.payment.config.MomoProperties;
import com.familyfinance.payment.dto.MomoIpnRequest;
import com.familyfinance.payment.dto.MomoIpnResponse;
import com.familyfinance.payment.entity.WebhookInbox;
import com.familyfinance.payment.exception.PaymentVerificationException;
import com.familyfinance.payment.provider.MoMoSandboxAdapter;
import com.familyfinance.payment.provider.momo.MomoSignatureService;
import com.familyfinance.payment.repository.WebhookInboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class MomoIpnService {

    private final MomoProperties momoProperties;
    private final MomoSignatureService signatureService;
    private final WebhookInboxRepository webhookInboxRepository;
    private final PaymentOrderService paymentOrderService;
    private final ObjectMapper objectMapper;

    public MomoIpnService(MomoProperties momoProperties,
                          MomoSignatureService signatureService,
                          WebhookInboxRepository webhookInboxRepository,
                          PaymentOrderService paymentOrderService,
                          ObjectMapper objectMapper) {
        this.momoProperties = momoProperties;
        this.signatureService = signatureService;
        this.webhookInboxRepository = webhookInboxRepository;
        this.paymentOrderService = paymentOrderService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MomoIpnResponse handle(MomoIpnRequest ipn) {
        if (!signatureService.verifyIpn(ipn)) {
            throw new PaymentVerificationException("Invalid MoMo IPN signature");
        }

        String configuredPartner = momoProperties.getPartnerCode();
        if (configuredPartner != null && !configuredPartner.isBlank()
                && ipn.partnerCode() != null
                && !configuredPartner.equals(ipn.partnerCode())) {
            throw new PaymentVerificationException("MoMo partnerCode mismatch");
        }

        if (ipn.resultCode() == null || ipn.resultCode() != 0) {
            throw new PaymentVerificationException("MoMo IPN resultCode is not success");
        }

        UUID orderId;
        try {
            orderId = UUID.fromString(ipn.orderId());
        } catch (IllegalArgumentException e) {
            throw new PaymentVerificationException("Invalid orderId in MoMo IPN");
        }

        String idempotencyKey = ipn.transId() != null ? ipn.transId() : ipn.requestId();
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new PaymentVerificationException("Missing transId/requestId in MoMo IPN");
        }

        Optional<WebhookInbox> existing = webhookInboxRepository
                .findByProviderCodeAndIdempotencyKey(MoMoSandboxAdapter.PROVIDER_CODE, idempotencyKey);
        if (existing.isPresent()) {
            WebhookInbox prior = existing.get();
            if ("PROCESSED".equals(prior.getStatus())) {
                return MomoIpnResponse.success();
            }
            return completeInboxProcessing(prior, orderId, ipn);
        }

        Instant now = Instant.now();
        WebhookInbox inbox = new WebhookInbox();
        inbox.setProviderCode(MoMoSandboxAdapter.PROVIDER_CODE);
        inbox.setIdempotencyKey(idempotencyKey);
        inbox.setEventType("MOMO_IPN");
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = objectMapper.convertValue(ipn, Map.class);
        inbox.setPayload(payload);
        inbox.setReceivedAt(now);
        inbox.setStatus("PENDING");

        try {
            webhookInboxRepository.save(inbox);
        } catch (DataIntegrityViolationException e) {
            return webhookInboxRepository
                    .findByProviderCodeAndIdempotencyKey(MoMoSandboxAdapter.PROVIDER_CODE, idempotencyKey)
                    .map(prior -> completeInboxProcessing(prior, orderId, ipn))
                    .orElse(MomoIpnResponse.success());
        }

        return completeInboxProcessing(inbox, orderId, ipn);
    }

    private MomoIpnResponse completeInboxProcessing(WebhookInbox inbox, UUID orderId, MomoIpnRequest ipn) {
        String idempotencyKey = ipn.transId() != null ? ipn.transId() : ipn.requestId();
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = objectMapper.convertValue(ipn, Map.class);

        paymentOrderService.applyProviderPaid(
                orderId,
                ipn.amount(),
                MoMoSandboxAdapter.PROVIDER_CODE,
                idempotencyKey,
                payload
        );

        inbox.setStatus("PROCESSED");
        inbox.setProcessedAt(Instant.now());
        webhookInboxRepository.save(inbox);
        return MomoIpnResponse.success();
    }
}