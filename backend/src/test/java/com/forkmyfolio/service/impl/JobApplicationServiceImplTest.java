package com.forkmyfolio.service.impl;

import com.forkmyfolio.exception.ResourceNotFoundException;
import com.forkmyfolio.model.JobApplication;
import com.forkmyfolio.model.JobApplicationNote;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.ApplicationStatus;
import com.forkmyfolio.repository.JobApplicationNoteRepository;
import com.forkmyfolio.repository.JobApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceImplTest {

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private JobApplicationNoteRepository jobApplicationNoteRepository;

    @InjectMocks
    private JobApplicationServiceImpl jobApplicationService;

    private User currentUser;
    private JobApplication mockApplication;
    private UUID appUuid;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1L);

        appUuid = UUID.randomUUID();
        mockApplication = JobApplication.builder()
                .id(1L)
                .uuid(appUuid)
                .jobTitle("Dev")
                .companyName("TestCo")
                .owner(currentUser)
                .status(ApplicationStatus.APPLIED)
                .notes(new ArrayList<>())
                .build();
    }

    @Test
    void getApplicationByUuid_ShouldReturnApplication_WhenOwnedByUser() {
        when(jobApplicationRepository.findByUuid(appUuid)).thenReturn(Optional.of(mockApplication));

        JobApplication result = jobApplicationService.getApplicationByUuid(appUuid, currentUser);

        assertNotNull(result);
        assertEquals(appUuid, result.getUuid());
        verify(jobApplicationRepository).findByUuid(appUuid);
    }

    @Test
    void getApplicationByUuid_ShouldConcealForeignApplicationAsNotFound_WhenNotOwned() {
        // Phase 7.4 (403/404 policy): cross-tenant access is concealed as 404
        // (ResourceNotFound) — identical to the uuid-miss case, so existence
        // of another user's application is never disclosed through probing.
        User otherUser = new User();
        otherUser.setId(2L);

        when(jobApplicationRepository.findByUuid(appUuid)).thenReturn(Optional.of(mockApplication));

        assertThrows(ResourceNotFoundException.class,
                () -> jobApplicationService.getApplicationByUuid(appUuid, otherUser));
    }

    @Test
    void createApplication_ShouldSaveAndReturnApplication() {
        JobApplication newApp = new JobApplication();
        newApp.setJobTitle("New Job");

        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(i -> i.getArguments()[0]);

        JobApplication saved = jobApplicationService.createApplication(newApp, "Initial Note", currentUser);

        assertNotNull(saved);
        assertEquals(currentUser, saved.getOwner());
        assertEquals(ApplicationStatus.DRAFT, saved.getStatus());
        assertNotNull(saved.getStatusUpdatedAt());
        assertFalse(saved.getNotes().isEmpty());
        assertEquals("Initial Note", saved.getNotes().get(0).getContent());
    }

    @Test
    void updateApplication_ShouldUpdateFieldsAndStatus() {
        when(jobApplicationRepository.findByUuid(appUuid)).thenReturn(Optional.of(mockApplication));
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(i -> i.getArguments()[0]);

        JobApplication updates = new JobApplication();
        updates.setJobTitle("Updated Title");
        updates.setStatus(ApplicationStatus.INTERVIEWING);

        JobApplication result = jobApplicationService.updateApplication(appUuid, updates, null, currentUser);

        assertEquals("Updated Title", result.getJobTitle());
        assertEquals(ApplicationStatus.INTERVIEWING, result.getStatus());
        verify(jobApplicationRepository).save(mockApplication);
    }

    /**
     * Phase 8.4 visibility-preservation regression: a PUT that omits
     * {@code visible} (null) must NOT change the stored visibility — the
     * previous implementation unconditionally applied the transient entity's
     * primitive default (false) and silently hid the application.
     */
    @Test
    void updateApplication_omittedVisible_preservesStoredVisibility() {
        mockApplication.setVisible(true);
        when(jobApplicationRepository.findByUuid(appUuid)).thenReturn(Optional.of(mockApplication));
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(i -> i.getArguments()[0]);

        JobApplication updates = new JobApplication();
        updates.setJobTitle("Updated Title");

        JobApplication result = jobApplicationService.updateApplication(appUuid, updates, null, currentUser);

        assertEquals("Updated Title", result.getJobTitle());
        assertTrue(result.isVisible(), "omitted visible must preserve the stored true value");
    }

    @Test
    void updateApplication_explicitVisible_false_appliesFalse() {
        mockApplication.setVisible(true);
        when(jobApplicationRepository.findByUuid(appUuid)).thenReturn(Optional.of(mockApplication));
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(i -> i.getArguments()[0]);

        JobApplication updates = new JobApplication();

        JobApplication result = jobApplicationService.updateApplication(appUuid, updates, Boolean.FALSE, currentUser);

        assertFalse(result.isVisible(), "explicit false must be applied");
    }

    @Test
    void updateApplication_explicitVisible_true_appliesTrue() {
        mockApplication.setVisible(false);
        when(jobApplicationRepository.findByUuid(appUuid)).thenReturn(Optional.of(mockApplication));
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(i -> i.getArguments()[0]);

        JobApplication updates = new JobApplication();

        JobApplication result = jobApplicationService.updateApplication(appUuid, updates, Boolean.TRUE, currentUser);

        assertTrue(result.isVisible(), "explicit true must be applied");
    }

    @Test
    void addNote_ShouldCreateAndLinkNote() {
        when(jobApplicationRepository.findByUuid(appUuid)).thenReturn(Optional.of(mockApplication));
        when(jobApplicationNoteRepository.save(any(JobApplicationNote.class))).thenAnswer(i -> i.getArguments()[0]);

        JobApplicationNote note = jobApplicationService.addNote(appUuid, "Test Note", currentUser);

        assertNotNull(note);
        assertEquals("Test Note", note.getContent());
        assertEquals(mockApplication, note.getJobApplication());
        assertTrue(mockApplication.getNotes().contains(note));
    }

    @Test
    void deleteNote_ShouldRemoveNote_WhenOwned() {
        UUID noteUuid = UUID.randomUUID();
        JobApplicationNote note = new JobApplicationNote();
        note.setUuid(noteUuid);
        note.setJobApplication(mockApplication);

        when(jobApplicationRepository.findByUuid(appUuid)).thenReturn(Optional.of(mockApplication));
        when(jobApplicationNoteRepository.findByUuid(noteUuid)).thenReturn(Optional.of(note));

        jobApplicationService.deleteNote(appUuid, noteUuid, currentUser);

        verify(jobApplicationNoteRepository).delete(note);
    }
}
