package com.familyfinance.household.service;

import com.familyfinance.household.dto.AdjustInventoryRequest;
import com.familyfinance.household.entity.InventoryItem;
import com.familyfinance.household.entity.OutboxEvent;
import com.familyfinance.household.repository.InventoryItemRepository;
import com.familyfinance.household.repository.InventoryMovementRepository;
import com.familyfinance.household.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
        InventoryItem item = item(id, "5", "4", false);

        when(inventoryItemRepository.findWithLockById(id)).thenReturn(Optional.of(item));
        when(inventoryItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(movementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.adjust(id, new AdjustInventoryRequest(
                "OUTBOUND", new BigDecimal("2"), "cook", null));

        assertTrue(response.lowStock());
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(1)).save(captor.capture());
        OutboxEvent outbox = captor.getValue();
        assertThat(outbox.getEventType()).isEqualTo(InventoryService.EVENT_INVENTORY_LOW);
        assertThat(outbox.getStatus()).isEqualTo("PENDING");
        assertThat(outbox.getPayload()).containsKeys("inventoryItemId", "familyId", "quantityOnHand", "reorderLevel");
        assertTrue(item.isLowStockAlerted());
    }

    @Test
    void aboveThresholdDoesNotEmitEvent() {
        UUID id = UUID.randomUUID();
        InventoryItem item = item(id, "10", "4", false);

        when(inventoryItemRepository.findWithLockById(id)).thenReturn(Optional.of(item));
        when(inventoryItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(movementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.adjust(id, new AdjustInventoryRequest(
                "OUTBOUND", new BigDecimal("1"), "use", null));

        assertFalse(response.lowStock());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void repeatedLowStockDoesNotDuplicateAlertWhenAlreadyAlerted() {
        UUID id = UUID.randomUUID();
        InventoryItem item = item(id, "3", "4", true);

        when(inventoryItemRepository.findWithLockById(id)).thenReturn(Optional.of(item));
        when(inventoryItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(movementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.adjust(id, new AdjustInventoryRequest(
                "OUTBOUND", new BigDecimal("1"), "again", null));

        verify(outboxEventRepository, never()).save(any());
        assertTrue(item.isLowStockAlerted());
    }

    private InventoryItem item(UUID id, String qty, String reorder, boolean alerted) {
        InventoryItem item = new InventoryItem();
        item.setId(id);
        item.setFamilyId(UUID.randomUUID());
        item.setQuantityOnHand(new BigDecimal(qty));
        item.setReorderLevel(new BigDecimal(reorder));
        item.setLowStockAlerted(alerted);
        item.setCreatedAt(Instant.now());
        item.setUpdatedAt(Instant.now());
        return item;
    }
}
