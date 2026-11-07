package com.forkmyfolio.service.impl;

import com.forkmyfolio.exception.ResourceNotFoundException;
import com.forkmyfolio.model.Setting;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.UserSetting;
import com.forkmyfolio.repository.SettingRepository;
import com.forkmyfolio.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service layer for managing user-specific settings.
 * Adheres to NAG-01 (DBR): Operates exclusively on Domain Models and Maps.
 */
@Service
@RequiredArgsConstructor
public class UserSettingService {

    private final UserSettingRepository userSettingRepository;
    private final SettingRepository settingRepository;

    /**
     * Gets all global setting definitions.
     */
    public List<Setting> getAllGlobalSettings() {
        return settingRepository.findAll();
    }

    /**
     * Gets user-specific override values as a map.
     *
     * @param user The user for whom to get overrides.
     * @return A map of setting names to their user-specific values.
     */
    public Map<String, String> getUserOverrides(User user) {
        return userSettingRepository.findByUser(user).stream()
                .collect(Collectors.toMap(UserSetting::getName, UserSetting::getValue));
    }

    /**
     * Updates a set of user-specific settings.
     *
     * @param user    The user whose settings are being updated.
     * @param updates A map of Setting UUID to the new value.
     */
    @Transactional
    public void updateUserSettings(User user, Map<UUID, String> updates) {
        // 1. Get all global settings to validate UUIDs and get names.
        Map<UUID, Setting> globalSettingsMap = getAllGlobalSettings().stream()
                .collect(Collectors.toMap(Setting::getUuid, Function.identity()));

        // 2. Fetch existing user-specific settings for this user.
        Map<String, UserSetting> existingUserSettingsMap = userSettingRepository.findByUser(user).stream()
                .collect(Collectors.toMap(UserSetting::getName, Function.identity()));

        List<UserSetting> settingsToSave = updates.entrySet().stream().map(entry -> {
            UUID settingUuid = entry.getKey();
            String value = entry.getValue();

            // 3. Validate UUID.
            Setting globalSetting = globalSettingsMap.get(settingUuid);
            if (globalSetting == null) {
                throw new ResourceNotFoundException("Setting with UUID " + settingUuid + " not found.");
            }
            String settingName = globalSetting.getName();

            // 4. Update or create override.
            UserSetting userSetting = existingUserSettingsMap.get(settingName);
            if (userSetting != null) {
                userSetting.setValue(value);
            } else {
                userSetting = new UserSetting(user, settingName, value);
            }
            return userSetting;
        }).collect(Collectors.toList());

        userSettingRepository.saveAll(settingsToSave);
    }
}