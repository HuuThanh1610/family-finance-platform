package com.familyfinance.profile.controller;

import com.familyfinance.profile.dto.*;
import com.familyfinance.profile.service.ProfileProvisioningService;
import com.familyfinance.profile.service.UserConsentService;
import com.familyfinance.profile.service.UserPreferenceService;
import com.familyfinance.profile.service.UserProfileService;
import com.familyfinance.profile.support.CurrentUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileV1Controller {

    private final ProfileProvisioningService provisioningService;
    private final UserProfileService profileService;
    private final UserPreferenceService preferenceService;
    private final UserConsentService consentService;
    private final CurrentUserResolver currentUserResolver;

    public ProfileV1Controller(
            ProfileProvisioningService provisioningService,
            UserProfileService profileService,
            UserPreferenceService preferenceService,
            UserConsentService consentService,
            CurrentUserResolver currentUserResolver) {
        this.provisioningService = provisioningService;
        this.profileService = profileService;
        this.preferenceService = preferenceService;
        this.consentService = consentService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/provision")
    @ResponseStatus(HttpStatus.OK)
    public UserProfileResponse provision(@Valid @RequestBody ProvisionProfileRequest request) {
        return provisioningService.provision(request);
    }

    @GetMapping("/me")
    public UserProfileResponse getMe(HttpServletRequest request) {
        return profileService.toResponse(currentUserResolver.requireCurrentProfile(request));
    }

    @PutMapping("/me")
    public UserProfileResponse updateMe(
            HttpServletRequest request,
            @Valid @RequestBody UpdateProfileRequest body) {
        return profileService.updateProfile(currentUserResolver.requireCurrentProfile(request), body);
    }

    @GetMapping("/me/preferences")
    public List<UserPreferenceResponse> getPreferences(HttpServletRequest request) {
        var profile = currentUserResolver.requireCurrentProfile(request);
        return preferenceService.listForProfile(profile.getId());
    }

    @PutMapping("/me/preferences")
    public List<UserPreferenceResponse> updatePreferences(
            HttpServletRequest request,
            @Valid @RequestBody UpsertPreferencesRequest body) {
        var profile = currentUserResolver.requireCurrentProfile(request);
        return preferenceService.upsertForProfile(profile, body.preferences());
    }

    @PostMapping("/me/consents")
    @ResponseStatus(HttpStatus.CREATED)
    public UserConsentResponse recordConsent(
            HttpServletRequest request,
            @Valid @RequestBody RecordConsentRequest body) {
        var profile = currentUserResolver.requireCurrentProfile(request);
        return consentService.record(profile, body);
    }
}