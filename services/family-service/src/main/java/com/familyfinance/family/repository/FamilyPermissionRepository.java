package com.familyfinance.family.repository;

import com.familyfinance.family.entity.FamilyPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FamilyPermissionRepository extends JpaRepository<FamilyPermission, UUID> {

    boolean existsByRoleCodeAndPermissionKey(String roleCode, String permissionKey);

    List<FamilyPermission> findByRoleCode(String roleCode);
}