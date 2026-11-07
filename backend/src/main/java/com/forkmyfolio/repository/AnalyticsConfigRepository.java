package com.forkmyfolio.repository;

import com.forkmyfolio.model.AnalyticsConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AnalyticsConfigRepository extends JpaRepository<AnalyticsConfig, Long> {

    Optional<AnalyticsConfig> findByUuid(UUID uuid);

    /**
     * Returns the singleton platform configuration row, if it exists.
     * The service layer enforces that at most one row is ever created.
     */
    Optional<AnalyticsConfig> findFirstByOrderByIdAsc();
}
