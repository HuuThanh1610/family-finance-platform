package com.familyfinance.profile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "user_preferences")
public class UserPreference {

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

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
