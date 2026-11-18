package com.familyfinance.household.controller;

import com.familyfinance.household.dto.AdjustInventoryRequest;
import com.familyfinance.household.dto.InventoryItemResponse;
import com.familyfinance.household.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/household")
public class InventoryV1Controller {

    private final InventoryService inventoryService;

    public InventoryV1Controller(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/inventory")
    public List<InventoryItemResponse> list(@RequestParam UUID familyId) {
        return inventoryService.listByFamily(familyId);
    }

    @PostMapping("/inventory/{inventoryItemId}/movements")
    public InventoryItemResponse adjust(
            @PathVariable UUID inventoryItemId,
            @Valid @RequestBody AdjustInventoryRequest request
    ) {
        return inventoryService.adjust(inventoryItemId, request);
    }
}
