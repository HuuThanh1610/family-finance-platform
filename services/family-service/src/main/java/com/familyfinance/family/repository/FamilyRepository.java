package com.familyfinance.family.repository;

import com.familyfinance.family.entity.Family;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FamilyRepository extends JpaRepository<Family, UUID> {

    boolean existsBySlug(String slug);

    Optional<Family> findBySlug(String slug);

    List<Family> findByIdInAndStatus(List<UUID> ids, String status);
}
