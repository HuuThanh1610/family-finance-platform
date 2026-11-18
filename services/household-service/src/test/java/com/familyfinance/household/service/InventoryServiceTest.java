package com.familyfinance.household.service;

import com.familyfinance.household.dto.AdjustInventoryRequest;
import com.familyfinance.household.entity.InventoryItem;
import com.familyfinance.household.repository.InventoryItemRepository;
import com.familyfinance.household.repository.InventoryMovementRepository;
import com.familyfinance.household.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock InventoryItemRepository inventoryItemRepository;
    @Mock InventoryMovementRepository movementRepository;
    @Mock OutboxEventRepository outboxEventRepository;
    @InjectMocks InventoryService service;

    @Test
    void outboundBelowReorderEmitsInventoryLowOnce() {
        UUID id = UUID.randomUUID();
        InventoryItem item = new InventoryItem();
        item.setId(id);
        item.setFamilyId(UUID.randomUUID());
        item.setQuantityOnHand(new BigDecimal("5"));
        item.setReorderLevel(new BigDecimal("4"));
        item.setLowStockAlerted(false);
        item.setCreatedAt(Instant.now());
        item.setUpdatedAt(Instant.now());

        when(inventoryItemRepository.findWithLockById(id)).thenReturn(Optional.of(item));
        when(inventoryItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(movementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.adjust(id, new AdjustInventoryRequest(
                "OUTBOUND", new BigDecimal("2"), "cook", null));

        assertTrue(response.lowStock());
        verify(outboxEventRepository, times(1)).save(any());
    }
}
