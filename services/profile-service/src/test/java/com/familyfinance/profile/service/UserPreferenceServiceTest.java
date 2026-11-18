package com.familyfinance.profile.service;

import com.familyfinance.profile.dto.PreferenceItemRequest;
import com.familyfinance.profile.entity.UserPreference;
import com.familyfinance.profile.entity.UserProfile;
import com.familyfinance.profile.repository.UserPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceTest {

    @Mock UserPreferenceRepository repository;

    UserPreferenceService service;

    UUID profileId = UUID.fromString("b1000000-0000-4000-8000-000000000001");

    @BeforeEach
    void setUp() {
        service = new UserPreferenceService(repository);
    }

    @Test
    void upsertsExistingPreferenceValue() {
        UserProfile profile = new UserProfile();
        profile.setId(profileId);

        UserPreference existing = new UserPreference();
        existing.setId(UUID.randomUUID());
        existing.setUserProfile(profile);
        existing.setPreferenceKey("ui.theme");
        existing.setPreferenceValue(Map.of("mode", "light"));

        when(repository.findByUserProfileIdAndPreferenceKey(profileId, "ui.theme"))
                .thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        var result = service.upsertForProfile(profile, List.of(
                new PreferenceItemRequest("ui.theme", Map.of("mode", "dark"))));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().preferenceValue()).containsEntry("mode", "dark");

        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPreferenceValue()).containsEntry("mode", "dark");
    }

    @Test
    void createsPreferenceWhenMissing() {
        UserProfile profile = new UserProfile();
        profile.setId(profileId);

        when(repository.findByUserProfileIdAndPreferenceKey(eq(profileId), eq("currency.display")))
                .thenReturn(Optional.empty());
        when(repository.save(any(UserPreference.class))).thenAnswer(invocation -> {
            UserPreference p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        var result = service.upsertForProfile(profile, List.of(
                new PreferenceItemRequest("currency.display", Map.of("code", "VND"))));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().preferenceKey()).isEqualTo("currency.display");
    }
}