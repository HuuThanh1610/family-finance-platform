package com.familyfinance.notification.controller;

import com.familyfinance.notification.dto.NotificationResponse;
import com.familyfinance.notification.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public List<NotificationResponse> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public NotificationResponse get(@PathVariable UUID id) {
        return service.findById(id);
    }
}
