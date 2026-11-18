package com.familyfinance.profile.support;

import com.familyfinance.profile.entity.UserProfile;
import com.familyfinance.profile.exception.ResourceNotFoundException;
import com.familyfinance.profile.exception.UnauthorizedException;
import com.familyfinance.profile.repository.UserProfileRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class CurrentUserResolver {

    public static final String HEADER_USER_ID = "X-User-Id";

    private final UserProfileRepository profileRepository;
    private final boolean securityEnabled;

    public CurrentUserResolver(
            UserProfileRepository profileRepository,
            @Value("${familyfinance.security.enabled:false}") boolean securityEnabled
    ) {
        this.profileRepository = profileRepository;
        this.securityEnabled = securityEnabled;
    }

    public UserProfile requireCurrentProfile(HttpServletRequest request) {
        Optional<UserProfile> fromJwt = resolveFromSecurityContext();
        if (fromJwt.isPresent()) {
            return fromJwt.get();
        }

        if (securityEnabled) {
            throw new UnauthorizedException("Authenticated JWT required");
        }

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

    private Optional<UserProfile> resolveFromSecurityContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            return Optional.empty();
        }
        Jwt jwt = jwtAuth.getToken();
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            return Optional.empty();
        }
        return profileRepository.findByKeycloakSub(sub);
    }
}
