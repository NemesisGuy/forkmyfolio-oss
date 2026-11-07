package com.forkmyfolio.mapper;

import com.forkmyfolio.dto.backup.BackupMetaDto;
import com.forkmyfolio.dto.backup.UserBackupDto;
import com.forkmyfolio.dto.response.*;
import com.forkmyfolio.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Backup/Restore DTOs and their domain-level
 * containers.
 * This ensures the service layer remains DTO-free (Rule 2: DBR).
 */
@Component
@RequiredArgsConstructor
public class BackupMapper {

    private final UserMapper userMapper;
    private final PortfolioProfileMapper portfolioProfileMapper;
    private final ProjectMapper projectMapper;
    private final UserSkillMapper userSkillMapper;
    private final ExperienceMapper experienceMapper;
    private final TestimonialMapper testimonialMapper;
    private final QualificationMapper qualificationMapper;

    /**
     * Converts a PortfolioBackupDto to a PortfolioBackup domain container.
     */
    public PortfolioBackup toDomain(PortfolioBackupDto dto, User user) {
        if (dto == null) {
            return null;
        }

        PortfolioBackup domain = new PortfolioBackup();

        if (dto.getProfile() != null) {
            domain.setProfile(portfolioProfileMapper.toEntityFromDto(dto.getProfile(), user));
        }

        if (dto.getSkills() != null) {
            domain.setSkills(dto.getSkills().stream()
                    .map(userSkillMapper::toEntity)
                    .collect(Collectors.toList()));
        }

        if (dto.getProjects() != null) {
            domain.setProjects(dto.getProjects().stream()
                    .map(pDto -> projectMapper.toEntityFromDto(pDto, user))
                    .collect(Collectors.toList()));
        }

        if (dto.getExperiences() != null) {
            domain.setExperiences(dto.getExperiences().stream()
                    .map(eDto -> experienceMapper.toEntityFromDto(eDto, user))
                    .collect(Collectors.toList()));
        }

        if (dto.getQualifications() != null) {
            domain.setQualifications(dto.getQualifications().stream()
                    .map(qDto -> qualificationMapper.toEntityFromDto(qDto, user))
                    .collect(Collectors.toList()));
        }

        if (dto.getTestimonials() != null) {
            domain.setTestimonials(dto.getTestimonials().stream()
                    .map(tDto -> testimonialMapper.toEntityFromDto(tDto, user))
                    .collect(Collectors.toList()));
        }

        // Phase 10 (Workstream 4): per-user setting overrides. The owning user
        // is attached later by the restore service (it owns the lifecycle).
        // Only name/value are meaningful here; uuid/description stay null.
        if (dto.getSettings() != null) {
            domain.setSettings(dto.getSettings().stream()
                    .filter(Objects::nonNull)
                    .filter(sDto -> sDto.getName() != null && !sDto.getName().isBlank())
                    .map(sDto -> {
                        UserSetting setting = new UserSetting();
                        setting.setName(sDto.getName());
                        setting.setValue(sDto.getValue());
                        return setting;
                    })
                    .collect(Collectors.toList()));
        }

        return domain;
    }

