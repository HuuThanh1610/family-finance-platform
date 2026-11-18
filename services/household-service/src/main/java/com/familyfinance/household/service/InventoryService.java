package com.familyfinance.household.service;

import com.familyfinance.household.dto.AdjustInventoryRequest;
import com.familyfinance.household.dto.InventoryItemResponse;
import com.familyfinance.household.entity.InventoryItem;
import com.familyfinance.household.entity.InventoryMovement;
import com.familyfinance.household.entity.OutboxEvent;
import com.familyfinance.household.exception.BusinessException;
import com.familyfinance.household.exception.ResourceNotFoundException;
import com.familyfinance.household.repository.InventoryItemRepository;
import com.familyfinance.household.repository.InventoryMovementRepository;
import com.familyfinance.household.repository.OutboxEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class InventoryService {

    public static final String EVENT_INVENTORY_LOW = "InventoryLow";
    private static final Set<String> MOVEMENT_TYPES = Set.of("INBOUND", "OUTBOUND", "ADJUSTMENT", "WASTE", "TRANSFER");

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryMovementRepository movementRepository;
    private final OutboxEventRepository outboxEventRepository;

    public InventoryService(
            InventoryItemRepository inventoryItemRepository,
            InventoryMovementRepository movementRepository,
            OutboxEventRepository outboxEventRepository
    ) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.movementRepository = movementRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> listByFamily(UUID familyId) {
        return inventoryItemRepository.findByFamilyId(familyId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public InventoryItemResponse adjust(UUID inventoryItemId, AdjustInventoryRequest request) {
        if (!MOVEMENT_TYPES.contains(request.movementType())) {
            throw new BusinessException("Invalid movement type", HttpStatus.BAD_REQUEST);
        }
        if (request.quantityDelta().compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("quantityDelta must be non-zero", HttpStatus.BAD_REQUEST);
        }

        InventoryItem item = inventoryItemRepository.findWithLockById(inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", inventoryItemId));

        BigDecimal delta = request.quantityDelta();
        if ("OUTBOUND".equals(request.movementType()) || "WASTE".equals(request.movementType())) {
            delta = request.quantityDelta().abs().negate();
        } else if ("INBOUND".equals(request.movementType())) {
            delta = request.quantityDelta().abs();
        }

        BigDecimal next = item.getQuantityOnHand().add(delta);
        if (next.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Insufficient inventory quantity", HttpStatus.CONFLICT);
        }

        Instant now = Instant.now();
        InventoryMovement movement = new InventoryMovement();
        movement.setInventoryItem(item);
        movement.setMovementType(request.movementType());
        movement.setQuantityDelta(delta);
        movement.setReason(request.reason());
        movement.setReferenceId(request.referenceId());
        movement.setMovedAt(now);
        movement.setCreatedAt(now);
        movementRepository.save(movement);

        item.setQuantityOnHand(next);
        item.setUpdatedAt(now);

        boolean below = item.getReorderLevel() != null && next.compareTo(item.getReorderLevel()) <= 0;
        if (below && !item.isLowStockAlerted()) {
            OutboxEvent outbox = new OutboxEvent();
            outbox.setEventType(EVENT_INVENTORY_LOW);
            outbox.setAggregateType("inventory_item");
            outbox.setAggregateId(item.getId());
            outbox.setPayload(Map.of(
                    "inventoryItemId", item.getId().toString(),
                    "familyId", item.getFamilyId().toString(),
                    "quantityOnHand", next,
                    "reorderLevel", item.getReorderLevel()
            ));
            outbox.setCreatedAt(now);
            outbox.setStatus("PENDING");
            outboxEventRepository.save(outbox);
            item.setLowStockAlerted(true);
        } else if (!below && item.isLowStockAlerted()) {
            item.setLowStockAlerted(false);
        }

        return toResponse(inventoryItemRepository.save(item));
    }

    private InventoryItemResponse toResponse(InventoryItem item) {
        boolean low = item.getReorderLevel() != null
                && item.getQuantityOnHand().compareTo(item.getReorderLevel()) <= 0;
        return new InventoryItemResponse(
                item.getId(),
                item.getFamilyId(),
                item.getCatalogItem() != null ? item.getCatalogItem().getId() : null,
                item.getLocationCode(),
                item.getQuantityOnHand(),
                item.getReorderLevel(),
                low,
                item.getUpdatedAt()
        );
    }
}
