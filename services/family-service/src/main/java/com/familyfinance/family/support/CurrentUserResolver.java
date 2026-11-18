package com.familyfinance.family.support;

import com.familyfinance.family.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentUserResolver {

    public static final String HEADER_USER_ID = "X-User-Id";

    public UUID requireUserProfileId(HttpServletRequest request) {
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
}