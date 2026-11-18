package com.familyfinance.wallet.security;

import com.familyfinance.wallet.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.UUID;

/**
 * Fail-closed family membership check against Family Service internal API.
 */
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

    public void requireMember(UUID familyId, UUID userProfileId) {
        if (!enabled) {
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restClient.get()
                    .uri("/internal/families/{familyId}/members/{userProfileId}/authorization",
                            familyId, userProfileId)
                    .headers(headers -> {
                        if (!internalApiKey.isBlank()) {
                            headers.add("X-Internal-Api-Key", internalApiKey);
                        }
                    })
                    .retrieve()
                    .body(Map.class);
            assertActiveMember(body);
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.warn("Family authorization unavailable familyId={} user={}: {}", familyId, userProfileId, ex.toString());
            throw new BusinessException("Family authorization unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    static void assertActiveMember(Map<String, Object> body) {
        boolean member = body != null && Boolean.TRUE.equals(body.get("member"));
        boolean active = body != null && Boolean.TRUE.equals(body.get("active"));
        if (!member || !active) {
            throw new BusinessException("Not an active member of this family", HttpStatus.FORBIDDEN);
        }
    }
}
