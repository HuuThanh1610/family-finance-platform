package com.familyfinance.notification.service;

import com.familyfinance.notification.entity.Notification;
import com.familyfinance.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository repository;

    NotificationService service;

  UUID notificationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository);
    }

    @Test
    void markReadSetsStatusAndReadFlag() {
        Notification entity = baseNotification("DELIVERED");
        when(repository.findById(notificationId)).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.markRead(notificationId);

        assertThat(response.status()).isEqualTo("READ");
        assertThat(response.read()).isTrue();
        verify(repository).save(entity);
    }

    private Notification baseNotification(String status) {
        Notification n = new Notification();
        n.setId(notificationId);
        n.setRecipientUserId(UUID.randomUUID());
        n.setChannel("IN_APP");
        n.setTitle("t");
        n.setBody("b");
        n.setPayload(Map.of());
        n.setStatus(status);
        n.setCreatedAt(Instant.now());
        n.setUpdatedAt(Instant.now());
        return n;
    }
}