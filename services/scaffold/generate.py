#!/usr/bin/env python3
"""Generate Spring Boot service scaffolding for Family Finance monorepo."""
from __future__ import annotations

import os
from pathlib import Path
from textwrap import dedent

ROOT = Path(__file__).resolve().parents[1]
BOOT_VERSION = "3.3.5"
SPRINGDOC_VERSION = "2.6.0"

SERVICES = [
    {"dir": "profile-service", "artifact": "profile-service", "pkg": "profile", "port": 8081,
     "db": "profile_db", "user": "profile_user", "pass": "changeme_profile", "kafka": False},
    {"dir": "family-service", "artifact": "family-service", "pkg": "family", "port": 8082,
     "db": "family_db", "user": "family_user", "pass": "changeme_family", "kafka": False},
    {"dir": "wallet-service", "artifact": "wallet-service", "pkg": "wallet", "port": 8083,
     "db": "wallet_db", "user": "wallet_user", "pass": "changeme_wallet", "kafka": True},
    {"dir": "payment-service", "artifact": "payment-service", "pkg": "payment", "port": 8084,
     "db": "payment_db", "user": "payment_user", "pass": "changeme_payment", "kafka": True},
    {"dir": "budget-service", "artifact": "budget-service", "pkg": "budget", "port": 8085,
     "db": "budget_db", "user": "budget_user", "pass": "changeme_budget", "kafka": True},
    {"dir": "expense-service", "artifact": "expense-service", "pkg": "expense", "port": 8086,
     "db": "expense_db", "user": "expense_user", "pass": "changeme_expense", "kafka": False},
    {"dir": "household-service", "artifact": "household-service", "pkg": "household", "port": 8087,
     "db": "household_db", "user": "household_user", "pass": "changeme_household", "kafka": False},
    {"dir": "notification-service", "artifact": "notification-service", "pkg": "notification", "port": 8088,
     "db": "notification_db", "user": "notification_user", "pass": "changeme_notification", "kafka": True},
    {"dir": "analytics-service", "artifact": "analytics-service", "pkg": "analytics", "port": 8089,
     "db": "analytics_db", "user": "analytics_user", "pass": "changeme_analytics", "kafka": False},
]


def write(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.strip() + "\n", encoding="utf-8")


def pom_xml(s: dict) -> str:
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>{BOOT_VERSION}</version>
        <relativePath/>
    </parent>

    <groupId>com.familyfinance</groupId>
    <artifactId>{s['artifact']}</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>{s['artifact']}</name>
    <description>Family Finance — {s['artifact']}</description>

    <properties>
        <java.version>21</java.version>
        <springdoc.version>{SPRINGDOC_VERSION}</springdoc.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${{springdoc.version}}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
"""


def application_yml(s: dict) -> str:
    kafka = ""
    if s["kafka"]:
        kafka = dedent("""

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: ${KAFKA_CONSUMER_GROUP:${spring.application.name}}
      auto-offset-reset: earliest
    # TODO: Kafka consumers/producers & outbox relay — reserved learning topic""")
    return f"""spring:
  application:
    name: {s['artifact']}
  datasource:
    url: ${{DB_URL:jdbc:postgresql://localhost:5432/{s['db']}}}
    username: ${{DB_USER:{s['user']}}}
    password: ${{DB_PASSWORD:{s['pass']}}}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        jdbc:
          time_zone: UTC
  flyway:
    enabled: true{kafka}
server:
  port: ${{SERVER_PORT:{s['port']}}}

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: when_authorized

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
"""


