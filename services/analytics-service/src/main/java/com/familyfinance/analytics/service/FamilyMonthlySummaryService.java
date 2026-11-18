package com.familyfinance.analytics.service;

import com.familyfinance.analytics.dto.FamilyMonthlySummaryResponse;
import com.familyfinance.analytics.entity.FamilyMonthlySummary;
import com.familyfinance.analytics.exception.ResourceNotFoundException;
import com.familyfinance.analytics.repository.FamilyMonthlySummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FamilyMonthlySummaryService {

    private final FamilyMonthlySummaryRepository repository;

    public FamilyMonthlySummaryService(FamilyMonthlySummaryRepository repository) {
        this.repository = repository;
    }

    public List<FamilyMonthlySummaryResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public FamilyMonthlySummaryResponse findById(UUID id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("FamilyMonthlySummary", id));
    }

        private FamilyMonthlySummaryResponse toResponse(FamilyMonthlySummary entity) {
        return new FamilyMonthlySummaryResponse(
                entity.getId(),
                entity.getFamilyId(),
                entity.getSummaryMonth(),
                entity.getTotalIncome(),
                entity.getTotalExpense(),
                entity.getNetCashflow(),
                entity.getCurrencyCode(),
                entity.getGeneratedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
