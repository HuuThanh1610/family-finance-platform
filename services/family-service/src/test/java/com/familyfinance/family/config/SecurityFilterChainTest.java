package com.familyfinance.family.config;

import com.familyfinance.family.controller.FamilyController;
import com.familyfinance.family.service.FamilyService;
import com.familyfinance.family.support.CurrentUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FamilyController.class)
@Import({SecurityConfig.class, CurrentUserResolver.class, InternalApiKeyFilter.class})
@TestPropertySource(properties = {
        "familyfinance.security.enabled=true",
        "familyfinance.internal.api-key=test-internal-key"
})
class SecurityFilterChainTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    FamilyService familyService;

    @MockBean
    JwtDecoder jwtDecoder;

    @Test
    void rejectsUnauthenticatedApiRequest() throws Exception {
        mockMvc.perform(get("/api/families").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsHealthWithoutToken() throws Exception {
        // actuator may not be mapped in WebMvcTest slice — expect 404 not 401
        mockMvc.perform(get("/actuator/health"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    if (s == 401) {
                        throw new AssertionError("health must not require auth");
                    }
                });
    }

    @Test
    void acceptsAuthenticatedJwt() throws Exception {
        UUID userId = UUID.fromString("b1000000-0000-4000-8000-000000000001");
        when(familyService.listForUser(any())).thenReturn(List.of());

        Jwt token = Jwt.withTokenValue("t")
                .header("alg", "none")
                .subject(userId.toString())
                .claim("realm_access", Map.of("roles", List.of("PARENT")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .build();

        mockMvc.perform(get("/api/families").with(jwt().jwt(token)))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsInvalidAndExpiredJwtViaDecoder() {
        when(jwtDecoder.decode(anyString())).thenThrow(new JwtException("invalid or expired"));

        Exception thrown = org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                mockMvc.perform(get("/api/families")
                        .header("Authorization", "Bearer invalid-or-expired")
                        .accept(MediaType.APPLICATION_JSON)));

        Throwable root = thrown;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        org.junit.jupiter.api.Assertions.assertTrue(
                root instanceof JwtException || thrown.getMessage().toLowerCase().contains("invalid"),
                "JWT validation must fail-closed: " + thrown);
    }
}