def common_java(pkg: str, title: str) -> dict[str, str]:
    base = f"com.familyfinance.{pkg}"
    app_class = "".join(p.capitalize() for p in pkg.split("-")) + "Application"
    if pkg == "profile":
        app_class = "ProfileServiceApplication"
    elif pkg.endswith("service"):
        pass
    app_names = {
        "profile": "ProfileServiceApplication",
        "family": "FamilyServiceApplication",
        "wallet": "WalletServiceApplication",
        "payment": "PaymentServiceApplication",
        "budget": "BudgetServiceApplication",
        "expense": "ExpenseServiceApplication",
        "household": "HouseholdServiceApplication",
        "notification": "NotificationServiceApplication",
        "analytics": "AnalyticsServiceApplication",
    }
    app_class = app_names[pkg]

    files: dict[str, str] = {}

    files[f"src/main/java/{base.replace('.', '/')}/{app_class}.java"] = f"""\
package {base};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class {app_class} {{

    public static void main(String[] args) {{
        SpringApplication.run({app_class}.class, args);
    }}
}}
"""

    files[f"src/main/java/{base.replace('.', '/')}/config/OpenApiConfig.java"] = f"""\
package {base}.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {{

    @Bean
    public OpenAPI familyFinanceOpenApi() {{
        return new OpenAPI()
                .info(new Info()
                        .title("{title}")
                        .description("Family Finance platform — read-focused scaffold")
                        .version("0.0.1"));
    }}
}}
"""

    files[f"src/main/java/{base.replace('.', '/')}/config/SecurityPlaceholderConfig.java"] = f"""\
package {base}.config;

import org.springframework.context.annotation.Configuration;

/**
 * Placeholder for OAuth2 Resource Server (Keycloak).
 * <pre>
 * // TODO: enable when integrating Keycloak JWT validation — reserved learning topic
 * // @EnableWebSecurity
 * // @Configuration
 * // public class SecurityConfig {{
 * //     @Bean
 * //     SecurityFilterChain securityFilterChain(HttpSecurity http) {{
 * //         http.authorizeHttpRequests(auth -> auth
 * //             .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
 * //             .anyRequest().authenticated())
 * //             .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
 * //         return http.build();
 * //     }}
 * // }}
 * </pre>
 */
@Configuration
public class SecurityPlaceholderConfig {{
}}
"""

    files[f"src/main/java/{base.replace('.', '/')}/exception/ResourceNotFoundException.java"] = f"""\
package {base}.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {{

    public ResourceNotFoundException(String resource, UUID id) {{
        super(resource + " not found: " + id);
    }}

    public ResourceNotFoundException(String message) {{
        super(message);
    }}
}}
"""

    files[f"src/main/java/{base.replace('.', '/')}/exception/ErrorResponse.java"] = f"""\
package {base}.exception;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {{
}}
"""

    files[f"src/main/java/{base.replace('.', '/')}/exception/GlobalExceptionHandler.java"] = f"""\
package {base}.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {{

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {{
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {{
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }}

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {{
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI());
    }}

    private static ResponseEntity<ErrorResponse> build(HttpStatus status, String message, String path) {{
        return ResponseEntity.status(status).body(new ErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), message, path));
    }}
}}
"""

    return files


# Entity spec: (class_name, table, fields as list of lines inside class)
def entity_class(pkg: str, class_name: str, table: str, body: str, imports_extra: str = "") -> str:
    base = f"com.familyfinance.{pkg}"
    imports = """import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;"""
    if "BigDecimal" in body:
        imports += "\nimport java.math.BigDecimal;"
    if "Map" in body or "Json" in imports_extra:
        imports += "\nimport java.util.Map;"
    if imports_extra:
        imports += "\n" + imports_extra
    if "@JdbcTypeCode" in body:
        imports += """
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;"""

    return f"""package {base}.entity;

{imports}

@Getter
@Setter
@Entity
@Table(name = "{table}")
public class {class_name} {{

{body}
}}
"""


def generate_profile() -> dict[str, str]:
    pkg = "profile"
    base = f"com.familyfinance.{pkg}"
    f: dict[str, str] = {}

    f["entity/UserProfile.java"] = entity_class(pkg, "UserProfile", "user_profiles", """\
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "keycloak_sub", nullable = false, length = 128)
    private String keycloakSub;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(length = 32)
    private String phone;

    @Column(name = "avatar_url", length = 2048)
    private String avatarUrl;

    @Column(nullable = false, length = 16)
    private String locale = "vi-VN";

    @Column(nullable = false, length = 64)
    private String timezone = "Asia/Ho_Chi_Minh";

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
""")

    f["entity/UserPreference.java"] = entity_class(pkg, "UserPreference", "user_preferences", """\
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(name = "preference_key", nullable = false, length = 128)
    private String preferenceKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preference_value", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> preferenceValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
""")

    f["entity/UserConsent.java"] = entity_class(pkg, "UserConsent", "user_consents", """\
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(name = "consent_type", nullable = false, length = 64)
    private String consentType;

    @Column(name = "policy_version", nullable = false, length = 32)
    private String policyVersion;

    @Column(nullable = false)
    private Boolean granted = true;

    @Column(name = "granted_at")
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
""")

    f["repository/UserProfileRepository.java"] = repo_iface(pkg, "UserProfile", "UUID")
    f["dto/UserProfileResponse.java"] = dto_record(pkg, "UserProfileResponse", [
        "UUID id", "String keycloakSub", "String email", "String displayName",
        "String phone", "String avatarUrl", "String locale", "String timezone",
        "String status", "Instant createdAt", "Instant updatedAt"
    ])
    f["service/UserProfileService.java"] = read_service(pkg, "UserProfile", "UserProfile", "UserProfileResponse", mapper_profile())
    f["controller/UserProfileController.java"] = controller_read(pkg, "UserProfile", "user-profiles", "UserProfileService", "UserProfileResponse")

    return prefix_paths(f, pkg)


