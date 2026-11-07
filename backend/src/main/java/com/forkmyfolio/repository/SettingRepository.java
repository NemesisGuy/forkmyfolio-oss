package com.forkmyfolio.repository;

import com.forkmyfolio.model.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettingRepository extends JpaRepository<Setting, Long> {
    Optional<Setting> findByName(String name);

    Optional<Setting> findByUuid(UUID uuid);

    List<Setting> findByUuidIn(Collection<UUID> uuids);

}