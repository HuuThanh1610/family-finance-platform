package com.familyfinance.payment.service;

import com.familyfinance.payment.dto.FakePaymentIpnRequest;
import com.familyfinance.payment.dto.FakePaymentIpnResponse;
import com.familyfinance.payment.entity.PaymentOrder;
import com.familyfinance.payment.entity.WebhookInbox;
import com.familyfinance.payment.exception.BusinessException;
import com.familyfinance.payment.exception.ResourceNotFoundException;
import com.familyfinance.payment.provider.FakePaymentAdapter;
import com.familyfinance.payment.repository.PaymentOrderRepository;
import com.familyfinance.payment.repository.WebhookInboxRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class FakePaymentIpnService {

  private final WebhookInboxRepository webhookInboxRepository;
  private final PaymentOrderRepository paymentOrderRepository;
  private final PaymentOrderService paymentOrderService;

  public FakePaymentIpnService(
      WebhookInboxRepository webhookInboxRepository,
      PaymentOrderRepository paymentOrderRepository,
      PaymentOrderService paymentOrderService) {
    this.webhookInboxRepository = webhookInboxRepository;
    this.paymentOrderRepository = paymentOrderRepository;
    this.paymentOrderService = paymentOrderService;
  }

  @Transactional
  public FakePaymentIpnResponse simulatePaid(FakePaymentIpnRequest request) {
    UUID orderId = request.orderId();
    PaymentOrder order =
        paymentOrderRepository
            .findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("PaymentOrder", orderId));

    long amount = request.amount() != null ? request.amount() : order.getAmount();
    String providerTxnId =
        request.providerTxnId() != null && !request.providerTxnId().isBlank()
            ? request.providerTxnId()
            : "fake-sim-" + orderId;

    Optional<WebhookInbox> existing =
        webhookInboxRepository.findByProviderCodeAndIdempotencyKey(
            FakePaymentAdapter.PROVIDER_CODE, providerTxnId);
    if (existing.isPresent()) {
      WebhookInbox prior = existing.get();
      if ("PROCESSED".equals(prior.getStatus())) {
        return FakePaymentIpnResponse.success();
      }
      completeInbox(prior, orderId, amount, providerTxnId);
      return FakePaymentIpnResponse.success();
    }

    Instant now = Instant.now();
    WebhookInbox inbox = new WebhookInbox();
    inbox.setProviderCode(FakePaymentAdapter.PROVIDER_CODE);
    inbox.setIdempotencyKey(providerTxnId);
    inbox.setEventType("FAKE_IPN");
    inbox.setPayload(
        Map.of(
            "orderId",
            orderId.toString(),
            "amount",
            amount,
            "providerTxnId",
            providerTxnId,
            "status",
            "PAID"));
    inbox.setReceivedAt(now);
    inbox.setStatus("PENDING");

    try {
      webhookInboxRepository.save(inbox);
    } catch (DataIntegrityViolationException e) {
      return webhookInboxRepository
          .findByProviderCodeAndIdempotencyKey(FakePaymentAdapter.PROVIDER_CODE, providerTxnId)
          .map(
              prior -> {
                completeInbox(prior, orderId, amount, providerTxnId);
                return FakePaymentIpnResponse.success();
              })
          .orElse(FakePaymentIpnResponse.success());
    }

    completeInbox(inbox, orderId, amount, providerTxnId);
    return FakePaymentIpnResponse.success();
  }

  private void completeInbox(
      WebhookInbox inbox, UUID orderId, long amount, String providerTxnId) {
    try {
      paymentOrderService.applyProviderPaid(
          orderId,
          amount,
          FakePaymentAdapter.PROVIDER_CODE,
          providerTxnId,
          inbox.getPayload());
      inbox.setStatus("PROCESSED");
      inbox.setProcessedAt(Instant.now());
      webhookInboxRepository.save(inbox);
    } catch (RuntimeException ex) {
      if (ex instanceof BusinessException business && business.getStatus() == HttpStatus.CONFLICT) {
        inbox.setStatus("PROCESSED");
        inbox.setProcessedAt(Instant.now());
        webhookInboxRepository.save(inbox);
        return;
      }
      throw ex;
    }
  }
}