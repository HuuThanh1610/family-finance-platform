package com.familyfinance.analytics.repository;

import com.familyfinance.analytics.entity.MemberSpendingSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberSpendingSummaryRepository extends JpaRepository<MemberSpendingSummary, UUID> {

    List<MemberSpendingSummary> findByFamilyIdAndSummaryMonth(UUID familyId, LocalDate summaryMonth);

    Optional<MemberSpendingSummary> findByFamilyIdAndMemberIdAndSummaryMonth(
            UUID familyId, UUID memberId, LocalDate summaryMonth);
}