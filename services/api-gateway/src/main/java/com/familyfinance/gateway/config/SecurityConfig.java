package com.familyfinance.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Resource-server JWT when {@code familyfinance.security.enabled=true}.
 * <p>
 * Gateway {@code globalcors} does not attach CORS headers to security-generated 401/403.
 * A highest-precedence {@link WebFilter} mirrors {@code WEB_ORIGIN} onto responses
 * (including auth failures) without using wildcard origins with credentials.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    WebFilter spaCorsWebFilter(
            @Value("${familyfinance.web-origin:http://localhost:5173}") String webOrigin
    ) {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            String origin = exchange.getRequest().getHeaders().getOrigin();
            if (webOrigin.equals(origin)) {
                HttpHeaders headers = exchange.getResponse().getHeaders();
                headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
                headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,PATCH,DELETE,OPTIONS,HEAD");
                headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*");
                headers.set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "*");
                headers.add(HttpHeaders.VARY, HttpHeaders.ORIGIN);
            }
            if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
                exchange.getResponse().setStatusCode(HttpStatus.OK);
                return exchange.getResponse().setComplete();
            }
            return chain.filter(exchange);
        };
    }

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            @Value("${familyfinance.security.enabled:false}") boolean securityEnabled
    ) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable);
        if (!securityEnabled) {
            return http.authorizeExchange(ex -> ex.anyExchange().permitAll()).build();
        }
        return http
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeExchange(ex -> ex
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        .pathMatchers("/api/v1/payments/momo/ipn", "/api/v1/payments/fake/**").permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }
}
