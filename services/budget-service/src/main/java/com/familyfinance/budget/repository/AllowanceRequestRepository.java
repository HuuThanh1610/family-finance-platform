package com.familyfinance.budget.repository;

import com.familyfinance.budget.entity.AllowanceRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AllowanceRequestRepository extends JpaRepository<AllowanceRequest, UUID> {

    List<AllowanceRequest> findByFamilyIdOrderByCreatedAtDesc(UUID familyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM AllowanceRequest r WHERE r.id = :id")
    Optional<AllowanceRequest> findByIdForUpdate(@Param("id") UUID id);
}