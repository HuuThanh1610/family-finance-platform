package com.familyfinance.wallet.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Api-Key";

    private final String expectedKey;
    private final boolean securityEnabled;

    public InternalApiKeyFilter(
            @Value("${familyfinance.internal.api-key:}") String expectedKey,
            @Value("${familyfinance.security.enabled:false}") boolean securityEnabled
    ) {
        this.expectedKey = expectedKey == null ? "" : expectedKey;
        this.securityEnabled = securityEnabled;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/internal/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {
        if (!securityEnabled) {
            filterChain.doFilter(request, response);
            return;
        }
        if (expectedKey.isBlank()) {
            response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), "Internal API key not configured");
            return;
        }
        String provided = request.getHeader(HEADER);
        if (provided == null || !expectedKey.equals(provided)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid internal API key");
            return;
        }
        var auth = new UsernamePasswordAuthenticationToken(
                "internal-service",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        filterChain.doFilter(request, response);
    }
}
