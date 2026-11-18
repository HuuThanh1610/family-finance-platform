package com.familyfinance.budget.repository;

import com.familyfinance.budget.entity.AllowancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AllowancePolicyRepository extends JpaRepository<AllowancePolicy, UUID> {

    List<AllowancePolicy> findByFamilyIdOrderByCreatedAtDesc(UUID familyId);
}