def mapper_profile() -> str:
    return """\
    private UserProfileResponse toResponse(UserProfile entity) {
        return new UserProfileResponse(
                entity.getId(),
                entity.getKeycloakSub(),
                entity.getEmail(),
                entity.getDisplayName(),
                entity.getPhone(),
                entity.getAvatarUrl(),
                entity.getLocale(),
                entity.getTimezone(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }"""


def repo_iface(pkg: str, entity: str, id_type: str = "UUID") -> str:
    base = f"com.familyfinance.{pkg}"
    return f"""package {base}.repository;

import {base}.entity.{entity};
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface {entity}Repository extends JpaRepository<{entity}, {id_type if id_type == 'UUID' else id_type}> {{
}}
"""


def dto_record(pkg: str, name: str, fields: list[str]) -> str:
    base = f"com.familyfinance.{pkg}"
    params = ",\n        ".join(fields)
    return f"""package {base}.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.UUID;

public record {name}(
        {params}
) {{
}}
"""


def read_service(pkg: str, entity: str, repo: str, dto: str, mapper: str, extra_imports: str = "") -> str:
    base = f"com.familyfinance.{pkg}"
    return f"""package {base}.service;

import {base}.dto.{dto};
import {base}.entity.{entity};
import {base}.exception.ResourceNotFoundException;
import {base}.repository.{repo}Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
{extra_imports}

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class {entity}Service {{

    private final {repo}Repository repository;

    public {entity}Service({repo}Repository repository) {{
        this.repository = repository;
    }}

    public List<{dto}> findAll() {{
        return repository.findAll().stream().map(this::toResponse).toList();
    }}

    public {dto} findById(UUID id) {{
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("{entity}", id));
    }}

    {mapper}
}}
"""


def controller_read(pkg: str, entity: str, path: str, service: str, dto: str) -> str:
    base = f"com.familyfinance.{pkg}"
    return f"""package {base}.controller;

import {base}.dto.{dto};
import {base}.service.{service};
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/{path}")
public class {entity}Controller {{

    private final {service} service;

    public {entity}Controller({service} service) {{
        this.service = service;
    }}

    @GetMapping
    public List<{dto}> list() {{
        return service.findAll();
    }}

    @GetMapping("/{{id}}")
    public {dto} get(@PathVariable UUID id) {{
        return service.findById(id);
    }}
}}
"""


def prefix_paths(files: dict[str, str], pkg: str) -> dict[str, str]:
    base = f"com.familyfinance.{pkg}".replace(".", "/")
    return {f"src/main/java/{base}/{k}": v for k, v in files.items()}


def generate_family() -> dict[str, str]:
    pkg = "family"
    f: dict[str, str] = {}
    f["entity/Family.java"] = entity_class(pkg, "Family", "families", """\
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 120)
    private String slug;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
""")
    f["entity/FamilyMember.java"] = entity_class(pkg, "FamilyMember", "family_members", """\
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @Column(name = "user_profile_id", nullable = false)
    private UUID userProfileId;

    @Column(name = "role_code", nullable = false, length = 32)
    private String roleCode;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(length = 100)
    private String nickname;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
""")
    f["entity/FamilyRole.java"] = entity_class(pkg, "FamilyRole", "family_roles", """\
    @Id
    @Column(length = 32)
    private String code;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
""")
    f["repository/FamilyRepository.java"] = repo_iface(pkg, "Family")
    f["dto/FamilyResponse.java"] = dto_record(pkg, "FamilyResponse", [
        "UUID id", "String name", "String slug", "String description",
        "String status", "Instant createdAt", "Instant updatedAt"
    ])
    f["service/FamilyService.java"] = read_service(pkg, "Family", "Family", "FamilyResponse", """\
    private FamilyResponse toResponse(Family entity) {
        return new FamilyResponse(
                entity.getId(),
                entity.getName(),
                entity.getSlug(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }""")
    f["controller/FamilyController.java"] = controller_read(pkg, "Family", "families", "FamilyService", "FamilyResponse")
    return prefix_paths(f, pkg)


