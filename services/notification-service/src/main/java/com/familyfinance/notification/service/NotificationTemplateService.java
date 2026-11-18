package com.familyfinance.notification.service;

import com.familyfinance.notification.dto.NotificationTemplateResponse;
import com.familyfinance.notification.repository.NotificationTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class NotificationTemplateService {

    private final NotificationTemplateRepository repository;

    public NotificationTemplateService(NotificationTemplateRepository repository) {
        this.repository = repository;
    }

    public List<NotificationTemplateResponse> listActive() {
        return repository.findByActiveTrueOrderByTemplateKeyAsc().stream()
                .map(t -> new NotificationTemplateResponse(
                        t.getId(),
                        t.getTemplateKey(),
                        t.getChannel(),
                        t.getLocale(),
                        t.getSubject(),
                        t.getBodyTemplate(),
                        t.isActive()
                ))
                .toList();
    }
}