package com.familyfinance.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(
        properties = {
                "familyfinance.security.enabled=true",
                "familyfinance.web-origin=http://localhost:5173",
                "spring.cloud.gateway.routes[0].id=profiles-stub",
                "spring.cloud.gateway.routes[0].uri=http://127.0.0.1:9",
                "spring.cloud.gateway.routes[0].predicates[0]=Path=/api/v1/profiles/**",
                "management.otlp.tracing.endpoint=",
                "management.tracing.enabled=false"
        }
)
@AutoConfigureWebTestClient
class GatewaySecurityCorsTest {

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    ReactiveJwtDecoder reactiveJwtDecoder;

    @Test
    void unauthorizedIncludesCorsAllowOriginForWebOrigin() {
        when(reactiveJwtDecoder.decode(anyString())).thenReturn(Mono.error(new JwtException("test-reject")));

        webTestClient.get()
                .uri("/api/v1/profiles/me")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173")
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    }

    @Test
    void preflightOptionsReturnsCorsHeadersForWebOrigin() {
        webTestClient.options()
                .uri("/api/v1/profiles/me")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173");
    }

    @Test
    void disallowedOriginDoesNotReceiveAllowOriginHeader() {
        webTestClient.get()
                .uri("/api/v1/profiles/me")
                .header(HttpHeaders.ORIGIN, "http://evil.example")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
    }
}
