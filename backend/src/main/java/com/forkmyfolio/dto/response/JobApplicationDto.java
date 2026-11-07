package com.forkmyfolio.dto.response;

import com.forkmyfolio.model.enums.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(name = "JobApplicationDto", description = "Represents a tracked job application in an API response.")
public class JobApplicationDto {

    @Schema(description = "The unique public identifier for the job application.", example = "a1b2c3d4-e5f6-a7b8-c9d0-e1f2a3b4c5d6")
    private UUID uuid;

    @Schema(description = "The title of the job position.", example = "Senior Software Engineer")
    private String jobTitle;

    @Schema(description = "The name of the company.", example = "NemesisNet")
    private String companyName;

    @Schema(description = "The location of the job.", example = "Remote / Johannesburg")
    private String jobLocation;

    @Schema(description = "The type of employment.", example = "Full-Time")
    private String jobType;

    @Schema(description = "URL to the original job posting or application portal.", example = "https://linkedin.com/jobs/12345")
    private String applicationUrl;

    @Schema(description = "Identifier or label for the resume version used.", example = "FullStack-v1.2")
    private String resumeVersion;

    // --- Phase 8.4: extended data model ---

    @Schema(description = "The full job description / posting text.")
    private String jobDescription;

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

    @Schema(description = "Recruiter or contact person's name.")
    private String recruiterName;

    @Schema(description = "Recruiter or contact person's email.")
    private String recruiterEmail;

    @Schema(description = "Free-form contact details (phone, portal handle, ...).")
    private String contactDetails;

    @Schema(description = "Salary / remuneration expectation.", example = "R45k - R55k")
    private String salaryRange;

    @Schema(description = "Currency for the salary expectation.", example = "ZAR")
    private String salaryCurrency;

    @Schema(description = "Salary period: HOURLY, MONTHLY, ANNUALLY, ...", example = "MONTHLY")
    private String salaryPeriod;

    @Schema(description = "Current lifecycle status of the application.", example = "INTERVIEWING")
    private ApplicationStatus status;

    @Schema(description = "Timestamp of the last status update.")
    private LocalDateTime statusUpdatedAt;

    @Schema(description = "Timestamp of when the application was formally submitted.")
    private LocalDateTime appliedAt;

    @Schema(description = "Whether the application is visible (for potential future guest sharing).")
    private boolean visible;

    @Schema(description = "The private notes associated with this application.")
    private List<JobApplicationNoteDto> notes;

    @Schema(description = "The timestamp when the application was created.")
    private LocalDateTime createdAt;

    @Schema(description = "The timestamp when the application was last updated.")
    private LocalDateTime updatedAt;

    public JobApplicationDto() {
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getJobLocation() {
        return jobLocation;
    }

    public void setJobLocation(String jobLocation) {
        this.jobLocation = jobLocation;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getApplicationUrl() {
        return applicationUrl;
    }

    public void setApplicationUrl(String applicationUrl) {
        this.applicationUrl = applicationUrl;
    }

    public String getResumeVersion() {
        return resumeVersion;
    }

    public void setResumeVersion(String resumeVersion) {
        this.resumeVersion = resumeVersion;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getWorkArrangement() {
        return workArrangement;
    }

    public void setWorkArrangement(String workArrangement) {
        this.workArrangement = workArrangement;
    }

    public LocalDate getDiscoveredAt() {
        return discoveredAt;
    }

    public void setDiscoveredAt(LocalDate discoveredAt) {
        this.discoveredAt = discoveredAt;
    }

    public LocalDate getDeadlineAt() {
        return deadlineAt;
    }

    public void setDeadlineAt(LocalDate deadlineAt) {
        this.deadlineAt = deadlineAt;
    }

    public LocalDate getFollowUpAt() {
        return followUpAt;
    }

    public void setFollowUpAt(LocalDate followUpAt) {
        this.followUpAt = followUpAt;
    }

    public LocalDateTime getInterviewAt() {
        return interviewAt;
    }

    public void setInterviewAt(LocalDateTime interviewAt) {
        this.interviewAt = interviewAt;
    }

    public LocalDate getAssessmentAt() {
        return assessmentAt;
    }

    public void setAssessmentAt(LocalDate assessmentAt) {
        this.assessmentAt = assessmentAt;
    }

    public String getRecruiterName() {
        return recruiterName;
    }

    public void setRecruiterName(String recruiterName) {
        this.recruiterName = recruiterName;
    }

    public String getRecruiterEmail() {
        return recruiterEmail;
    }

    public void setRecruiterEmail(String recruiterEmail) {
        this.recruiterEmail = recruiterEmail;
    }

    public String getContactDetails() {
        return contactDetails;
    }

    public void setContactDetails(String contactDetails) {
        this.contactDetails = contactDetails;
    }

    public String getSalaryRange() {
        return salaryRange;
    }

    public void setSalaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
    }

    public String getSalaryCurrency() {
        return salaryCurrency;
    }

    public void setSalaryCurrency(String salaryCurrency) {
        this.salaryCurrency = salaryCurrency;
    }

    public String getSalaryPeriod() {
        return salaryPeriod;
    }

    public void setSalaryPeriod(String salaryPeriod) {
        this.salaryPeriod = salaryPeriod;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public LocalDateTime getStatusUpdatedAt() {
        return statusUpdatedAt;
    }

    public void setStatusUpdatedAt(LocalDateTime statusUpdatedAt) {
        this.statusUpdatedAt = statusUpdatedAt;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public List<JobApplicationNoteDto> getNotes() {
        return notes;
    }

    public void setNotes(List<JobApplicationNoteDto> notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
