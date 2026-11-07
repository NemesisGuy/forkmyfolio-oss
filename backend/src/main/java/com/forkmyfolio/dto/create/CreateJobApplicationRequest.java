package com.forkmyfolio.dto.create;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(name = "CreateJobApplicationRequest", description = "Data required to track a new job application.")
public class CreateJobApplicationRequest {

    @NotBlank(message = "Job title cannot be blank")
    @Size(min = 2, max = 150, message = "Job title must be between 2 and 150 characters")
    @Schema(description = "The title of the job position.", example = "Senior Software Engineer")
    private String jobTitle;

    @NotBlank(message = "Company name cannot be blank")
    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    @Schema(description = "The name of the company.", example = "NemesisNet")
    private String companyName;

    @Size(max = 100)
    @Schema(description = "The location of the job.", example = "Remote")
    private String jobLocation;

    @Size(max = 50)
    @Schema(description = "The type of employment.", example = "Full-Time")
    private String jobType;

    @URL(message = "Application URL must be a valid URL")
    @Size(max = 2048)
    @Schema(description = "URL to the job posting.", example = "https://linkedin.com/jobs/12345")
    private String applicationUrl;

    @Size(max = 100)
    @Schema(description = "Identifier for the resume version used.", example = "Standard-v1")
    private String resumeVersion;

    // --- Phase 8.4: extended data model (all optional) ---

    @Size(max = 100000)
    @Schema(description = "The full job description / posting text.")
    private String jobDescription;

    @Size(max = 20)
    @Schema(description = "Work arrangement: REMOTE, HYBRID or ON_SITE.", example = "REMOTE")
    private String workArrangement;

    @Schema(description = "When the role was discovered.")
    private LocalDate discoveredAt;

    @Schema(description = "Application deadline, if the posting declares one.")
    private LocalDate deadlineAt;

    @Schema(description = "Next planned follow-up date.")
    private LocalDate followUpAt;

    @Schema(description = "Scheduled (or most recent) interview date/time.")
    private LocalDateTime interviewAt;

    @Schema(description = "Date of the technical assessment / take-home task.")
    private LocalDate assessmentAt;

    @Size(max = 100)
    @Schema(description = "Recruiter or contact person's name.")
    private String recruiterName;

    @Email(message = "Recruiter email must be a valid email address")
    @Size(max = 150)
    @Schema(description = "Recruiter or contact person's email.")
    private String recruiterEmail;

    @Size(max = 255)
    @Schema(description = "Free-form contact details (phone, portal handle, ...).")
    private String contactDetails;

    @Size(max = 100)
    @Schema(description = "Salary / remuneration expectation.", example = "R45k - R55k")
    private String salaryRange;

    @Size(max = 8)
    @Schema(description = "Currency for the salary expectation.", example = "ZAR")
    private String salaryCurrency;

    @Size(max = 20)
    @Schema(description = "Salary period: HOURLY, MONTHLY, ANNUALLY, ...", example = "MONTHLY")
    private String salaryPeriod;

    @Schema(description = "Initial note to add to the application.", example = "Referral from John Doe.")
    private String initialNote;
}
