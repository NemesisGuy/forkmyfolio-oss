package com.forkmyfolio.repository;

import com.forkmyfolio.model.GuestPass;
import com.forkmyfolio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GuestPassRepository extends JpaRepository<GuestPass, Long> {
    Optional<GuestPass> findByToken(String token);

    Optional<GuestPass> findByUuid(UUID uuid);

    List<GuestPass> findByOwner(User owner);

    void deleteByExpiresAtBefore(Instant now);

    /** Bulk delete of passes issued by an owner before the owner is removed. */
    @Modifying
    @Query("DELETE FROM GuestPass g WHERE g.owner = :owner")
    int deleteByOwner(@Param("owner") User owner);
}
