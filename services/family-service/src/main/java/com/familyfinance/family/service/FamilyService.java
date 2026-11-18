package com.familyfinance.family.service;

import com.familyfinance.family.dto.FamilyResponse;
import com.familyfinance.family.entity.Family;
import com.familyfinance.family.exception.ResourceNotFoundException;
import com.familyfinance.family.repository.FamilyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FamilyService {

    private final FamilyRepository repository;

    public FamilyService(FamilyRepository repository) {
        this.repository = repository;
    }

    public List<FamilyResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public FamilyResponse findById(UUID id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Family", id));
    }

        private FamilyResponse toResponse(Family entity) {
        return new FamilyResponse(
                entity.getId(),
                entity.getName(),
                entity.getSlug(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
