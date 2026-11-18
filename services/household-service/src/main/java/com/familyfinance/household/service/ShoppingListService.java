package com.familyfinance.household.service;

import com.familyfinance.household.dto.ShoppingListResponse;
import com.familyfinance.household.entity.ShoppingList;
import com.familyfinance.household.exception.ResourceNotFoundException;
import com.familyfinance.household.repository.ShoppingListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ShoppingListService {

    private final ShoppingListRepository repository;

    public ShoppingListService(ShoppingListRepository repository) {
        this.repository = repository;
    }

    public List<ShoppingListResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public ShoppingListResponse findById(UUID id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("ShoppingList", id));
    }

        private ShoppingListResponse toResponse(ShoppingList entity) {
        return new ShoppingListResponse(
                entity.getId(),
                entity.getFamilyId(),
                entity.getTitle(),
                entity.getStatus(),
                entity.getDueDate(),
                entity.getCreatedByMemberId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
