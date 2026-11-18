package com.familyfinance.family.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "family_permissions")
public class FamilyPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "role_code", nullable = false, length = 32)
    private String roleCode;

    @Column(name = "permission_key", nullable = false, length = 128)
    private String permissionKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}