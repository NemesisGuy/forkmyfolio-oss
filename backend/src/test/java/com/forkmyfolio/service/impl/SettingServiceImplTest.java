package com.forkmyfolio.service.impl;

import com.forkmyfolio.model.Setting;
import com.forkmyfolio.repository.SettingRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingServiceImplTest {

    @Mock
    private SettingRepository settingRepository;

    @InjectMocks
    private SettingServiceImpl settingService;

    @Test
    void getAllSettings_shouldReturnAllSettings() {
        List<Setting> expectedSettings = Collections.singletonList(new Setting());
        when(settingRepository.findAll()).thenReturn(expectedSettings);

        List<Setting> actualSettings = settingService.getAllSettings();

        assertEquals(expectedSettings, actualSettings);
        verify(settingRepository, times(1)).findAll();
    }

    @Test
    void updateSettings_whenAllSettingsExist_shouldUpdateAndReturnThem() {
        UUID settingId = UUID.randomUUID();
        Map<UUID, String> settingsToUpdate = Map.of(settingId, "new-value");

        Setting settingToModify = new Setting("test.setting", "old-value", "description");
        settingToModify.setUuid(settingId);

        when(settingRepository.findByUuidIn(anyList())).thenReturn(Collections.singletonList(settingToModify));
        when(settingRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Setting> updatedSettings = settingService.updateSettings(settingsToUpdate);

        assertNotNull(updatedSettings);
        assertEquals(1, updatedSettings.size());
        assertEquals("new-value", updatedSettings.get(0).getValue());
        verify(settingRepository, times(1)).saveAll(anyList());
    }

    @Test
    void updateSettings_whenSettingNotFound_shouldThrowException() {
        UUID settingId = UUID.randomUUID();
        Map<UUID, String> settingsToUpdate = Map.of(settingId, "new-value");

        when(settingRepository.findByUuidIn(anyList())).thenReturn(Collections.emptyList());

        assertThrows(EntityNotFoundException.class, () -> {
            settingService.updateSettings(settingsToUpdate);
        });

        verify(settingRepository, never()).saveAll(anyList());
    }

    @Test
    void getPublicSettings_shouldReturnOnlyBooleanSettings() {
        Setting boolSettingTrue = new Setting("show.contact", "true", "");
        Setting boolSettingFalse = new Setting("show.projects", "false", "");
        Setting nonBoolSetting = new Setting("theme", "dark", "");

        when(settingRepository.findAll()).thenReturn(Arrays.asList(boolSettingTrue, boolSettingFalse, nonBoolSetting));

        Map<String, Boolean> publicSettings = settingService.getPublicSettings();

        assertEquals(2, publicSettings.size());
        assertTrue(publicSettings.get("show.contact"));
        assertFalse(publicSettings.get("show.projects"));
        assertNull(publicSettings.get("theme"));
    }

    @Test
    void createDefaultSettings_whenSettingExists_shouldNotCreateIt() {
        String settingName = "portfolio.theme";
        when(settingRepository.findByName(settingName)).thenReturn(Optional.of(new Setting()));

        settingService.createDefaultSettings();

        // Verify save is not called for the existing setting
        verify(settingRepository, never()).save(argThat(s -> s.getName().equals(settingName)));
    }

    @Test
    void createDefaultSettings_whenSettingDoesNotExist_shouldCreateIt() {
        String settingName = "portfolio.theme";
        when(settingRepository.findByName(anyString())).thenReturn(Optional.empty());

        settingService.createDefaultSettings();

        // Verify save is called for the new setting
        verify(settingRepository, times(1)).save(argThat(s -> s.getName().equals(settingName)));
        // Verify it's called for all default settings
        verify(settingRepository, atLeast(7)).save(any(Setting.class));
    }
}
