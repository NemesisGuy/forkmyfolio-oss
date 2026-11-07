package com.forkmyfolio.mapper;

import com.forkmyfolio.dto.response.SettingDto;
import com.forkmyfolio.model.Setting;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SettingMapper {

    public SettingDto toDto(Setting entity) {
        return new SettingDto(entity.getUuid(), entity.getName(), entity.getValue(), entity.getDescription());
    }

    public List<SettingDto> toDtoList(List<Setting> entities) {
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }
}