def generate_wallet() -> dict[str, str]:
    pkg = "wallet"
    f: dict[str, str] = {}
    f["entity/WalletAccount.java"] = entity_class(pkg, "WalletAccount", "wallet_accounts", """\
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "owner_member_id")
    private UUID ownerMemberId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "VND";

    @Column(name = "wallet_type", nullable = false, length = 32)
    private String walletType = "FAMILY_POOL";

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
""")
    f["entity/LedgerAccount.java"] = entity_class(pkg, "LedgerAccount", "ledger_accounts", """\
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_account_id", nullable = false)
    private WalletAccount walletAccount;

    @Column(name = "account_code", nullable = false, length = 64)
    private String accountCode;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "account_kind", nullable = false, length = 32)
    private String accountKind;

    @Column(name = "normal_balance", nullable = false, length = 6)
    private String normalBalance;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
""")
    f["entity/LedgerTransaction.java"] = entity_class(pkg, "LedgerTransaction", "ledger_transactions", """\
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_account_id", nullable = false)
    private WalletAccount walletAccount;

    @Column(name = "reference_id", nullable = false, length = 128)
    private String referenceId;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(length = 500)
    private String description;

    @Column(name = "transaction_type", nullable = false, length = 64)
    private String transactionType;

    @Column(name = "posted_at", nullable = false)
    private Instant postedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
""")
    f["entity/LedgerEntry.java"] = entity_class(pkg, "LedgerEntry", "ledger_entries", """\
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private LedgerTransaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ledger_account_id", nullable = false)
    private LedgerAccount ledgerAccount;

    @Column(name = "entry_type", nullable = false, length = 6)
    private String entryType;

    @Column(nullable = false)
    private Long amount;

    @Column(length = 255)
    private String memo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
""")
    f["entity/WalletBalance.java"] = entity_class(pkg, "WalletBalance", "wallet_balances", """\
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ledger_account_id", nullable = false, unique = true)
    private LedgerAccount ledgerAccount;

    @Column(name = "balance_amount", nullable = false)
    private Long balanceAmount = 0L;

    @Column(nullable = false)
    private Integer version = 0;

    @Column(name = "recalculated_at", nullable = false)
    private Instant recalculatedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
""")
    f["entity/InboxEvent.java"] = entity_class(pkg, "InboxEvent", "inbox_events", """\
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(nullable = false, length = 32)
    private String status = "PENDING";
""")
    f["entity/OutboxEvent.java"] = entity_class(pkg, "OutboxEvent", "outbox_events", """\
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false, length = 32)
    private String status = "PENDING";
""")
    f["service/LedgerPostingService.java"] = f"""package com.familyfinance.wallet.service;

import java.util.UUID;

/**
 * Double-entry ledger posting — intentionally not implemented in scaffold.
 */
public interface LedgerPostingService {{

    /**
     * TODO: Post a balanced ledger transaction with idempotency — reserved learning topic.
     */
    UUID postTransaction(LedgerPostCommand command);

    record LedgerPostCommand(
            UUID walletAccountId,
            String referenceId,
            String idempotencyKey,
            String transactionType,
            String description
    ) {{
    }}
}}
"""
    f["repository/WalletAccountRepository.java"] = repo_iface(pkg, "WalletAccount")
    f["dto/WalletAccountResponse.java"] = dto_record(pkg, "WalletAccountResponse", [
        "UUID id", "UUID familyId", "UUID ownerMemberId", "String name",
        "String currencyCode", "String walletType", "String status",
        "Instant createdAt", "Instant updatedAt"
    ])
    f["service/WalletAccountService.java"] = read_service(pkg, "WalletAccount", "WalletAccount", "WalletAccountResponse", """\
    private WalletAccountResponse toResponse(WalletAccount entity) {
        return new WalletAccountResponse(
                entity.getId(),
                entity.getFamilyId(),
                entity.getOwnerMemberId(),
                entity.getName(),
                entity.getCurrencyCode(),
                entity.getWalletType(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }""")
    f["controller/WalletAccountController.java"] = controller_read(pkg, "WalletAccount", "wallet-accounts", "WalletAccountService", "WalletAccountResponse")
    return prefix_paths(f, pkg)


