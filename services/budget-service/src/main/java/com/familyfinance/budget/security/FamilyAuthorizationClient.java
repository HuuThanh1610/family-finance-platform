package com.familyfinance.budget.security;

import com.familyfinance.budget.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class FamilyAuthorizationClient {

    private static final Logger log = LoggerFactory.getLogger(FamilyAuthorizationClient.class);

    private final RestClient restClient;
    private final boolean enabled;
    private final String internalApiKey;

    public FamilyAuthorizationClient(
            RestClient.Builder builder,
            @Value("${familyfinance.family-service.base-url:http://localhost:8082}") String baseUrl,
            @Value("${familyfinance.family-auth.enabled:false}") boolean enabled,
            @Value("${familyfinance.internal.api-key:}") String internalApiKey
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.enabled = enabled;
        this.internalApiKey = internalApiKey == null ? "" : internalApiKey;
    }

    public void requirePermission(UUID familyId, UUID userProfileId, String permissionKey) {
        Map<String, Object> body = fetchAuthorization(familyId, userProfileId);
        boolean member = body != null && Boolean.TRUE.equals(body.get("member"));
        boolean active = body != null && Boolean.TRUE.equals(body.get("active"));
        if (!member || !active) {
            throw new BusinessException("Not an active member of this family", HttpStatus.FORBIDDEN);
        }
        Object raw = body.get("permissions");
        boolean allowed = raw instanceof Collection<?> permissions
                && permissions.stream().map(String::valueOf)
                .anyMatch(p -> "*".equals(p) || permissionKey.equals(p));
        if (!allowed) {
            throw new BusinessException("Missing permission: " + permissionKey, HttpStatus.FORBIDDEN);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchAuthorization(UUID familyId, UUID userProfileId) {
        if (!enabled) {
            return Map.of("member", true, "active", true, "permissions", List.of("*"));
        }
        try {
            return restClient.get()
                    .uri("/internal/families/{familyId}/members/{userProfileId}/authorization",
                            familyId, userProfileId)
                    .headers(headers -> {
                        if (!internalApiKey.isBlank()) {
                            headers.add("X-Internal-Api-Key", internalApiKey);
                        }
                    })
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException ex) {
            log.warn("Family authorization unavailable familyId={} user={}: {}", familyId, userProfileId, ex.toString());
            throw new BusinessException("Family authorization unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
