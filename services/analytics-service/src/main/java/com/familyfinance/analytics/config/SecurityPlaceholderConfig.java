package com.familyfinance.analytics.config;

import org.springframework.context.annotation.Configuration;

/**
 * Placeholder for OAuth2 Resource Server (Keycloak).
 * <pre>
 * // TODO: enable when integrating Keycloak JWT validation — reserved learning topic
 * // @EnableWebSecurity
 * // @Configuration
 * // public class SecurityConfig {
 * //     @Bean
 * //     SecurityFilterChain securityFilterChain(HttpSecurity http) {
 * //         http.authorizeHttpRequests(auth -> auth
 * //             .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
 * //             .anyRequest().authenticated())
 * //             .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
 * //         return http.build();
 * //     }
 * // }
 * </pre>
 */
@Configuration
public class SecurityPlaceholderConfig {
}
