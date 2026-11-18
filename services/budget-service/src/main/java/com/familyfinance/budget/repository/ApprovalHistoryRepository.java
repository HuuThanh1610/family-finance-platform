package com.familyfinance.budget.repository;

import com.familyfinance.budget.entity.ApprovalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, UUID> {
}