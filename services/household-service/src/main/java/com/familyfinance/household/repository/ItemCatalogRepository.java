package com.familyfinance.household.repository;

import com.familyfinance.household.entity.ItemCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ItemCatalogRepository extends JpaRepository<ItemCatalog, UUID> {
}
