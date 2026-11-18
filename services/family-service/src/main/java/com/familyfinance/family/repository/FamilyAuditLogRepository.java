package com.familyfinance.family.repository;

import com.familyfinance.family.entity.FamilyAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FamilyAuditLogRepository extends JpaRepository<FamilyAuditLog, UUID> {
}