def generate_payment() -> dict[str, str]:
    pkg = "payment"
    f: dict[str, str] = {}
    f["entity/PaymentOrder.java"] = entity_class(pkg, "PaymentOrder", "payment_orders", """\
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "payer_member_id")
    private UUID payerMemberId;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "VND";

    @Column(nullable = false, length = 64)
    private String purpose;

    @Column(name = "external_reference", length = 128)
    private String externalReference;

    @Column(nullable = false, length = 32)
    private String status = "CREATED";

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
""")
    f["entity/PaymentAttempt.java"] = entity_class(pkg, "PaymentAttempt", "payment_attempts", """\
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_order_id", nullable = false)
    private PaymentOrder paymentOrder;

    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;

    @Column(name = "provider_code", nullable = false, length = 64)
    private String providerCode;

    @Column(nullable = false, length = 32)
    private String status = "INITIATED";

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "initiated_at", nullable = false)
    private Instant initiatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
""")
    f["entity/OutboxEvent.java"] = entity_class(pkg, "OutboxEvent", "outbox_events", """\
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false, length = 32)
    private String status = "PENDING";
""")
    base = f"com.familyfinance.{pkg}"
    f["domain/PaymentStatus.java"] = f"""package {base}.domain;

public enum PaymentStatus {{
    CREATED,
    PENDING,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    CANCELLED,
    REFUNDED
}}
"""
    f["provider/PaymentProvider.java"] = f"""package {base}.provider;

import {base}.dto.InitiatePaymentRequest;
import {base}.dto.InitiatePaymentResponse;

/**
 * Abstraction over PSP integrations (e.g. MoMo). No signing logic in scaffold.
 */
public interface PaymentProvider {{

    String providerCode();

    /**
     * TODO: Initiate provider payment session — reserved learning topic (MoMo signing, no fake credentials).
     */
    InitiatePaymentResponse initiate(InitiatePaymentRequest request);
}}
"""
    f["dto/InitiatePaymentRequest.java"] = dto_record(pkg, "InitiatePaymentRequest", [
        "java.util.UUID paymentOrderId", "String providerCode", "Long amount", "String purpose"
    ]).replace("java.util.UUID paymentOrderId", "UUID paymentOrderId")
    f["dto/InitiatePaymentResponse.java"] = dto_record(pkg, "InitiatePaymentResponse", [
        "UUID paymentOrderId", "String status", "String redirectUrl"
    ])
    f["repository/PaymentOrderRepository.java"] = repo_iface(pkg, "PaymentOrder")
    f["dto/PaymentOrderResponse.java"] = dto_record(pkg, "PaymentOrderResponse", [
        "UUID id", "UUID familyId", "UUID payerMemberId", "Long amount",
        "String currencyCode", "String purpose", "String externalReference",
        "String status", "Instant expiresAt", "Instant createdAt", "Instant updatedAt"
    ])
    f["service/PaymentOrderService.java"] = read_service(pkg, "PaymentOrder", "PaymentOrder", "PaymentOrderResponse", """\
    private PaymentOrderResponse toResponse(PaymentOrder entity) {
        return new PaymentOrderResponse(
                entity.getId(),
                entity.getFamilyId(),
                entity.getPayerMemberId(),
                entity.getAmount(),
                entity.getCurrencyCode(),
                entity.getPurpose(),
                entity.getExternalReference(),
                entity.getStatus(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }""")
    f["controller/PaymentOrderController.java"] = f"""package {base}.controller;

import {base}.dto.InitiatePaymentRequest;
import {base}.dto.InitiatePaymentResponse;
import {base}.dto.PaymentOrderResponse;
import {base}.service.PaymentOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment-orders")
public class PaymentOrderController {{

    private final PaymentOrderService service;

    public PaymentOrderController(PaymentOrderService service) {{
        this.service = service;
    }}

    @GetMapping
    public List<PaymentOrderResponse> list() {{
        return service.findAll();
    }}

    @GetMapping("/{{id}}")
    public PaymentOrderResponse get(@PathVariable UUID id) {{
        return service.findById(id);
    }}

    /**
     * TODO: Create payment order + dispatch to PaymentProvider — reserved learning topic.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public PaymentOrderResponse create(@RequestBody InitiatePaymentRequest request) {{
        throw new UnsupportedOperationException("Payment order creation not implemented in scaffold");
    }}

    /**
     * TODO: Provider callback/webhook ingestion via webhook_inbox — reserved learning topic.
     */
    @PostMapping("/{{id}}/initiate")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public InitiatePaymentResponse initiate(@PathVariable UUID id, @RequestBody InitiatePaymentRequest request) {{
        throw new UnsupportedOperationException("Payment initiation not implemented in scaffold");
    }}
}}
"""
    return prefix_paths(f, pkg)


