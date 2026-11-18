package com.familyfinance.wallet.security;

import com.familyfinance.wallet.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FamilyAuthorizationClientTest {

    private final UUID familyId = UUID.fromString("a1000000-0000-4000-8000-000000000001");
    private final UUID userId = UUID.fromString("b1000000-0000-4000-8000-000000000001");

    @Test
    void denyWhenNotMember() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                FamilyAuthorizationClient.assertActiveMember(Map.of("member", false, "active", false)));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void failClosedWhenFamilyServiceUnavailable() {
        FamilyAuthorizationClient client = new FamilyAuthorizationClient(
                RestClient.builder(), "http://127.0.0.1:1", true, "k");

        BusinessException ex = assertThrows(BusinessException.class, () -> client.requireMember(familyId, userId));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
    }

    @Test
    void skipWhenDisabled() {
        FamilyAuthorizationClient client = new FamilyAuthorizationClient(
                RestClient.builder(), "http://127.0.0.1:1", false, "");
        client.requireMember(familyId, userId);
    }
}
