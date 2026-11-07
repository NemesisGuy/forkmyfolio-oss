package com.forkmyfolio.repository;

import com.forkmyfolio.model.GuestPass;
import com.forkmyfolio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
