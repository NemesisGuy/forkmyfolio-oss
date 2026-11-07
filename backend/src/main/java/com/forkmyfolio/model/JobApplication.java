package com.forkmyfolio.model;

import com.forkmyfolio.model.enums.ApplicationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a job application tracked by a user.
 */
@Entity
@Table(name = "job_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "uuid")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "uuid", nullable = false, updatable = false, unique = true)
    private UUID uuid;

    @NotBlank(message = "Job title cannot be blank")
    @Size(min = 2, max = 150, message = "Job title must be between 2 and 150 characters")
    @Column(nullable = false)
    private String jobTitle;

    @NotBlank(message = "Company name cannot be blank")
    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    @Column(nullable = false)
    private String companyName;

    @Size(max = 100)
    private String jobLocation;

    @Size(max = 50)
    private String jobType; // e.g., Full-Time, Contract, Internship

    @URL(message = "Application URL must be a valid URL")
    @Size(max = 2048)
    private String applicationUrl;

    @Size(max = 100)
    private String resumeVersion;

    // --- Phase 8.4: extended data model (all optional) ---

    /**
     * The full job description / posting text, for reference after the
     * posting disappears.
     */
    @Column(columnDefinition = "TEXT")
    private String jobDescription;

    /**
     * Work arrangement: REMOTE, HYBRID or ON_SITE (free string so legacy
     * data and future values never break reads; the UI offers the three
     * standard options).
     */
    @Size(max = 20)
    private String workArrangement;

    /** When the user discovered the role. */
    private LocalDate discoveredAt;

    /** Application deadline, if the posting declares one. */
    private LocalDate deadlineAt;

    /** Next date the user plans to follow up. */
    private LocalDate followUpAt;

    /** The scheduled (or most recent) interview date/time. */
    private LocalDateTime interviewAt;

    /** Date of the technical assessment / take-home task. */
    private LocalDate assessmentAt;

    /** Recruiter or contact person's name. */
    @Size(max = 100)
    private String recruiterName;

    /** Recruiter or contact person's email. */
    @Size(max = 150)
    @Email(message = "Recruiter email must be a valid email address")
    private String recruiterEmail;

    /** Free-form contact details (phone, portal handle, ...). */
    @Size(max = 255)
    private String contactDetails;

    /** Salary / remuneration expectation (free text, e.g. "R45k - R55k"). */
    @Size(max = 100)
    private String salaryRange;

    /** ISO currency code or symbol for the salary expectation. */
    @Size(max = 8)
    private String salaryCurrency;

    /** Salary period: HOURLY, MONTHLY, ANNUALLY, ... */
    @Size(max = 20)
    private String salaryPeriod;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(nullable = false)
    private LocalDateTime statusUpdatedAt;

    private LocalDateTime appliedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "jobApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<JobApplicationNote> notes = new ArrayList<>();

    @Column(nullable = false)
    private boolean visible = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (status == null) {
            status = ApplicationStatus.DRAFT;
        }
        if (statusUpdatedAt == null) {
            statusUpdatedAt = LocalDateTime.now();
        }
    }

    /**
     * Helper method to add a note to this application.
     */
    public void addNote(JobApplicationNote note) {
        notes.add(note);
        note.setJobApplication(this);
    }

    /**
     * Helper method to remove a note from this application.
     */
    public void removeNote(JobApplicationNote note) {
        notes.remove(note);
        note.setJobApplication(null);
    }
}
