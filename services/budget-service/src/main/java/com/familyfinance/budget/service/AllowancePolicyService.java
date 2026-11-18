package com.familyfinance.budget.service;

import com.familyfinance.budget.dto.AllowancePolicyResponse;
import com.familyfinance.budget.dto.CreateAllowancePolicyRequest;
import com.familyfinance.budget.entity.AllowancePolicy;
import com.familyfinance.budget.exception.BusinessException;
import com.familyfinance.budget.exception.ResourceNotFoundException;
import com.familyfinance.budget.repository.AllowancePolicyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AllowancePolicyService {

    private static final Set<String> ALLOWED_CADENCE = Set.of("WEEKLY", "BIWEEKLY", "MONTHLY", "ONE_TIME");

    private final AllowancePolicyRepository repository;

    public AllowancePolicyService(AllowancePolicyRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AllowancePolicyResponse> findByFamily(UUID familyId) {
        if (familyId == null) {
            return repository.findAll().stream().map(this::toResponse).toList();
        }
        return repository.findByFamilyIdOrderByCreatedAtDesc(familyId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AllowancePolicyResponse findById(UUID id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("AllowancePolicy", id));
    }

    @Transactional
    public AllowancePolicyResponse create(CreateAllowancePolicyRequest request) {
        if (!ALLOWED_CADENCE.contains(request.cadence())) {
            throw new BusinessException("Invalid cadence: " + request.cadence(), HttpStatus.BAD_REQUEST);
        }
        Instant now = Instant.now();
        AllowancePolicy policy = new AllowancePolicy();
        policy.setFamilyId(request.familyId());
        policy.setMemberId(request.memberId());
        policy.setAmount(request.amount());
        policy.setCadence(request.cadence());
        policy.setDayOfWeek(request.dayOfWeek());
        policy.setDayOfMonth(request.dayOfMonth());
        policy.setEffectiveFrom(request.effectiveFrom());
        policy.setEffectiveTo(request.effectiveTo());
        policy.setStatus("ACTIVE");
        policy.setCreatedAt(now);
        policy.setUpdatedAt(now);
        policy = repository.save(policy);
        return toResponse(policy);
    }

    private AllowancePolicyResponse toResponse(AllowancePolicy entity) {
        return new AllowancePolicyResponse(
                entity.getId(),
                entity.getFamilyId(),
                entity.getMemberId(),
                entity.getAmount(),
                entity.getCadence(),
                entity.getDayOfWeek(),
                entity.getDayOfMonth(),
                entity.getEffectiveFrom(),
                entity.getEffectiveTo(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}