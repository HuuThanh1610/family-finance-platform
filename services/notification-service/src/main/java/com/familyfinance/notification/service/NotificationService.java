package com.familyfinance.notification.service;

import com.familyfinance.notification.dto.NotificationResponse;
import com.familyfinance.notification.entity.Notification;
import com.familyfinance.notification.exception.ResourceNotFoundException;
import com.familyfinance.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public List<NotificationResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public NotificationResponse findById(UUID id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
    }

        private NotificationResponse toResponse(Notification entity) {
        return new NotificationResponse(
                entity.getId(),
                entity.getRecipientUserId(),
                entity.getFamilyId(),
                entity.getTemplateKey(),
                entity.getChannel(),
                entity.getTitle(),
                entity.getBody(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
