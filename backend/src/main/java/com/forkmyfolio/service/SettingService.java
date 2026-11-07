package com.forkmyfolio.service;

import com.forkmyfolio.model.Setting;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SettingService {
    List<Setting> getAllSettings();

    List<Setting> updateSettings(Map<UUID, String> settingsToUpdate);

    /**
     * Ensures that a default set of application settings exists in the database.
     * This is an idempotent operation; it will only create settings that are missing.
     */
    void createDefaultSettings();
}