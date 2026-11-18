package com.familyfinance.profile.support;

import com.familyfinance.profile.entity.UserProfile;
import com.familyfinance.profile.exception.ResourceNotFoundException;
import com.familyfinance.profile.exception.UnauthorizedException;
import com.familyfinance.profile.repository.UserProfileRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentUserResolver {

    public static final String HEADER_USER_ID = "X-User-Id";

    private final UserProfileRepository profileRepository;

    public CurrentUserResolver(UserProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public UserProfile requireCurrentProfile(HttpServletRequest request) {
        String header = request.getHeader(HEADER_USER_ID);
        if (header != null && !header.isBlank()) {
            try {
                UUID id = UUID.fromString(header.trim());
                return profileRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("UserProfile", id));
            } catch (IllegalArgumentException ex) {
                throw new UnauthorizedException("Invalid X-User-Id header");
            }
        }
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring("Bearer ".length()).trim();
            String sub = JwtSubjectExtractor.extractSubject(token)
                    .orElseThrow(() -> new UnauthorizedException("Invalid or missing JWT sub"));
            return profileRepository.findByKeycloakSub(sub)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "UserProfile not found for keycloak sub: " + sub));
        }
        throw new UnauthorizedException("Missing X-User-Id or Authorization");
    }
}