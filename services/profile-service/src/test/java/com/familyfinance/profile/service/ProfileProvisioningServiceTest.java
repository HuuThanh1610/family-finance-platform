package com.familyfinance.profile.service;

import com.familyfinance.profile.dto.ProvisionProfileRequest;
import com.familyfinance.profile.entity.UserProfile;
import com.familyfinance.profile.exception.ConflictException;
import com.familyfinance.profile.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileProvisioningServiceTest {

    @Mock UserProfileRepository repository;
    UserProfileMapper mapper = new UserProfileMapper();

    ProfileProvisioningService service;

    @BeforeEach
    void setUp() {
        service = new ProfileProvisioningService(repository, mapper);
    }

    @Test
    void createsProfileWhenKeycloakSubNotFound() {
        when(repository.findByKeycloakSub("kc-new")).thenReturn(Optional.empty());
        when(repository.findByEmail("new@demo.local")).thenReturn(Optional.empty());
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> {
            UserProfile p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        var response = service.provision(new ProvisionProfileRequest(
                "kc-new", "new@demo.local", "New User"));

        assertThat(response.keycloakSub()).isEqualTo("kc-new");
        assertThat(response.email()).isEqualTo("new@demo.local");
        assertThat(response.displayName()).isEqualTo("New User");

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getKeycloakSub()).isEqualTo("kc-new");
    }

    @Test
    void updatesExistingProfileByKeycloakSub() {
        UUID id = UUID.randomUUID();
        UserProfile existing = new UserProfile();
        existing.setId(id);
        existing.setKeycloakSub("kc-existing");
        existing.setEmail("old@demo.local");
        existing.setDisplayName("Old Name");

        when(repository.findByKeycloakSub("kc-existing")).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        var response = service.provision(new ProvisionProfileRequest(
                "kc-existing", "old@demo.local", "Updated Name"));

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.displayName()).isEqualTo("Updated Name");
        verify(repository, never()).findByEmail(any());
    }

    @Test
    void rejectsEmailConflictOnCreate() {
        when(repository.findByKeycloakSub("kc-a")).thenReturn(Optional.empty());
        when(repository.findByEmail("taken@demo.local")).thenReturn(Optional.of(new UserProfile()));

        assertThatThrownBy(() -> service.provision(new ProvisionProfileRequest(
                "kc-a", "taken@demo.local", "A")))
                .isInstanceOf(ConflictException.class);
    }
}