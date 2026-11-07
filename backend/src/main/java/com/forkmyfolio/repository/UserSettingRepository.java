package com.forkmyfolio.repository;

import com.forkmyfolio.model.User;
import com.forkmyfolio.model.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link UserSetting} entities.
 * Provides methods for data access and manipulation of user-specific settings.
 */
@Repository
public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {

    /**
     * Finds all settings for a specific user.
     *
     * @param user The user whose settings are to be retrieved.
     * @return A list of UserSetting objects.
     */
    List<UserSetting> findByUser(User user);

    /**
     * Finds a specific setting for a given user by its name.
     *
     * @param user The user who owns the setting.
     * @param name The name of the setting (e.g., "SHOW_SKILLS").
     * @return An Optional containing the UserSetting if found, otherwise empty.
     */
    Optional<UserSetting> findByUserAndName(User user, String name);
}