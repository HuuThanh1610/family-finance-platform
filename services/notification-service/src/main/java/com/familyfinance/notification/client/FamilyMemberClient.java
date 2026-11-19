package com.familyfinance.notification.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.UUID;

/**
 * Resolves active family members via Family Service internal API.
 * {@code userProfileId} is the notification recipient identity (not Keycloak-only opaque ids).
 */
@Component
public class FamilyMemberClient {

    private static final Logger log = LoggerFactory.getLogger(FamilyMemberClient.class);

    private final RestClient restClient;
    private final String internalApiKey;

    public FamilyMemberClient(
            RestClient.Builder builder,
            @Value("${familyfinance.family-service.base-url:http://localhost:8082}") String baseUrl,
            @Value("${familyfinance.internal.api-key:}") String internalApiKey
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.internalApiKey = internalApiKey == null ? "" : internalApiKey;
    }

    public List<InternalMember> listActiveMembers(UUID familyId) {
        try {
            List<InternalMember> body = restClient.get()
                    .uri("/internal/families/{familyId}/members", familyId)
                    .headers(headers -> {
                        if (!internalApiKey.isBlank()) {
                            headers.add("X-Internal-Api-Key", internalApiKey);
                        }
                    })
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return body == null ? List.of() : body;
        } catch (RestClientException ex) {
            log.warn("Family member lookup failed familyId={}: {}", familyId, ex.toString());
            throw new FamilyLookupException("Family membership lookup unavailable", ex);
        }
    }

    public record InternalMember(UUID userProfileId, String roleCode, String status) {
    }

    public static class FamilyLookupException extends RuntimeException {
        public FamilyLookupException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
