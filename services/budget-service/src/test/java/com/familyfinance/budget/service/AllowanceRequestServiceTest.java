package com.familyfinance.budget.service;

import com.familyfinance.budget.domain.AllowanceRequestStatus;
import com.familyfinance.budget.dto.ApproveAllowanceRequest;
import com.familyfinance.budget.dto.RejectAllowanceRequest;
import com.familyfinance.budget.entity.AllowanceRequest;
import com.familyfinance.budget.entity.OutboxEvent;
import com.familyfinance.budget.exception.InvalidAllowanceStateException;
import com.familyfinance.budget.repository.AllowanceRequestRepository;
import com.familyfinance.budget.repository.ApprovalHistoryRepository;
import com.familyfinance.budget.repository.OutboxEventRepository;
import com.familyfinance.budget.security.FamilyAuthorizationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllowanceRequestServiceTest {

    @Mock AllowanceRequestRepository requestRepository;
    @Mock ApprovalHistoryRepository approvalHistoryRepository;
    @Mock OutboxEventRepository outboxEventRepository;
    @Mock FamilyAuthorizationClient familyAuthorizationClient;

    AllowanceRequestService service;

  UUID requestId = UUID.fromString("12000000-0000-4000-8000-000000000002");
    UUID approverId = UUID.fromString("c1000000-0000-4000-8000-000000000002");
    UUID actorProfileId = UUID.fromString("b1000000-0000-4000-8000-000000000002");

    @BeforeEach
    void setUp() {
        service = new AllowanceRequestService(
                requestRepository, approvalHistoryRepository, outboxEventRepository, familyAuthorizationClient);
    }

    @Test
    void approveLocksTransitionsWritesHistoryAndOutboxWithoutDisbursed() {
        AllowanceRequest pending = request("PENDING");
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(pending));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.approve(requestId, new ApproveAllowanceRequest(approverId, "OK"), actorProfileId);

        assertThat(response.status()).isEqualTo(AllowanceRequestStatus.APPROVED_PENDING_DISBURSEMENT.name());
        assertThat(pending.getDecidedAt()).isNotNull();

        verify(approvalHistoryRepository).save(any());
        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType())
                .isEqualTo(AllowanceRequestService.EVENT_ALLOWANCE_DISBURSEMENT_REQUESTED);
        assertThat(outboxCaptor.getValue().getStatus()).isEqualTo("PENDING");
    }

    @Test
    void rejectDoesNotCreateOutbox() {
        AllowanceRequest pending = request("PENDING");
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(pending));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.reject(requestId, new RejectAllowanceRequest(approverId, "No"));

        assertThat(response.status()).isEqualTo(AllowanceRequestStatus.REJECTED.name());
        verify(approvalHistoryRepository).save(any());
        verify(outboxEventRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void cannotDisburseFromPending() {
        AllowanceRequest pending = request("PENDING");
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.markDisbursed(requestId))
                .isInstanceOf(InvalidAllowanceStateException.class);
    }

    @Test
    void markDisbursedFromApprovedPending() {
        AllowanceRequest approved = request(AllowanceRequestStatus.APPROVED_PENDING_DISBURSEMENT.name());
        when(requestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(approved));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.markDisbursed(requestId);

        assertThat(response.status()).isEqualTo(AllowanceRequestStatus.DISBURSED.name());
    }

  private AllowanceRequest request(String status) {
        AllowanceRequest r = new AllowanceRequest();
        r.setId(requestId);
        r.setFamilyId(UUID.fromString("a1000000-0000-4000-8000-000000000001"));
        r.setRequesterMemberId(UUID.fromString("c1000000-0000-4000-8000-000000000003"));
        r.setAmount(150_000L);
        r.setReason("Books");
        r.setStatus(status);
        r.setCreatedAt(Instant.parse("2026-11-01T00:00:00Z"));
        r.setUpdatedAt(Instant.parse("2026-11-01T00:00:00Z"));
        return r;
    }
}