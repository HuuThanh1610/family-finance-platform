package com.familyfinance.expense.support;

import com.familyfinance.expense.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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

    private final boolean securityEnabled;

    public CurrentUserResolver(@Value("${familyfinance.security.enabled:false}") boolean securityEnabled) {
        this.securityEnabled = securityEnabled;
    }

    public UUID requireUserProfileId(HttpServletRequest request) {
        Optional<UUID> fromJwt = resolveFromSecurityContext();
        if (fromJwt.isPresent()) {
            return fromJwt.get();
        }
        if (securityEnabled) {
            throw new BusinessException("Authenticated JWT required", HttpStatus.UNAUTHORIZED);
        }
        String header = request.getHeader(HEADER_USER_ID);
        if (header != null && !header.isBlank()) {
            try {
                return UUID.fromString(header.trim());
            } catch (IllegalArgumentException ex) {
                throw new BusinessException("Invalid X-User-Id header", HttpStatus.UNAUTHORIZED);
            }
        }
        throw new BusinessException("Missing authenticated user", HttpStatus.UNAUTHORIZED);
    }

    private Optional<UUID> resolveFromSecurityContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            Object profileClaim = jwt.getClaim("user_profile_id");
            if (profileClaim != null) {
                try {
                    return Optional.of(UUID.fromString(profileClaim.toString()));
                } catch (IllegalArgumentException ignored) {
                    // fall through
                }
            }
            String sub = jwt.getSubject();
            if (sub != null && !sub.isBlank()) {
                try {
                    return Optional.of(UUID.fromString(sub));
                } catch (IllegalArgumentException ex) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }
}
