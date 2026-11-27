package com.forkmyfolio.repository;

import com.forkmyfolio.model.UserAnalyticsConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserAnalyticsConfigRepository extends JpaRepository<UserAnalyticsConfig, Long> {

    Optional<UserAnalyticsConfig> findByUuid(UUID uuid);

    Optional<UserAnalyticsConfig> findByUserId(Long userId);

    /** Bulk delete of an owner's analytics settings before the user is removed. */
    @Modifying
    @Query("DELETE FROM UserAnalyticsConfig c WHERE c.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
