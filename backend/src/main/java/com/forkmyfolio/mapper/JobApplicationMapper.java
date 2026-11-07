package com.forkmyfolio.mapper;

import com.forkmyfolio.dto.create.CreateJobApplicationRequest;
import com.forkmyfolio.dto.response.JobApplicationDto;
import com.forkmyfolio.dto.response.JobApplicationNoteDto;
import com.forkmyfolio.dto.update.UpdateJobApplicationRequest;
import com.forkmyfolio.model.JobApplication;
import com.forkmyfolio.model.JobApplicationNote;
import com.forkmyfolio.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper class responsible for converting between JobApplication domain models
 * and DTOs.
 */
@Component
@RequiredArgsConstructor
public class JobApplicationMapper {

    /**
     * Converts a JobApplication entity to a DTO.
     */
    public JobApplicationDto toDto(JobApplication application) {
        if (application == null) {
            return null;
        }

        JobApplicationDto dto = new JobApplicationDto();
        dto.setUuid(application.getUuid());
        dto.setJobTitle(application.getJobTitle());
        dto.setCompanyName(application.getCompanyName());
        dto.setJobLocation(application.getJobLocation());
        dto.setJobType(application.getJobType());
        dto.setApplicationUrl(application.getApplicationUrl());
        dto.setResumeVersion(application.getResumeVersion());
        // Phase 8.4: extended data model
        dto.setJobDescription(application.getJobDescription());
        dto.setWorkArrangement(application.getWorkArrangement());
        dto.setDiscoveredAt(application.getDiscoveredAt());
        dto.setDeadlineAt(application.getDeadlineAt());
        dto.setFollowUpAt(application.getFollowUpAt());
        dto.setInterviewAt(application.getInterviewAt());
        dto.setAssessmentAt(application.getAssessmentAt());
        dto.setRecruiterName(application.getRecruiterName());
        dto.setRecruiterEmail(application.getRecruiterEmail());
        dto.setContactDetails(application.getContactDetails());
        dto.setSalaryRange(application.getSalaryRange());
        dto.setSalaryCurrency(application.getSalaryCurrency());
        dto.setSalaryPeriod(application.getSalaryPeriod());
        dto.setStatus(application.getStatus());
        dto.setStatusUpdatedAt(application.getStatusUpdatedAt());
        dto.setAppliedAt(application.getAppliedAt());
        dto.setVisible(application.isVisible());
        dto.setCreatedAt(application.getCreatedAt());
        dto.setUpdatedAt(application.getUpdatedAt());

        if (application.getNotes() != null) {
            dto.setNotes(application.getNotes().stream()
                    .map(this::toNoteDto)
                    .collect(Collectors.toList()));
        } else {
            dto.setNotes(Collections.emptyList());
        }

        return dto;
    }

    /**
     * Converts a JobApplicationNote entity to a DTO.
     */
    public JobApplicationNoteDto toNoteDto(JobApplicationNote note) {
        if (note == null) {
            return null;
        }
        JobApplicationNoteDto dto = new JobApplicationNoteDto();
        dto.setUuid(note.getUuid());
        dto.setContent(note.getContent());
        dto.setCreatedAt(note.getCreatedAt());
        return dto;
    }

    /**
     * Maps a list of entities to a list of DTOs.
     */
    public List<JobApplicationDto> toDtoList(List<JobApplication> applications) {
        if (applications == null) {
            return Collections.emptyList();
        }
        return applications.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Converts a CreateJobApplicationRequest DTO to a transient JobApplication
     * entity.
     */
    public JobApplication toEntity(CreateJobApplicationRequest request, User owner) {
        if (request == null) {
            return null;
        }

        JobApplication application = new JobApplication();
        application.setJobTitle(request.getJobTitle());
        application.setCompanyName(request.getCompanyName());
        application.setJobLocation(request.getJobLocation());
        application.setJobType(request.getJobType());
        application.setApplicationUrl(request.getApplicationUrl());
        application.setResumeVersion(request.getResumeVersion());
        // Phase 8.4: extended data model
        application.setJobDescription(request.getJobDescription());
        application.setWorkArrangement(request.getWorkArrangement());
        application.setDiscoveredAt(request.getDiscoveredAt());
        application.setDeadlineAt(request.getDeadlineAt());
        application.setFollowUpAt(request.getFollowUpAt());
        application.setInterviewAt(request.getInterviewAt());
        application.setAssessmentAt(request.getAssessmentAt());
        application.setRecruiterName(request.getRecruiterName());
        application.setRecruiterEmail(request.getRecruiterEmail());
        application.setContactDetails(request.getContactDetails());
        application.setSalaryRange(request.getSalaryRange());
        application.setSalaryCurrency(request.getSalaryCurrency());
        application.setSalaryPeriod(request.getSalaryPeriod());
        application.setOwner(owner);

        // Notes are handled separately in the service if an initial note is provided

        return application;
    }

    /**
     * Maps an update request to a transient entity for partial updates in the
     * service.
     *
     * <p>
     * Phase 8.4: {@code visible} is deliberately NOT mapped here. The entity
     * field is a primitive {@code boolean} whose default (false) cannot be
     * distinguished from "explicitly false", so mapping it here silently hid
     * applications whenever a PUT omitted the field (the Phase 8.4 defect).
     * The nullable {@link UpdateJobApplicationRequest#getVisible()} is passed
     * to the service as an explicit parameter instead.
     */
    public JobApplication toEntity(UpdateJobApplicationRequest request) {
        if (request == null) {
            return null;
        }
        JobApplication application = new JobApplication();
        application.setJobTitle(request.getJobTitle());
        application.setCompanyName(request.getCompanyName());
        application.setJobLocation(request.getJobLocation());
        application.setJobType(request.getJobType());
        application.setApplicationUrl(request.getApplicationUrl());
        application.setResumeVersion(request.getResumeVersion());
        // Phase 8.4: extended data model
        application.setJobDescription(request.getJobDescription());
        application.setWorkArrangement(request.getWorkArrangement());
        application.setDiscoveredAt(request.getDiscoveredAt());
        application.setDeadlineAt(request.getDeadlineAt());
        application.setFollowUpAt(request.getFollowUpAt());
        application.setInterviewAt(request.getInterviewAt());
        application.setAssessmentAt(request.getAssessmentAt());
        application.setRecruiterName(request.getRecruiterName());
        application.setRecruiterEmail(request.getRecruiterEmail());
        application.setContactDetails(request.getContactDetails());
        application.setSalaryRange(request.getSalaryRange());
        application.setSalaryCurrency(request.getSalaryCurrency());
        application.setSalaryPeriod(request.getSalaryPeriod());
        application.setStatus(request.getStatus());
        application.setAppliedAt(request.getAppliedAt());
        return application;
    }
}