def generate_budget() -> dict[str, str]:
    pkg = "budget"
    f: dict[str, str] = {}
    f["entity/Budget.java"] = entity_class(pkg, "Budget", "budgets", """\
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "total_limit", nullable = false)
    private Long totalLimit;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "VND";

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
""")
    f["entity/BudgetCategory.java"] = entity_class(pkg, "BudgetCategory", "budget_categories", """\
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @Column(name = "category_code", nullable = false, length = 64)
    private String categoryCode;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "allocated_amount", nullable = false)
    private Long allocatedAmount;

    @Column(name = "spent_amount", nullable = false)
    private Long spentAmount = 0L;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
""")
    f["repository/BudgetRepository.java"] = repo_iface(pkg, "Budget")
    f["dto/BudgetResponse.java"] = dto_record(pkg, "BudgetResponse", [
        "UUID id", "UUID familyId", "String name", "LocalDate periodStart", "LocalDate periodEnd",
        "Long totalLimit", "String currencyCode", "String status", "Instant createdAt", "Instant updatedAt"
    ])
    f["service/BudgetService.java"] = read_service(pkg, "Budget", "Budget", "BudgetResponse", """\
    private BudgetResponse toResponse(Budget entity) {
        return new BudgetResponse(
                entity.getId(),
                entity.getFamilyId(),
                entity.getName(),
                entity.getPeriodStart(),
                entity.getPeriodEnd(),
                entity.getTotalLimit(),
                entity.getCurrencyCode(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }""")
    f["controller/BudgetController.java"] = controller_read(pkg, "Budget", "budgets", "BudgetService", "BudgetResponse")
    return prefix_paths(f, pkg)


def generate_expense() -> dict[str, str]:
    pkg = "expense"
    f: dict[str, str] = {}
    f["entity/ExpenseCategory.java"] = entity_class(pkg, "ExpenseCategory", "expense_categories", """\
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ExpenseCategory parent;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
""")
    f["entity/Expense.java"] = entity_class(pkg, "Expense", "expenses", """\
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "paid_by_member_id")
    private UUID paidByMemberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ExpenseCategory category;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(name = "amount_total", nullable = false)
    private Long amountTotal;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false, length = 32)
    private String status = "RECORDED";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
""")
    f["repository/ExpenseRepository.java"] = repo_iface(pkg, "Expense")
    f["dto/ExpenseResponse.java"] = dto_record(pkg, "ExpenseResponse", [
        "UUID id", "UUID familyId", "UUID paidByMemberId", "UUID categoryId", "String title",
        "Long amountTotal", "LocalDate expenseDate", "String notes", "String status",
        "Instant createdAt", "Instant updatedAt"
    ])
    f["service/ExpenseService.java"] = read_service(pkg, "Expense", "Expense", "ExpenseResponse", """\
    private ExpenseResponse toResponse(Expense entity) {
        return new ExpenseResponse(
                entity.getId(),
                entity.getFamilyId(),
                entity.getPaidByMemberId(),
                entity.getCategory() != null ? entity.getCategory().getId() : null,
                entity.getTitle(),
                entity.getAmountTotal(),
                entity.getExpenseDate(),
                entity.getNotes(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }""")
    f["controller/ExpenseController.java"] = controller_read(pkg, "Expense", "expenses", "ExpenseService", "ExpenseResponse")
    return prefix_paths(f, pkg)


def generate_household() -> dict[str, str]:
    pkg = "household"
    f: dict[str, str] = {}
    f["entity/ItemCatalog.java"] = entity_class(pkg, "ItemCatalog", "item_catalog", """\
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false, length = 300)
    private String name;

    @Column(length = 64)
    private String category;

    @Column(name = "default_unit", nullable = false, length = 32)
    private String defaultUnit = "pcs";

    @Column(length = 64)
    private String barcode;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
""")
    f["entity/ShoppingList.java"] = entity_class(pkg, "ShoppingList", "shopping_lists", """\
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 32)
    private String status = "OPEN";

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_by_member_id")
    private UUID createdByMemberId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
""")
    f["repository/ShoppingListRepository.java"] = repo_iface(pkg, "ShoppingList")
    f["dto/ShoppingListResponse.java"] = dto_record(pkg, "ShoppingListResponse", [
        "UUID id", "UUID familyId", "String title", "String status", "LocalDate dueDate",
        "UUID createdByMemberId", "Instant createdAt", "Instant updatedAt"
    ])
    f["service/ShoppingListService.java"] = read_service(pkg, "ShoppingList", "ShoppingList", "ShoppingListResponse", """\
    private ShoppingListResponse toResponse(ShoppingList entity) {
        return new ShoppingListResponse(
                entity.getId(),
                entity.getFamilyId(),
                entity.getTitle(),
                entity.getStatus(),
                entity.getDueDate(),
                entity.getCreatedByMemberId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }""")
    f["controller/ShoppingListController.java"] = controller_read(pkg, "ShoppingList", "shopping-lists", "ShoppingListService", "ShoppingListResponse")
    return prefix_paths(f, pkg)


