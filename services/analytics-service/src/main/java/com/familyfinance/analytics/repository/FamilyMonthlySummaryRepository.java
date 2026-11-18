package com.familyfinance.analytics.repository;

import com.familyfinance.analytics.entity.FamilyMonthlySummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FamilyMonthlySummaryRepository extends JpaRepository<FamilyMonthlySummary, UUID> {
}