    /**
     * Converts a PortfolioBackup domain container to a PortfolioBackupDto.
     */
    public PortfolioBackupDto toDto(PortfolioBackup domain) {
        if (domain == null) {
            return null;
        }

        PortfolioBackupDto dto = new PortfolioBackupDto();

        // Create the lookup map that several mappers need for building complete DTOs
        Map<UUID, UserSkill> userSkillLookup = Optional.ofNullable(domain.getSkills())
                .orElse(Collections.emptyList())
                .stream()
                .filter(us -> us.getSkill() != null && us.getSkill().getUuid() != null)
                .collect(Collectors.toMap(
                        us -> us.getSkill().getUuid(),
                        us -> us,
                        (existing, replacement) -> existing));

        if (domain.getProfile() != null) {
            dto.setProfile(portfolioProfileMapper.toDto(domain.getProfile()));
        }

        if (domain.getProjects() != null) {
            dto.setProjects(domain.getProjects().stream()
                    .map(p -> projectMapper.toDto(p, userSkillLookup))
                    .collect(Collectors.toList()));
        }

        if (domain.getSkills() != null) {
            dto.setSkills(userSkillMapper.toDtoList(domain.getSkills()));
        }

        if (domain.getExperiences() != null) {
            dto.setExperiences(domain.getExperiences().stream()
                    .map(e -> experienceMapper.toDto(e, userSkillLookup))
                    .collect(Collectors.toList()));
        }

        if (domain.getTestimonials() != null) {
            dto.setTestimonials(domain.getTestimonials().stream()
                    .map(testimonialMapper::toDto)
                    .collect(Collectors.toList()));
        }

        if (domain.getQualifications() != null) {
            dto.setQualifications(domain.getQualifications().stream()
                    .map(qualificationMapper::toDto)
                    .collect(Collectors.toList()));
        }

        // Phase 10 (Workstream 4): per-user setting overrides.
        if (domain.getSettings() != null) {
            dto.setSettings(domain.getSettings().stream()
                    .filter(Objects::nonNull)
                    .map(setting -> new UserSettingDto(null, setting.getName(), setting.getValue(), null))
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    /**
     * Converts a UserFullBackupDto to a UserFullBackup domain container.
     */
    public UserFullBackup toDomain(UserFullBackupDto dto) {
        if (dto == null) {
            return null;
        }
        User user = userMapper.toEntityFromBackup(dto.getUser());
        PortfolioBackup portfolio = toDomain(dto.getPortfolio(), user);
        return new UserFullBackup(user, portfolio);
    }

    /**
     * Converts a list of UserFullBackupDto to a list of UserFullBackup domain
     * containers.
     */
    public List<UserFullBackup> toDomainList(List<UserFullBackupDto> dtoList) {
        if (dtoList == null) {
            return Collections.emptyList();
        }
        return dtoList.stream().map(this::toDomain).collect(Collectors.toList());
    }

    /**
     * Converts a UserFullBackup domain container to a UserFullBackupDto.
     */
    public UserFullBackupDto toDto(UserFullBackup domain) {
        if (domain == null) {
            return null;
        }
        return new UserFullBackupDto(
                userMapper.toBackupDto(domain.getUser()),
                toDto(domain.getPortfolio()));
    }

    /**
     * Converts a list of UserFullBackup domain containers to a list of
     * UserFullBackupDto.
     */
    public List<UserFullBackupDto> toDtoList(List<UserFullBackup> domainList) {
        if (domainList == null) {
            return Collections.emptyList();
        }
        return domainList.stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Converts a backup metadata DTO to a domain object.
     */
    public BackupMetadata toDomain(BackupMetaDto dto) {
        if (dto == null) {
            return null;
        }

        BackupMetadata.Compatibility compatibility = null;
        if (dto.getCompatibility() != null) {
            compatibility = BackupMetadata.Compatibility.builder()
                    .minSupportedVersion(dto.getCompatibility().getMinSupportedVersion())
                    .maxSupportedVersion(dto.getCompatibility().getMaxSupportedVersion())
                    .build();
        }

        return BackupMetadata.builder()
                .version(dto.getVersion())
                .exportedAt(dto.getExportedAt())
                .type(dto.getType())
                .compatibility(compatibility)
                .exportedBy(dto.getExportedBy())
                .system(dto.getSystem())
                .build();
    }

    /**
     * Converts a backup metadata domain object to a DTO.
     */
    public BackupMetaDto toDto(BackupMetadata domain) {
        if (domain == null) {
            return null;
        }

        BackupMetaDto.Compatibility compatibility = null;
        if (domain.getCompatibility() != null) {
            compatibility = BackupMetaDto.Compatibility.builder()
                    .minSupportedVersion(domain.getCompatibility().getMinSupportedVersion())
                    .maxSupportedVersion(domain.getCompatibility().getMaxSupportedVersion())
                    .build();
        }

        return BackupMetaDto.builder()
                .version(domain.getVersion())
                .exportedAt(domain.getExportedAt())
                .type(domain.getType())
                .compatibility(compatibility)
                .exportedBy(domain.getExportedBy())
                .system(domain.getSystem())
                .build();
    }
}