def generate_notification() -> dict[str, str]:
    pkg = "notification"
    f: dict[str, str] = {}
    f["entity/Notification.java"] = entity_class(pkg, "Notification", "notifications", """\
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "recipient_user_id", nullable = false)
    private UUID recipientUserId;

    @Column(name = "family_id")
    private UUID familyId;

    @Column(name = "template_key", length = 128)
    private String templateKey;

    @Column(nullable = false, length = 32)
    private String channel;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(nullable = false, length = 32)
    private String status = "QUEUED";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
""")
    f["repository/NotificationRepository.java"] = repo_iface(pkg, "Notification")
    f["dto/NotificationResponse.java"] = dto_record(pkg, "NotificationResponse", [
        "UUID id", "UUID recipientUserId", "UUID familyId", "String templateKey", "String channel",
        "String title", "String body", "String status", "Instant createdAt", "Instant updatedAt"
    ])
    f["service/NotificationService.java"] = read_service(pkg, "Notification", "Notification", "NotificationResponse", """\
    private NotificationResponse toResponse(Notification entity) {
        return new NotificationResponse(
                entity.getId(),
                entity.getRecipientUserId(),
                entity.getFamilyId(),
                entity.getTemplateKey(),
                entity.getChannel(),
                entity.getTitle(),
                entity.getBody(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }""")
    f["controller/NotificationController.java"] = controller_read(pkg, "Notification", "notifications", "NotificationService", "NotificationResponse")
    return prefix_paths(f, pkg)


def generate_analytics() -> dict[str, str]:
    pkg = "analytics"
    f: dict[str, str] = {}
    f["entity/FamilyMonthlySummary.java"] = entity_class(pkg, "FamilyMonthlySummary", "family_monthly_summary", """\
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "summary_month", nullable = false)
    private LocalDate summaryMonth;

    @Column(name = "total_income", nullable = false)
    private Long totalIncome = 0L;

    @Column(name = "total_expense", nullable = false)
    private Long totalExpense = 0L;

    @Column(name = "net_cashflow", nullable = false)
    private Long netCashflow = 0L;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "VND";

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
""")
    f["entity/MemberSpendingSummary.java"] = entity_class(pkg, "MemberSpendingSummary", "member_spending_summary", """\
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "summary_month", nullable = false)
    private LocalDate summaryMonth;

    @Column(name = "total_spent", nullable = false)
    private Long totalSpent = 0L;

    @Column(name = "transaction_count", nullable = false)
    private Integer transactionCount = 0;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "VND";

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
""")
    f["repository/FamilyMonthlySummaryRepository.java"] = repo_iface(pkg, "FamilyMonthlySummary")
    f["dto/FamilyMonthlySummaryResponse.java"] = dto_record(pkg, "FamilyMonthlySummaryResponse", [
        "UUID id", "UUID familyId", "LocalDate summaryMonth", "Long totalIncome", "Long totalExpense",
        "Long netCashflow", "String currencyCode", "Instant generatedAt", "Instant createdAt", "Instant updatedAt"
    ])
    f["service/FamilyMonthlySummaryService.java"] = read_service(pkg, "FamilyMonthlySummary", "FamilyMonthlySummary", "FamilyMonthlySummaryResponse", """\
    private FamilyMonthlySummaryResponse toResponse(FamilyMonthlySummary entity) {
        return new FamilyMonthlySummaryResponse(
                entity.getId(),
                entity.getFamilyId(),
                entity.getSummaryMonth(),
                entity.getTotalIncome(),
                entity.getTotalExpense(),
                entity.getNetCashflow(),
                entity.getCurrencyCode(),
                entity.getGeneratedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }""")
    f["controller/FamilyMonthlySummaryController.java"] = controller_read(
        pkg, "FamilyMonthlySummary", "family-monthly-summaries", "FamilyMonthlySummaryService", "FamilyMonthlySummaryResponse")
    return prefix_paths(f, pkg)


