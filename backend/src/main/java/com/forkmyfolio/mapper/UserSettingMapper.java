package com.forkmyfolio.mapper;

import com.forkmyfolio.dto.response.UserSettingDto;
import com.forkmyfolio.model.Setting;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper for aggregating global settings and user overrides into
 * UserSettingDto.
 * 
 * Adheres to NAG-01 (DBR): Centralizes the transformation logic for settings.
 */
@Component
public class UserSettingMapper {

    /**
     * Maps a global setting and a user-specific value to a DTO.
     *
     * @param globalSetting  The base setting definition.
     * @param effectiveValue The value to display (either global or user-override).
     * @return A UserSettingDto.
     */
    public UserSettingDto toDto(Setting globalSetting, String effectiveValue) {
        if (globalSetting == null) {
            return null;
        }

        return new UserSettingDto(
                globalSetting.getUuid(),
                globalSetting.getName(),
                effectiveValue,
                globalSetting.getDescription());
    }

    /**
     * Aggregates global settings and user overrides into a list of DTOs.
     *
     * @param allGlobalSettings List of all global setting definitions.
     * @param userOverrides     Map of user-specific overrides (name -> value).
     * @return A list of populated UserSettingDtos.
     */
    public List<UserSettingDto> toDtoList(List<Setting> allGlobalSettings, Map<String, String> userOverrides) {
        return allGlobalSettings.stream()
                .map(globalSetting -> {
                    String effectiveValue = userOverrides.getOrDefault(globalSetting.getName(),
                            globalSetting.getValue());
                    return toDto(globalSetting, effectiveValue);
                })
                .collect(Collectors.toList());
    }
}
