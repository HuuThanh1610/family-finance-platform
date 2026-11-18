package com.familyfinance.profile.service;

import com.familyfinance.profile.dto.UpdateProfileRequest;
import com.familyfinance.profile.dto.UserProfileResponse;
import com.familyfinance.profile.entity.UserProfile;
import com.familyfinance.profile.exception.ResourceNotFoundException;
import com.familyfinance.profile.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserProfileRepository repository;
    private final UserProfileMapper mapper;

    public UserProfileService(UserProfileRepository repository, UserProfileMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<UserProfileResponse> findAll() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    public UserProfileResponse findById(UUID id) {
        return repository.findById(id).map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", id));
    }

    public UserProfileResponse toResponse(UserProfile profile) {
        return mapper.toResponse(profile);
    }

    @Transactional
    public UserProfileResponse updateProfile(UserProfile profile, UpdateProfileRequest request) {
        profile.setDisplayName(request.displayName().trim());
        profile.setPhone(blankToNull(request.phone()));
        profile.setAvatarUrl(blankToNull(request.avatarUrl()));
        profile.setLocale(request.locale().trim());
        profile.setTimezone(request.timezone().trim());
        return mapper.toResponse(repository.save(profile));
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}