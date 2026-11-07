package com.forkmyfolio.mapper;

import com.forkmyfolio.dto.create.CreateQualificationRequest;
import com.forkmyfolio.dto.response.QualificationDto;
import com.forkmyfolio.dto.update.UpdateQualificationRequest;
import com.forkmyfolio.model.Qualification;
import com.forkmyfolio.model.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper class responsible for converting between Qualification domain models and DTOs.
 */
@Component
public class QualificationMapper {

    /**
     * Converts a Qualification entity to a QualificationDto.
     *
     * @param qualification The Qualification entity to convert.
     * @return The corresponding QualificationDto.
     */
    public QualificationDto toDto(Qualification qualification) {
        if (qualification == null) {
            return null;
        }
        QualificationDto dto = new QualificationDto();
        dto.setUuid(qualification.getUuid());
        dto.setQualificationName(qualification.getQualificationName());
        dto.setInstitutionName(qualification.getInstitutionName());
        dto.setInstitutionLogoUrl(qualification.getInstitutionLogoUrl());
        dto.setInstitutionWebsite(qualification.getInstitutionWebsite());
        dto.setFieldOfStudy(qualification.getFieldOfStudy());
        dto.setLevel(qualification.getLevel());
        dto.setStartYear(qualification.getStartYear());
        dto.setCompletionYear(qualification.getCompletionYear());
        dto.setStillStudying(qualification.getStillStudying());
        dto.setGrade(qualification.getGrade());
        dto.setCredentialUrl(qualification.getCredentialUrl());
        dto.setVisible(qualification.isVisible());
        dto.setCreatedAt(qualification.getCreatedAt());
        dto.setUpdatedAt(qualification.getUpdatedAt());
        return dto;
    }

    /**
     * Converts a list of Qualification entities to a list of QualificationDtos.
     *
     * @param qualifications The list of Qualification entities.
     * @return A list of corresponding QualificationDtos.
     */
    public List<QualificationDto> toDtoList(List<Qualification> qualifications) {
        return qualifications.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Converts a CreateQualificationRequest DTO to a new Qualification entity.
     *
     * @param request The DTO containing the creation data.
     * @return A new Qualification entity, ready to be persisted.
     */
    public Qualification toEntity(CreateQualificationRequest request) {
        if (request == null) {
            return null;
        }
        Qualification qualification = new Qualification();
        qualification.setQualificationName(request.getQualificationName());
        qualification.setInstitutionName(request.getInstitutionName());
        qualification.setInstitutionLogoUrl(request.getInstitutionLogoUrl());
        qualification.setInstitutionWebsite(request.getInstitutionWebsite());
        qualification.setFieldOfStudy(request.getFieldOfStudy());
        qualification.setLevel(request.getLevel());
        qualification.setStartYear(request.getStartYear());
        qualification.setCompletionYear(request.getCompletionYear());
        qualification.setStillStudying(request.getStillStudying());
        qualification.setGrade(request.getGrade());
        qualification.setCredentialUrl(request.getCredentialUrl());
        qualification.setVisible(request.isVisible());
        return qualification;
    }

    /**
     * Converts a QualificationDto (typically from a backup) to a new Qualification entity.
     *
     * @param dto  The DTO containing the qualification data.
     * @param user The user who will own this qualification.
     * @return A new Qualification entity, ready to be persisted.
     */
    public Qualification toEntityFromDto(QualificationDto dto, User user) {
        if (dto == null) {
            return null;
        }
        Qualification qualification = new Qualification();
        qualification.setUser(user);
        qualification.setQualificationName(dto.getQualificationName());
        qualification.setInstitutionName(dto.getInstitutionName());
        qualification.setInstitutionLogoUrl(dto.getInstitutionLogoUrl());
        qualification.setInstitutionWebsite(dto.getInstitutionWebsite());
        qualification.setFieldOfStudy(dto.getFieldOfStudy());
        qualification.setLevel(dto.getLevel());
        qualification.setStartYear(dto.getStartYear());
        qualification.setCompletionYear(dto.getCompletionYear());
        qualification.setStillStudying(dto.getStillStudying());
        qualification.setGrade(dto.getGrade());
        qualification.setCredentialUrl(dto.getCredentialUrl());
        qualification.setVisible(dto.isVisible());
        return qualification;
    }

    /**
     * Applies updates from an UpdateQualificationRequest DTO to an existing Qualification entity.
     *
     * @param request       The DTO containing the update information.
     * @param qualification The Qualification entity to be updated.
     */
    public void updateEntityFromDto(UpdateQualificationRequest request, Qualification qualification) {
        if (request == null || qualification == null) {
            return;
        }
        qualification.setQualificationName(request.getQualificationName());
        qualification.setInstitutionName(request.getInstitutionName());
        qualification.setInstitutionLogoUrl(request.getInstitutionLogoUrl());
        qualification.setInstitutionWebsite(request.getInstitutionWebsite());
        qualification.setFieldOfStudy(request.getFieldOfStudy());
        qualification.setLevel(request.getLevel());
        qualification.setStartYear(request.getStartYear());
        qualification.setCompletionYear(request.getCompletionYear());
        qualification.setStillStudying(request.getStillStudying());
        qualification.setGrade(request.getGrade());
        qualification.setCredentialUrl(request.getCredentialUrl());
        qualification.setVisible(request.getVisible());
    }
}