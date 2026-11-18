package com.familyfinance.payment.service;

import com.familyfinance.payment.config.PaymentProviderProperties;
import com.familyfinance.payment.domain.PaymentStatus;
import com.familyfinance.payment.entity.OutboxEvent;
import com.familyfinance.payment.entity.PaymentAttempt;
import com.familyfinance.payment.entity.PaymentOrder;
import com.familyfinance.payment.entity.ProviderTransaction;
import com.familyfinance.payment.exception.BusinessException;
import com.familyfinance.payment.repository.OutboxEventRepository;
import com.familyfinance.payment.repository.PaymentAttemptRepository;
import com.familyfinance.payment.repository.PaymentOrderRepository;
import com.familyfinance.payment.repository.ProviderTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
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
class PaymentOrderServiceTest {

    private static final UUID ORDER_ID = UUID.fromString("22000000-0000-4000-8000-000000000001");
    private static final UUID WALLET_ID = UUID.fromString("33000000-0000-4000-8000-000000000001");

    @Mock
    private PaymentOrderRepository repository;
    @Mock
    private PaymentAttemptRepository attemptRepository;
    @Mock
    private ProviderTransactionRepository providerTransactionRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;

    private PaymentOrderService service;

    @BeforeEach
    void setUp() {
        PaymentProviderProperties providerProperties = new PaymentProviderProperties();
        providerProperties.setActive("fake");
        service = new PaymentOrderService(
                repository,
                attemptRepository,
                providerTransactionRepository,
                outboxEventRepository,
                Map.of(),
                providerProperties);
    }

    @Test
    void applyProviderPaid_isNoOp_whenAlreadyCreditPending() {
        PaymentOrder order = creditPendingOrder();
        when(repository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        service.applyProviderPaid(ORDER_ID, 150_000L, "MOMO_SANDBOX", "txn-1", Map.of());

        verify(outboxEventRepository, never()).save(any());
        verify(repository, never()).save(any());
        verify(attemptRepository, never()).findTopByPaymentOrder_IdOrderByAttemptNoDesc(any());
    }

    @Test
    void applyProviderPaid_skipsDuplicateOutbox_afterFirstWrite() {
        PaymentOrder order = pendingOrder();
        when(repository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(attemptRepository.findTopByPaymentOrder_IdOrderByAttemptNoDesc(ORDER_ID))
                .thenReturn(Optional.of(sampleAttempt(order)));
        when(providerTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxEventRepository.existsByAggregateIdAndEventType(
                ORDER_ID, PaymentOrderService.EVENT_PAYMENT_PROVIDER_PAID)).thenReturn(true);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.applyProviderPaid(ORDER_ID, 150_000L, "MOMO_SANDBOX", "txn-2", Map.of());

        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void applyProviderPaid_rejectsMissingWalletId_beforeStateChange() {
        PaymentOrder order = pendingOrder();
        order.setWalletId(null);
        when(repository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(BusinessException.class, () ->
                service.applyProviderPaid(ORDER_ID, 150_000L, "MOMO_SANDBOX", "txn-1", Map.of()));

        verify(repository, never()).save(any());
        assertEquals(PaymentStatus.PENDING_PROVIDER.name(), order.getStatus());
    }

    @Test
    void applyProviderPaid_writesSingleOutbox_onFirstSuccess() {
        PaymentOrder order = pendingOrder();
        when(repository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(attemptRepository.findTopByPaymentOrder_IdOrderByAttemptNoDesc(ORDER_ID))
                .thenReturn(Optional.of(sampleAttempt(order)));
        when(providerTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxEventRepository.existsByAggregateIdAndEventType(
                ORDER_ID, PaymentOrderService.EVENT_PAYMENT_PROVIDER_PAID)).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.applyProviderPaid(ORDER_ID, 150_000L, "MOMO_SANDBOX", "txn-1", Map.of());

        assertEquals(PaymentStatus.CREDIT_PENDING.name(), order.getStatus());
        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(1)).save(outboxCaptor.capture());
        assertEquals(PaymentOrderService.EVENT_PAYMENT_PROVIDER_PAID, outboxCaptor.getValue().getEventType());
    }

    @Test
    void ensurePaymentProviderPaidOutbox_replaysMissingOutbox() {
        PaymentOrder order = creditPendingOrder();
        ProviderTransaction paid = new ProviderTransaction();
        paid.setAmount(150_000L);
        paid.setProviderCode("MOMO_SANDBOX");
        paid.setProviderTxnId("txn-99");

        when(repository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(providerTransactionRepository.findTopByPaymentAttempt_PaymentOrder_IdAndStatusOrderByRecordedAtDesc(
                ORDER_ID, "PAID")).thenReturn(Optional.of(paid));
        when(outboxEventRepository.existsByAggregateIdAndEventType(
                ORDER_ID, PaymentOrderService.EVENT_PAYMENT_PROVIDER_PAID)).thenReturn(false);

        service.ensurePaymentProviderPaidOutbox(ORDER_ID);

        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    private static PaymentOrder pendingOrder() {
        PaymentOrder order = new PaymentOrder();
        order.setId(ORDER_ID);
        order.setFamilyId(UUID.randomUUID());
        order.setWalletId(WALLET_ID);
        order.setAmount(150_000L);
        order.setCurrencyCode("VND");
        order.setStatus(PaymentStatus.PENDING_PROVIDER.name());
        return order;
    }

    private static PaymentOrder creditPendingOrder() {
        PaymentOrder order = pendingOrder();
        order.setStatus(PaymentStatus.CREDIT_PENDING.name());
        return order;
    }

    private static PaymentAttempt sampleAttempt(PaymentOrder order) {
        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setId(UUID.randomUUID());
        attempt.setPaymentOrder(order);
        attempt.setAttemptNo(1);
        attempt.setInitiatedAt(Instant.now());
        return attempt;
    }
}