GENERATORS = {
    "profile": generate_profile,
    "family": generate_family,
    "wallet": generate_wallet,
    "payment": generate_payment,
    "budget": generate_budget,
    "expense": generate_expense,
    "household": generate_household,
    "notification": generate_notification,
    "analytics": generate_analytics,
}


def main() -> None:
    for s in SERVICES:
        svc_dir = ROOT / s["dir"]
        write(svc_dir / "pom.xml", pom_xml(s))
        write(svc_dir / "src/main/resources/application.yml", application_yml(s))

        title = s["artifact"].replace("-", " ").title()
        files = common_java(s["pkg"], title)
        files.update(GENERATORS[s["pkg"]]())

        for rel, content in files.items():
            write(svc_dir / rel, content)

        print(f"Generated {s['dir']}")

    aggregator = """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.familyfinance</groupId>
    <artifactId>services-aggregator</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>Family Finance Services (aggregator only)</name>

    <modules>
        <module>profile-service</module>
        <module>family-service</module>
        <module>wallet-service</module>
        <module>payment-service</module>
        <module>budget-service</module>
        <module>expense-service</module>
        <module>household-service</module>
        <module>notification-service</module>
        <module>analytics-service</module>
    </modules>
</project>
"""
    write(ROOT / "pom.xml", aggregator)

    readme = """# Family Finance — Backend Services

Mỗi microservice là **Maven module độc lập** (parent `spring-boot-starter-parent` riêng). Có thể build/chạy từng service không cần reactor.

## Yêu cầu

- Java 21
- Maven 3.9+
- PostgreSQL (xem `infra/postgres/init/01-create-databases.sql`)

## Build một service

```bash
mvn -q -f services/family-service/pom.xml -DskipTests package
```

## Chạy dev

```bash
export DB_URL=jdbc:postgresql://localhost:5432/family_db
export DB_USER=family_user
export DB_PASSWORD=changeme_family
mvn -f services/family-service/pom.xml spring-boot:run
```

Hoặc chỉ dùng default trong `application.yml` (localhost + user placeholder).

## Cổng mặc định

| Service | Port | Database |
|---------|------|----------|
| profile-service | 8081 | profile_db |
| family-service | 8082 | family_db |
| wallet-service | 8083 | wallet_db |
| payment-service | 8084 | payment_db |
| budget-service | 8085 | budget_db |
| expense-service | 8086 | expense_db |
| household-service | 8087 | household_db |
| notification-service | 8088 | notification_db |
| analytics-service | 8089 | analytics_db |

## OpenAPI

Swagger UI: `http://localhost:<port>/swagger-ui.html`

## Chủ đề để lại TODO (học sau)

- OAuth2 Resource Server (Keycloak)
- Double-entry `LedgerPostingService`
- MoMo / PSP signing & webhooks
- Outbox relay & Kafka consumers
- Integration/E2E tests (Testcontainers)
"""
    write(ROOT / "README.md", readme)

    tests_readme = """# Integration test scaffolding (Testcontainers)

Không có test giả `assertTrue` trong scaffold. Thêm module test theo từng service khi sẵn sàng.

## Kịch bản giá trị cao (gợi ý)

### profile-service
- Flyway migrate + đọc `user_profiles` seed (V99)
- Unique constraint `keycloak_sub` / email

### family-service
- RBAC seed `family_roles` / `family_permissions`
- Đọc members theo `family_id`

### wallet-service
- **Không** test CRUD số dư; chỉ đọc `wallet_accounts`
- TODO: double-entry invariant (DEBIT=CREDIT) khi `LedgerPostingService` được implement

### payment-service
- Đọc `payment_orders`; webhook inbox idempotency (khi có processor)

### budget-service
- Allowance request workflow + outbox row pending

### expense-service
- Chi phí theo `family_id` + category hierarchy

### household-service
- Shopping list + catalog SKU unique per family

### notification-service
- Template unique (key, locale, channel)

### analytics-service
- Upsert natural key `family_id + summary_month`

## Mẫu dependency (test scope)

```xml
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>postgresql</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>junit-jupiter</artifactId>
  <scope>test</scope>
</dependency>
```

```java
// TODO: @SpringBootTest + @ServiceConnection hoặc @DynamicPropertySource — reserved learning topic
```
"""
    write(ROOT / "tests" / "README.md", tests_readme)


if __name__ == "__main__":
    main()