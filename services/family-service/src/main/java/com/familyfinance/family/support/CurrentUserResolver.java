package com.familyfinance.family.support;

import com.familyfinance.family.exception.UnauthorizedException;
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

    private final boolean securityEnabled;

    public CurrentUserResolver(@Value("${familyfinance.security.enabled:false}") boolean securityEnabled) {
        this.securityEnabled = securityEnabled;
    }

    public UUID requireUserProfileId(HttpServletRequest request) {
        Optional<UUID> fromJwt = resolveFromSecurityContext();
        if (fromJwt.isPresent()) {
            return fromJwt.get();
        }

        // When JWT enforcement is on, never trust client-supplied identity headers.
        if (securityEnabled) {
            throw new UnauthorizedException("Authenticated JWT required");
        }

        String header = request.getHeader(HEADER_USER_ID);
        if (header != null && !header.isBlank()) {
            try {
                return UUID.fromString(header.trim());
            } catch (IllegalArgumentException ex) {
                throw new UnauthorizedException("Invalid X-User-Id header");
            }
        }
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring("Bearer ".length()).trim();
            return JwtSubjectExtractor.extractUserProfileId(token)
                    .orElseThrow(() -> new UnauthorizedException(
                            "Missing X-User-Id; JWT sub is not a valid user profile UUID"));
        }
        throw new UnauthorizedException("Missing X-User-Id");
    }

    private Optional<UUID> resolveFromSecurityContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String sub = jwt.getSubject();
            if (sub == null || sub.isBlank()) {
                return Optional.empty();
            }
            // Prefer explicit profile claim when present; otherwise require UUID sub.
            Object profileClaim = jwt.getClaim("user_profile_id");
            if (profileClaim != null) {
                try {
                    return Optional.of(UUID.fromString(profileClaim.toString()));
                } catch (IllegalArgumentException ignored) {
                    // fall through
                }
            }
            try {
                return Optional.of(UUID.fromString(sub));
            } catch (IllegalArgumentException ex) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
