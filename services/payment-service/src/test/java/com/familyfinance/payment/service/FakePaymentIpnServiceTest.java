package com.familyfinance.payment.service;

import com.familyfinance.payment.dto.FakePaymentIpnRequest;
import com.familyfinance.payment.entity.PaymentOrder;
import com.familyfinance.payment.entity.WebhookInbox;
import com.familyfinance.payment.repository.PaymentOrderRepository;
import com.familyfinance.payment.repository.WebhookInboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FakePaymentIpnServiceTest {

  private static final UUID ORDER_ID = UUID.fromString("22000000-0000-4000-8000-000000000099");

  @Mock private WebhookInboxRepository webhookInboxRepository;
  @Mock private PaymentOrderRepository paymentOrderRepository;
  @Mock private PaymentOrderService paymentOrderService;

  private FakePaymentIpnService service;

  @BeforeEach
  void setUp() {
    service = new FakePaymentIpnService(webhookInboxRepository, paymentOrderRepository, paymentOrderService);
  }

  @Test
  void simulatePaid_appliesProviderPaid_oncePerTxnId() {
    PaymentOrder order = new PaymentOrder();
    order.setId(ORDER_ID);
    order.setAmount(100_000L);
    when(paymentOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
    when(webhookInboxRepository.findByProviderCodeAndIdempotencyKey(anyString(), anyString()))
        .thenReturn(Optional.empty());
    when(webhookInboxRepository.save(any(WebhookInbox.class))).thenAnswer(inv -> inv.getArgument(0));

    service.simulatePaid(new FakePaymentIpnRequest(ORDER_ID, null, "fake-txn-1"));

    verify(paymentOrderService, times(1))
        .applyProviderPaid(eq(ORDER_ID), eq(100_000L), anyString(), eq("fake-txn-1"), any());
  }

  @Test
  void simulatePaid_skipsSecondCall_whenInboxAlreadyProcessed() {
    PaymentOrder order = new PaymentOrder();
    order.setId(ORDER_ID);
    order.setAmount(100_000L);
    when(paymentOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

    WebhookInbox processed = new WebhookInbox();
    processed.setStatus("PROCESSED");
    processed.setProviderCode("FAKE_PAYMENT");
    processed.setIdempotencyKey("fake-txn-1");
    when(webhookInboxRepository.findByProviderCodeAndIdempotencyKey(anyString(), eq("fake-txn-1")))
        .thenReturn(Optional.of(processed));

    service.simulatePaid(new FakePaymentIpnRequest(ORDER_ID, null, "fake-txn-1"));

    verify(paymentOrderService, never()).applyProviderPaid(any(), anyLong(), anyString(), anyString(), any());
  }
}