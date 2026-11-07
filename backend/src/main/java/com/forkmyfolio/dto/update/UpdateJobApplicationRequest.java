package com.forkmyfolio.dto.update;

import com.forkmyfolio.model.enums.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(name = "UpdateJobApplicationRequest", description = "Data required to update an existing job application.")
public class UpdateJobApplicationRequest {

    @Size(min = 2, max = 150)
    @Schema(description = "The updated title of the job position.")
    private String jobTitle;

    @Size(min = 2, max = 100)
    @Schema(description = "The updated name of the company.")
    private String companyName;

    @Size(max = 100)
    private String jobLocation;

    @Size(max = 50)
    private String jobType;

    @URL
    @Size(max = 2048)
    private String applicationUrl;

    @Size(max = 100)
    private String resumeVersion;

    @Schema(description = "Updated status of the application.")
    private ApplicationStatus status;

    @Schema(description = "Manual override for applied date.")
    private LocalDateTime appliedAt;

    // --- Phase 8.4: extended data model (all optional; null = no change) ---

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

    @Schema(description = "Visibility toggle. Omitted (null) preserves the stored value.", nullable = true)
    private Boolean visible;
}
