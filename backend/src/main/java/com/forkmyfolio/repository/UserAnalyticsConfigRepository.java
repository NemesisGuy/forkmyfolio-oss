package com.forkmyfolio.repository;

import com.forkmyfolio.model.UserAnalyticsConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAnalyticsConfigRepository extends JpaRepository<UserAnalyticsConfig, Long> {

    Optional<UserAnalyticsConfig> findByUuid(UUID uuid);

    Optional<UserAnalyticsConfig> findByUserId(Long userId);
}
