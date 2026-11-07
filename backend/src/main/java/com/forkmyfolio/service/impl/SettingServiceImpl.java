package com.forkmyfolio.service.impl;

import com.forkmyfolio.model.Setting;
import com.forkmyfolio.repository.SettingRepository;
import com.forkmyfolio.service.SettingService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
// Hidden Lines
public class SettingServiceImpl implements SettingService {

    //logger
    private final Logger log = LoggerFactory.getLogger(SettingServiceImpl.class);

    private final SettingRepository settingRepository;

    @Override
    @Transactional(readOnly = true) // Switched to Spring's Transactional
    public List<Setting> getAllSettings() {
        return settingRepository.findAll();
    }

    @Override
    @Transactional
    public List<Setting> updateSettings(Map<UUID, String> settingsToUpdate) {
        List<UUID> uuids = settingsToUpdate.keySet().stream().toList();
        List<Setting> settingsToModify = settingRepository.findByUuidIn(uuids);

        if (settingsToModify.size() != uuids.size()) {
            throw new EntityNotFoundException("One or more settings could not be found for the given UUIDs.");
        }

        settingsToModify.forEach(setting ->
                setting.setValue(settingsToUpdate.get(setting.getUuid()))
        );

        return settingRepository.saveAll(settingsToModify);
    }

    /**
     * Gets all settings intended for public consumption.
     * For backward compatibility, this converts settings with "true" or "false" values
     * into a Map<String, Boolean> for the public visibility toggles.
     */
    @Transactional
    public Map<String, Boolean> getPublicSettings() {
        return settingRepository.findAll().stream()
                .filter(s -> "true".equalsIgnoreCase(s.getValue()) || "false".equalsIgnoreCase(s.getValue()))
                .collect(Collectors.toMap(Setting::getName, s -> Boolean.parseBoolean(s.getValue())));
    }

    @Override
    @Transactional
    public void createDefaultSettings() {
        log.info("Checking and initializing default application settings...");
        createSettingIfNotExists("portfolio.theme", "modern", "The visual theme for the public portfolio.");
        // FIX: Normalize setting name from .enabled to .show for consistency.
        createSettingIfNotExists("portfolio.contact.show", "true", "Show or hide the contact form on the public portfolio.");
        createSettingIfNotExists("portfolio.testimonials.show", "true", "Show or hide the testimonials section.");
        createSettingIfNotExists("portfolio.projects.show", "true", "Show or hide the projects section.");
        createSettingIfNotExists("portfolio.experience.show", "true", "Show or hide the experience section.");
        createSettingIfNotExists("portfolio.qualifications.show", "true", "Show or hide the qualifications section.");
        createSettingIfNotExists("portfolio.skills.show", "true", "Show or hide the skills section.");
        // Revert default PDF template to 'modern'.
        createSettingIfNotExists("DEFAULT_PDF_TEMPLATE", "modern", "The default PDF template for the application used when visitors download from the homepage. Admin can change this.");
        // Legacy setting name kept for backward compatibility with user settings.
        createSettingIfNotExists("portfolio.pdf.template", "modern", "The default PDF template for generated resumes. Users can override this in their personal settings.");
        // Phase 8.3: per-user export preferences (mode + section toggles).
        // Seeded as global defaults so GET/PUT /api/v1/me/settings exposes
        // them with safe values; users override them via UserSetting rows.
        createSettingIfNotExists("export.mode", "FULL", "Export mode for your own downloads: FULL (complete portfolio) or BRIEF (concise resume).");
        createSettingIfNotExists("export.section.summary", "true", "Include the Summary / About section in your own exports.");
        createSettingIfNotExists("export.section.experience", "true", "Include the Experience section in your own exports.");
        createSettingIfNotExists("export.section.projects", "true", "Include the Projects section in your own exports.");
        createSettingIfNotExists("export.section.skills", "true", "Include the Skills section in your own exports.");
        createSettingIfNotExists("export.section.qualifications", "true", "Include the Qualifications / Education section in your own exports.");
        createSettingIfNotExists("export.section.testimonials", "true", "Include the Testimonials section in your own exports.");
        createSettingIfNotExists("export.section.contact", "true", "Include the contact details block in your own exports.");
        log.info("Default settings initialization complete.");
    }

    private void createSettingIfNotExists(String name, String value, String description) {
        // Use the correct findByName method from the repository
        if (settingRepository.findByName(name).isEmpty()) {
            Setting setting = new Setting(name, value, description);
            settingRepository.save(setting);
            log.debug("Created default setting: {}", name);
        }
    }
}