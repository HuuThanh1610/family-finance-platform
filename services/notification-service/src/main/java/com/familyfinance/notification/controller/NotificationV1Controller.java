package com.familyfinance.notification.controller;

import com.familyfinance.notification.dto.NotificationPreferenceResponse;
import com.familyfinance.notification.dto.NotificationResponse;
import com.familyfinance.notification.dto.NotificationTemplateResponse;
import com.familyfinance.notification.dto.UpsertPreferenceRequest;
import com.familyfinance.notification.service.NotificationApplicationService;
import com.familyfinance.notification.service.NotificationTemplateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationV1Controller {

    private final NotificationApplicationService service;
    private final NotificationTemplateService templateService;

    public NotificationV1Controller(
            NotificationApplicationService service,
            NotificationTemplateService templateService) {
        this.service = service;
        this.templateService = templateService;
    }

    @GetMapping
    public List<NotificationResponse> list(
            @RequestParam UUID recipientUserId,
            @RequestParam(required = false) UUID familyId) {
        return service.listForUser(recipientUserId, familyId);
    }

    @GetMapping("/{id}")
    public NotificationResponse get(@PathVariable UUID id) {
        return service.getById(id);
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markReadPatch(@PathVariable UUID id) {
        return service.markRead(id);
    }

    @PostMapping("/{id}/read")
    public NotificationResponse markReadPost(@PathVariable UUID id) {
        return service.markRead(id);
    }

    @PostMapping("/mark-all-read")
    public int markAllRead(@RequestParam UUID recipientUserId) {
        return service.markAllRead(recipientUserId);
    }

    @GetMapping("/preferences")
    public List<NotificationPreferenceResponse> preferences(@RequestParam UUID userProfileId) {
        return service.preferences(userProfileId);
    }

    @PutMapping("/preferences")
    public NotificationPreferenceResponse upsertPreference(@Valid @RequestBody UpsertPreferenceRequest request) {
        return service.upsertPreference(request);
    }

    @GetMapping("/templates")
    public List<NotificationTemplateResponse> templates() {
        return templateService.listActive();
    }
}