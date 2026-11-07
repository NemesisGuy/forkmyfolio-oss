package com.forkmyfolio.service.impl;

import com.forkmyfolio.exception.ResourceNotFoundException;
import com.forkmyfolio.model.JobApplication;
import com.forkmyfolio.model.JobApplicationNote;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.ApplicationStatus;
import com.forkmyfolio.repository.JobApplicationNoteRepository;
import com.forkmyfolio.repository.JobApplicationRepository;
import com.forkmyfolio.service.JobApplicationService;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobApplicationServiceImpl implements JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobApplicationNoteRepository jobApplicationNoteRepository;

    @Override
    @Transactional(readOnly = true)
    public List<JobApplication> getAllApplicationsForUser(User owner) {
        List<JobApplication> applications = jobApplicationRepository.findByOwnerOrderByStatusUpdatedAtDesc(owner);
        // Initialize notes to avoid lazy loading issues in the mapper
        applications.forEach(app -> Hibernate.initialize(app.getNotes()));
        return applications;
    }

    @Override
    @Transactional(readOnly = true)
    public JobApplication getApplicationByUuid(UUID uuid, User owner) {
        JobApplication application = jobApplicationRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Job application not found with UUID: " + uuid));

        verifyOwnership(application, owner);
        Hibernate.initialize(application.getNotes());
        return application;
    }

    @Override
    @Transactional
    public JobApplication createApplication(JobApplication application, String initialNote, User owner) {
        application.setOwner(owner);
        application.setStatus(ApplicationStatus.DRAFT); // Default if not set
        application.setStatusUpdatedAt(LocalDateTime.now());

        JobApplication savedApplication = jobApplicationRepository.save(application);

        if (StringUtils.hasText(initialNote)) {
            JobApplicationNote note = JobApplicationNote.builder()
                    .content(initialNote)
                    .jobApplication(savedApplication)
                    .build();
            jobApplicationNoteRepository.save(note);
            savedApplication.getNotes().add(note);
        }

        return savedApplication;
    }

    @Override
    @Transactional
    public JobApplication updateApplication(UUID uuid, JobApplication updates, Boolean visible, User owner) {
        JobApplication existing = getApplicationByUuid(uuid, owner);

        if (updates.getJobTitle() != null)
            existing.setJobTitle(updates.getJobTitle());
        if (updates.getCompanyName() != null)
            existing.setCompanyName(updates.getCompanyName());
        if (updates.getJobLocation() != null)
            existing.setJobLocation(updates.getJobLocation());
        if (updates.getJobType() != null)
            existing.setJobType(updates.getJobType());
        if (updates.getApplicationUrl() != null)
            existing.setApplicationUrl(updates.getApplicationUrl());
        if (updates.getResumeVersion() != null)
            existing.setResumeVersion(updates.getResumeVersion());

        // Phase 8.4: extended data model (partial update semantics — a null
        // field means "no change", mirroring the pre-8.4 behaviour of the
        // original fields).
        if (updates.getJobDescription() != null)
            existing.setJobDescription(updates.getJobDescription());
        if (updates.getWorkArrangement() != null)
            existing.setWorkArrangement(updates.getWorkArrangement());
        if (updates.getDiscoveredAt() != null)
            existing.setDiscoveredAt(updates.getDiscoveredAt());
        if (updates.getDeadlineAt() != null)
            existing.setDeadlineAt(updates.getDeadlineAt());
        if (updates.getFollowUpAt() != null)
            existing.setFollowUpAt(updates.getFollowUpAt());
        if (updates.getInterviewAt() != null)
            existing.setInterviewAt(updates.getInterviewAt());
        if (updates.getAssessmentAt() != null)
            existing.setAssessmentAt(updates.getAssessmentAt());
        if (updates.getRecruiterName() != null)
            existing.setRecruiterName(updates.getRecruiterName());
        if (updates.getRecruiterEmail() != null)
            existing.setRecruiterEmail(updates.getRecruiterEmail());
        if (updates.getContactDetails() != null)
            existing.setContactDetails(updates.getContactDetails());
        if (updates.getSalaryRange() != null)
            existing.setSalaryRange(updates.getSalaryRange());
        if (updates.getSalaryCurrency() != null)
            existing.setSalaryCurrency(updates.getSalaryCurrency());
        if (updates.getSalaryPeriod() != null)
            existing.setSalaryPeriod(updates.getSalaryPeriod());

        // Handle status change
        if (updates.getStatus() != null && updates.getStatus() != existing.getStatus()) {
            existing.setStatus(updates.getStatus());
            existing.setStatusUpdatedAt(LocalDateTime.now());
        }

        if (updates.getAppliedAt() != null) {
            existing.setAppliedAt(updates.getAppliedAt());
        }

        // Phase 8.4 visibility-preservation fix: the update DTO carries a
        // NULLABLE Boolean, but the transient entity's primitive boolean
        // cannot distinguish "omitted" from "false" — the previous
        // `existing.setVisible(updates.isVisible())` silently hid the
        // application on every PUT that omitted the field. Apply the value
        // only when the caller explicitly supplied it.
        if (visible != null) {
            existing.setVisible(visible);
        }

        return jobApplicationRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteApplication(UUID uuid, User owner) {
        JobApplication application = getApplicationByUuid(uuid, owner);
        jobApplicationRepository.delete(application);
    }

    @Override
    @Transactional
    public JobApplicationNote addNote(UUID applicationUuid, String content, User owner) {
        JobApplication application = getApplicationByUuid(applicationUuid, owner);

        JobApplicationNote note = JobApplicationNote.builder()
                .content(content)
                .jobApplication(application)
                .build();

        JobApplicationNote savedNote = jobApplicationNoteRepository.save(note);
        application.getNotes().add(savedNote);
        return savedNote;
    }

    @Override
    @Transactional
    public void deleteNote(UUID applicationUuid, UUID noteUuid, User owner) {
        JobApplication application = getApplicationByUuid(applicationUuid, owner);

        JobApplicationNote note = jobApplicationNoteRepository.findByUuid(noteUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found with UUID: " + noteUuid));

        if (!note.getJobApplication().getId().equals(application.getId())) {
            // Phase 7.4 (403/404 policy): a note hanging under the wrong
            // application is concealed as 404 instead of 403.
            throw new ResourceNotFoundException("Note not found with UUID: " + noteUuid);
        }

        jobApplicationNoteRepository.delete(note);
    }

    private void verifyOwnership(JobApplication application, User owner) {
        if (!application.getOwner().getId().equals(owner.getId())) {
            // Phase 7.4 (403/404 policy): cross-tenant access is concealed as
            // 404 - never disclose that another user's application exists.
            throw new ResourceNotFoundException(
                    "Job application not found with UUID: " + application.getUuid());
        }
    }
}
