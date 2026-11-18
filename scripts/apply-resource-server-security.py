#!/usr/bin/env python3
"""Apply OAuth2 Resource Server SecurityConfig to all Spring Boot services."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SERVICES = [
    "profile-service",
    "family-service",
    "wallet-service",
    "payment-service",
    "budget-service",
    "expense-service",
    "household-service",
    "notification-service",
    "analytics-service",
]

PUBLIC_EXTRA = {
    "payment-service": '                        .requestMatchers("/api/v1/payments/momo/ipn").permitAll()\n',
}

SECURITY_JAVA = '''package com.familyfinance.{pkg}.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Independent OAuth2 Resource Server. Gateway is not the sole enforcement layer.
 * Set familyfinance.security.enabled=true with KEYCLOAK_ISSUER_URI for production-like local runs.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {{

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${{familyfinance.security.enabled:false}}") boolean securityEnabled
    ) throws Exception {{
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (!securityEnabled) {{
            return http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
        }}

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
{extra}                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtConverter())));
        return http.build();
    }}

    private JwtAuthenticationConverter keycloakJwtConverter() {{
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {{
            List<GrantedAuthority> authorities = new ArrayList<>();
            Object realmAccess = jwt.getClaim("realm_access");
            if (realmAccess instanceof Map<?, ?> map) {{
                Object roles = map.get("roles");
                if (roles instanceof Collection<?> collection) {{
                    for (Object role : collection) {{
                        if (role != null) {{
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                        }}
                    }}
                }}
            }}
            return authorities;
        }});
        return converter;
    }}
}}
'''

YAML_SNIPPET = '''
spring.security.oauth2.resourceserver.jwt:
  issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8180/realms/family-finance}
  audiences: ${KEYCLOAK_AUDIENCE:account,family-finance-web}

familyfinance.security:
  enabled: ${SECURITY_ENABLED:false}
'''

DEP = '''        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-security-oauth2-resource-server</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
'''


def package_name(service: str) -> str:
    return service.replace("-service", "").replace("-", "")


def ensure_pom(pom: Path) -> None:
    text = pom.read_text()
    if "spring-security-oauth2-resource-server" in text:
        return
    needle = """        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>"""
    if needle not in text:
        raise SystemExit(f"actuator dep not found in {pom}")
    pom.write_text(text.replace(needle, needle + "\n" + DEP, 1))
    print("pom+", pom)


def ensure_yaml(yml: Path) -> None:
    text = yml.read_text()
    if "familyfinance.security" in text or "familyfinance:\n  security:" in text:
        if "issuer-uri" in text:
            return
    # Prefer nested familyfinance if present
    if "\nfamilyfinance:" in text and "security:" not in text.split("familyfinance:", 1)[1][:200]:
        text = text.replace("\nfamilyfinance:\n", "\nfamilyfinance:\n  security:\n    enabled: ${SECURITY_ENABLED:false}\n", 1)
    if "spring.security.oauth2" not in text and "resourceserver:" not in text:
        # insert under spring:
        lines = text.splitlines(True)
        out = []
        inserted = False
        for i, line in enumerate(lines):
            out.append(line)
            if not inserted and line.startswith("spring:"):
                out.append("  security:\n")
                out.append("    oauth2:\n")
                out.append("      resourceserver:\n")
                out.append("        jwt:\n")
                out.append("          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8180/realms/family-finance}\n")
                out.append("          audiences: ${KEYCLOAK_AUDIENCE:account,family-finance-web}\n")
                inserted = True
        text = "".join(out)
    if "familyfinance:" not in text:
        text += "\nfamilyfinance:\n  security:\n    enabled: ${SECURITY_ENABLED:false}\n"
    elif "security:\n    enabled:" not in text and "familyfinance.security" not in text:
        text = text.replace("\nfamilyfinance:\n", "\nfamilyfinance:\n  security:\n    enabled: ${SECURITY_ENABLED:false}\n", 1)
    yml.write_text(text)
    print("yml+", yml)


def main() -> None:
    for service in SERVICES:
        base = ROOT / "services" / service
        pkg = package_name(service)
        # map names correctly
        pkg_map = {
            "profile-service": "profile",
            "family-service": "family",
            "wallet-service": "wallet",
            "payment-service": "payment",
            "budget-service": "budget",
            "expense-service": "expense",
            "household-service": "household",
            "notification-service": "notification",
            "analytics-service": "analytics",
        }
        pkg = pkg_map[service]
        ensure_pom(base / "pom.xml")
        cfg_dir = base / "src/main/java/com/familyfinance" / pkg / "config"
        cfg_dir.mkdir(parents=True, exist_ok=True)
        placeholder = cfg_dir / "SecurityPlaceholderConfig.java"
        security = cfg_dir / "SecurityConfig.java"
        extra = PUBLIC_EXTRA.get(service, "")
        security.write_text(SECURITY_JAVA.format(pkg=pkg, extra=extra))
        if placeholder.exists():
            placeholder.unlink()
            print("rm", placeholder)
        print("wrote", security)
        ensure_yaml(base / "src/main/resources/application.yml")


if __name__ == "__main__":
    main()
