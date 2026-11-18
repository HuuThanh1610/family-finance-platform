package com.familyfinance.notification.service;

import com.familyfinance.notification.entity.Notification;
import com.familyfinance.notification.repository.InboxEventRepository;
import com.familyfinance.notification.repository.NotificationPreferenceRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationApplicationServiceMarkReadTest {

    @Mock NotificationRepository notificationRepository;
    @Mock NotificationPreferenceRepository preferenceRepository;
    @Mock InboxEventRepository inboxEventRepository;

    NotificationApplicationService service;
    UUID notificationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new NotificationApplicationService(notificationRepository, preferenceRepository, inboxEventRepository);
    }

    @Test
    void markReadSetsReadFlag() {
        Notification entity = new Notification();
        entity.setId(notificationId);
        entity.setRecipientUserId(UUID.randomUUID());
        entity.setChannel("IN_APP");
        entity.setTitle("t");
        entity.setBody("b");
        entity.setPayload(Map.of());
        entity.setStatus("DELIVERED");
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(entity));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.markRead(notificationId);

        assertThat(response.status()).isEqualTo("READ");
        assertThat(response.read()).isTrue();
    }
}