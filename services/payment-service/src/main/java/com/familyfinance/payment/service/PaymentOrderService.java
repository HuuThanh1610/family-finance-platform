package com.familyfinance.payment.service;

import com.familyfinance.payment.domain.PaymentOrderStateMachine;
import com.familyfinance.payment.domain.PaymentStatus;
import com.familyfinance.payment.dto.CreatePaymentRequest;
import com.familyfinance.payment.dto.CreatePaymentResponse;
import com.familyfinance.payment.dto.PaymentOrderResponse;
import com.familyfinance.payment.entity.OutboxEvent;
import com.familyfinance.payment.entity.PaymentAttempt;
import com.familyfinance.payment.entity.PaymentOrder;
import com.familyfinance.payment.entity.ProviderTransaction;
import com.familyfinance.payment.exception.BusinessException;
import com.familyfinance.payment.exception.ResourceNotFoundException;
import com.familyfinance.payment.provider.PaymentProvider;
import com.familyfinance.payment.provider.ProviderInitiateResult;
import com.familyfinance.payment.repository.OutboxEventRepository;
import com.familyfinance.payment.repository.PaymentAttemptRepository;
import com.familyfinance.payment.repository.PaymentOrderRepository;
import com.familyfinance.payment.repository.ProviderTransactionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentOrderService {

    private static final String AGGREGATE_TYPE = "payment_order";
    public static final String EVENT_PAYMENT_PROVIDER_PAID = "PaymentProviderPaid";

    private final PaymentOrderRepository repository;
    private final PaymentAttemptRepository attemptRepository;
    private final ProviderTransactionRepository providerTransactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final Map<String, PaymentProvider> paymentProviders;

    public PaymentOrderService(PaymentOrderRepository repository,
                               PaymentAttemptRepository attemptRepository,
                               ProviderTransactionRepository providerTransactionRepository,
                               OutboxEventRepository outboxEventRepository,
                               Map<String, PaymentProvider> paymentProviders) {
        this.repository = repository;
        this.attemptRepository = attemptRepository;
        this.providerTransactionRepository = providerTransactionRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.paymentProviders = paymentProviders;
    }

    @Transactional(readOnly = true)
    public List<PaymentOrderResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PaymentOrderResponse findById(UUID id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentOrder", id));
    }

    @Transactional
    public CreatePaymentResponse create(CreatePaymentRequest request) {
        PaymentProvider provider = paymentProviders.get(request.providerCode());
        if (provider == null) {
            throw new BusinessException("Unknown payment provider: " + request.providerCode(), HttpStatus.BAD_REQUEST);
        }

        Instant now = Instant.now();
        PaymentOrder order = new PaymentOrder();
        order.setFamilyId(request.familyId());
        order.setPayerMemberId(request.payerMemberId());
        order.setWalletId(request.walletId());
        order.setAmount(request.amount());
        order.setCurrencyCode("VND");
        order.setPurpose(request.purpose());
        order.setExternalReference(request.externalReference());
        order.setStatus(PaymentStatus.CREATED.name());
        order.setExpiresAt(now.plus(24, ChronoUnit.HOURS));
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order = repository.save(order);

        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setPaymentOrder(order);
        attempt.setAttemptNo(1);
        attempt.setProviderCode(provider.providerCode());
        attempt.setStatus("INITIATED");
        attempt.setInitiatedAt(now);
        attempt.setCreatedAt(now);
        attempt.setUpdatedAt(now);
        attempt = attemptRepository.save(attempt);

        ProviderInitiateResult providerResult;
        try {
            providerResult = provider.initiate(order, attempt);
        } catch (Exception e) {
            PaymentOrderStateMachine.transition(order, PaymentStatus.FAILED);
            attempt.setStatus("FAILED");
            attempt.setFailureReason(truncate(e.getMessage()));
            attempt.setCompletedAt(Instant.now());
            attempt.setUpdatedAt(Instant.now());
            repository.save(order);
            attemptRepository.save(attempt);
            throw e;
        }

        PaymentOrderStateMachine.transition(order, PaymentStatus.PENDING_PROVIDER);
        attempt.setStatus("PENDING");
        attempt.setUpdatedAt(Instant.now());
        repository.save(order);
        attemptRepository.save(attempt);

        saveProviderTransaction(attempt, provider.providerCode(), providerResult, now);

        PaymentOrderResponse orderResponse = toResponse(order);
        return new CreatePaymentResponse(
                order.getId(),
                order.getStatus(),
                providerResult.payUrl(),
                provider.providerCode(),
                orderResponse
        );
    }

    @Transactional
    public PaymentOrderResponse markCompleted(UUID id) {
        PaymentOrder order = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentOrder", id));
        PaymentStatus current = PaymentStatus.fromDbValue(order.getStatus());
        if (current == PaymentStatus.COMPLETED) {
            return toResponse(order);
        }
        if (current != PaymentStatus.CREDIT_PENDING) {
            throw new InvalidPaymentStateForIpnException(id, current);
        }
        PaymentOrderStateMachine.transition(order, PaymentStatus.COMPLETED);
        repository.save(order);
        return toResponse(order);
    }

    @Transactional
    public void applyProviderPaid(UUID orderId, long amount, String providerCode, String providerTxnId,
                                  Map<String, Object> webhookPayload) {
        PaymentOrder order = repository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentOrder", orderId));

        PaymentStatus current = PaymentStatus.fromDbValue(order.getStatus());
        if (current == PaymentStatus.CREDIT_PENDING || current == PaymentStatus.COMPLETED) {
            return;
        }
        if (current != PaymentStatus.PENDING_PROVIDER && current != PaymentStatus.PROVIDER_PAID) {
            throw new InvalidPaymentStateForIpnException(order.getId(), current);
        }

        if (!Long.valueOf(amount).equals(order.getAmount())) {
            throw new BusinessException("IPN amount mismatch for order " + orderId, HttpStatus.BAD_REQUEST);
        }

        Instant now = Instant.now();
        if (current == PaymentStatus.PENDING_PROVIDER) {
            PaymentOrderStateMachine.transition(order, PaymentStatus.PROVIDER_PAID);
        }
        PaymentOrderStateMachine.transition(order, PaymentStatus.CREDIT_PENDING);
        repository.save(order);

        PaymentAttempt attempt = attemptRepository.findTopByPaymentOrder_IdOrderByAttemptNoDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentAttempt for order", orderId));
        attempt.setStatus("SUCCEEDED");
        attempt.setCompletedAt(now);
        attempt.setUpdatedAt(now);
        attemptRepository.save(attempt);

        ProviderTransaction txn = new ProviderTransaction();
        txn.setPaymentAttempt(attempt);
        txn.setProviderTxnId(providerTxnId);
        txn.setProviderCode(providerCode);
        txn.setAmount(amount);
        txn.setStatus("PAID");
        txn.setRawResponse(webhookPayload);
        txn.setRecordedAt(now);
        txn.setCreatedAt(now);
        try {
            providerTransactionRepository.save(txn);
        } catch (DataIntegrityViolationException ignored) {
            // duplicate provider txn id — idempotent
        }

        if (order.getWalletId() == null) {
            throw new BusinessException("Payment order missing walletId for credit", HttpStatus.CONFLICT);
        }

        OutboxEvent outbox = new OutboxEvent();
        outbox.setEventType(EVENT_PAYMENT_PROVIDER_PAID);
        outbox.setAggregateType(AGGREGATE_TYPE);
        outbox.setAggregateId(orderId);
        outbox.setPayload(Map.of(
                "paymentOrderId", orderId.toString(),
                "familyId", order.getFamilyId().toString(),
                "walletId", order.getWalletId().toString(),
                "amount", amount,
                "currencyCode", order.getCurrencyCode(),
                "providerCode", providerCode,
                "providerTxnId", providerTxnId
        ));
        outbox.setCreatedAt(now);
        outbox.setStatus("PENDING");
        outboxEventRepository.save(outbox);
    }

    PaymentOrderResponse toResponse(PaymentOrder entity) {
        return new PaymentOrderResponse(
                entity.getId(),
                entity.getFamilyId(),
                entity.getPayerMemberId(),
                entity.getAmount(),
                entity.getCurrencyCode(),
                entity.getPurpose(),
                entity.getExternalReference(),
                entity.getStatus(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private void saveProviderTransaction(PaymentAttempt attempt, String providerCode,
                                         ProviderInitiateResult providerResult, Instant now) {
        ProviderTransaction txn = new ProviderTransaction();
        txn.setPaymentAttempt(attempt);
        txn.setProviderTxnId(providerResult.providerTxnId() != null
                ? providerResult.providerTxnId()
                : providerResult.providerRequestId());
        txn.setProviderCode(providerCode);
        txn.setAmount(attempt.getPaymentOrder().getAmount());
        txn.setStatus("INITIATED");
        txn.setRawResponse(providerResult.rawResponse());
        txn.setRecordedAt(now);
        txn.setCreatedAt(now);
        providerTransactionRepository.save(txn);
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    public static class InvalidPaymentStateForIpnException extends BusinessException {
        public InvalidPaymentStateForIpnException(UUID orderId, PaymentStatus status) {
            super("Payment order " + orderId + " in state " + status + " cannot accept provider IPN",
                    HttpStatus.CONFLICT);
        }
    }
}