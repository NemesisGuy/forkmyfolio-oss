package com.forkmyfolio.repository;

import com.forkmyfolio.model.Announcement;
import com.forkmyfolio.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /**
     * Finds an announcement by its public UUID.
     */
    java.util.Optional<Announcement> findByUuid(UUID uuid);

    /**
     * Deletes an announcement by its public UUID.
     */
    void deleteByUuid(UUID uuid);

    /**
     * Finds active announcements for a specific role or global ones.
     * 
     * @param role The role to filter by.
     * @return A list of announcements.
     */
    @Query("SELECT a FROM Announcement a WHERE a.targetRole IS NULL OR a.targetRole = :role ORDER BY a.createdAt DESC")
    List<Announcement> findAllForRole(@Param("role") Role role);

    /**
     * Finds all global announcements.
     * 
     * @return A list of announcements.
     */
    List<Announcement> findByTargetRoleIsNullOrderByCreatedAtDesc();
}
