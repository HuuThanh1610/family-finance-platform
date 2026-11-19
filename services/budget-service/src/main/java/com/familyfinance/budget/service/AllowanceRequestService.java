package com.familyfinance.budget.service;

import com.familyfinance.budget.domain.AllowanceRequestStateMachine;
import com.familyfinance.budget.domain.AllowanceRequestStatus;
import com.familyfinance.budget.dto.*;
import com.familyfinance.budget.entity.AllowanceRequest;
import com.familyfinance.budget.entity.ApprovalHistory;
import com.familyfinance.budget.entity.OutboxEvent;
import com.familyfinance.budget.exception.ResourceNotFoundException;
import com.familyfinance.budget.repository.AllowanceRequestRepository;
import com.familyfinance.budget.repository.ApprovalHistoryRepository;
import com.familyfinance.budget.repository.OutboxEventRepository;
import com.familyfinance.budget.security.FamilyAuthorizationClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AllowanceRequestService {

    private static final String AGGREGATE_TYPE = "allowance_request";
    public static final String EVENT_ALLOWANCE_DISBURSEMENT_REQUESTED = "AllowanceDisbursementRequested";

    private final AllowanceRequestRepository requestRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final FamilyAuthorizationClient familyAuthorizationClient;

    public AllowanceRequestService(
            AllowanceRequestRepository requestRepository,
            ApprovalHistoryRepository approvalHistoryRepository,
            OutboxEventRepository outboxEventRepository,
            FamilyAuthorizationClient familyAuthorizationClient) {
        this.requestRepository = requestRepository;
        this.approvalHistoryRepository = approvalHistoryRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.familyAuthorizationClient = familyAuthorizationClient;
    }

    @Transactional(readOnly = true)
    public List<AllowanceRequestResponse> findByFamily(UUID familyId) {
        List<AllowanceRequest> rows = familyId == null
                ? requestRepository.findAll()
                : requestRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
        return rows.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AllowanceRequestResponse findById(UUID id) {
        return requestRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("AllowanceRequest", id));
    }

    @Transactional
    public AllowanceRequestResponse create(CreateAllowanceRequest request) {
        Instant now = Instant.now();
        AllowanceRequest entity = new AllowanceRequest();
        entity.setFamilyId(request.familyId());
        entity.setRequesterMemberId(request.requesterMemberId());
        entity.setAmount(request.amount());
        entity.setReason(request.reason());
        entity.setStatus(AllowanceRequestStatus.PENDING.name());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity = requestRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional
    public AllowanceRequestResponse approve(UUID requestId, ApproveAllowanceRequest body, UUID actorUserProfileId) {
        AllowanceRequest request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("AllowanceRequest", requestId));
        familyAuthorizationClient.requirePermission(request.getFamilyId(), actorUserProfileId, "budget.approve");

        AllowanceRequestStateMachine.transition(request, AllowanceRequestStatus.APPROVED_PENDING_DISBURSEMENT);
        requestRepository.save(request);

        Instant now = Instant.now();
        ApprovalHistory history = new ApprovalHistory();
        history.setAllowanceRequest(request);
        history.setApproverMemberId(body.approverMemberId());
        history.setDecision("APPROVED");
        history.setComment(body.comment());
        history.setDecidedAt(now);
        approvalHistoryRepository.save(history);

        OutboxEvent outbox = new OutboxEvent();
        outbox.setEventType(EVENT_ALLOWANCE_DISBURSEMENT_REQUESTED);
        outbox.setAggregateType(AGGREGATE_TYPE);
        outbox.setAggregateId(request.getId());
        outbox.setPayload(Map.of(
                "requestId", request.getId().toString(),
                "familyId", request.getFamilyId().toString(),
                "requesterMemberId", request.getRequesterMemberId().toString(),
                "amount", request.getAmount(),
                "currencyCode", "VND",
                "reason", request.getReason() != null ? request.getReason() : ""
        ));
        outbox.setCreatedAt(now);
        outbox.setStatus("PENDING");
        outboxEventRepository.save(outbox);

        return toResponse(request);
    }

    /**
     * Wallet saga success. Idempotent when already DISBURSED.
     * Does not mark DISBURSED merely because an outbox was published.
     */
    @Transactional
    public AllowanceRequestResponse applyDisbursementCompleted(UUID requestId) {
        AllowanceRequest request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("AllowanceRequest", requestId));
        if (AllowanceRequestStatus.DISBURSED.name().equals(request.getStatus())) {
            return toResponse(request);
        }
        AllowanceRequestStateMachine.transition(request, AllowanceRequestStatus.DISBURSED);
        requestRepository.save(request);
        return toResponse(request);
    }

    /**
     * Permanent wallet failure (e.g. insufficient funds). Idempotent when already failed.
     */
    @Transactional
    public AllowanceRequestResponse applyDisbursementFailed(UUID requestId, String errorCode) {
        AllowanceRequest request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("AllowanceRequest", requestId));
        if (AllowanceRequestStatus.DISBURSEMENT_FAILED.name().equals(request.getStatus())) {
            return toResponse(request);
        }
        if (AllowanceRequestStatus.DISBURSED.name().equals(request.getStatus())) {
            return toResponse(request);
        }
        AllowanceRequestStateMachine.transition(request, AllowanceRequestStatus.DISBURSEMENT_FAILED);
        requestRepository.save(request);
        return toResponse(request);
    }

    @Transactional
    public AllowanceRequestResponse reject(UUID requestId, RejectAllowanceRequest body) {
        AllowanceRequest request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("AllowanceRequest", requestId));

        AllowanceRequestStateMachine.transition(request, AllowanceRequestStatus.REJECTED);
        requestRepository.save(request);

        ApprovalHistory history = new ApprovalHistory();
        history.setAllowanceRequest(request);
        history.setApproverMemberId(body.approverMemberId());
        history.setDecision("REJECTED");
        history.setComment(body.comment());
        history.setDecidedAt(Instant.now());
        approvalHistoryRepository.save(history);

        return toResponse(request);
    }

    @Transactional
    public AllowanceRequestResponse cancel(UUID requestId) {
        AllowanceRequest request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("AllowanceRequest", requestId));

        AllowanceRequestStateMachine.transition(request, AllowanceRequestStatus.CANCELLED);
        requestRepository.save(request);
        return toResponse(request);
    }

    @Transactional
    public AllowanceRequestResponse markDisbursed(UUID requestId) {
        AllowanceRequest request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("AllowanceRequest", requestId));

        AllowanceRequestStateMachine.transition(request, AllowanceRequestStatus.DISBURSED);
        requestRepository.save(request);
        return toResponse(request);
    }

    @Transactional
    public AllowanceRequestResponse markDisbursementFailed(UUID requestId) {
        AllowanceRequest request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("AllowanceRequest", requestId));

        AllowanceRequestStateMachine.transition(request, AllowanceRequestStatus.DISBURSEMENT_FAILED);
        requestRepository.save(request);
        return toResponse(request);
    }

    private AllowanceRequestResponse toResponse(AllowanceRequest entity) {
        return new AllowanceRequestResponse(
                entity.getId(),
                entity.getFamilyId(),
                entity.getRequesterMemberId(),
                entity.getAmount(),
                entity.getReason(),
                entity.getStatus(),
                entity.getDecidedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}