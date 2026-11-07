-- V11 (Phase 8.4): Job Application Tracker lifecycle + data model extension.
--
-- 1) status column: MySQL ENUM in V8 with the original 7 values. Extend the
--    ENUM set to the full 13-state lifecycle requested for Phase 8.4 PLUS the
--    two legacy values (INTERVIEWING, ACCEPTED) so every existing row remains
--    valid. MODIFY COLUMN with a superset of the old values is a metadata-only
--    change in MySQL 8 — no data transformation, nothing destructive.
-- 2) New optional columns for the management UI (all NULL-able; existing rows
--    keep working unchanged).

ALTER TABLE job_applications
    MODIFY COLUMN status ENUM(
        'INTERESTED',
        'PREPARING',
        'DRAFT',
        'APPLIED',
        'ACKNOWLEDGED',
        'INTERVIEW_SCHEDULED',
        'INTERVIEW_COMPLETED',
        'TECHNICAL_ASSESSMENT',
        'OFFER',
        'REJECTED',
        'WITHDRAWN',
        'NO_RESPONSE',
        'ARCHIVED',
        'INTERVIEWING',
        'ACCEPTED'
    ) NOT NULL;

ALTER TABLE job_applications
    ADD COLUMN job_description TEXT DEFAULT NULL,
    ADD COLUMN work_arrangement VARCHAR(20) DEFAULT NULL,
    ADD COLUMN discovered_at DATE DEFAULT NULL,
    ADD COLUMN deadline_at DATE DEFAULT NULL,
    ADD COLUMN follow_up_at DATE DEFAULT NULL,
    ADD COLUMN interview_at DATETIME(6) DEFAULT NULL,
    ADD COLUMN assessment_at DATE DEFAULT NULL,
    ADD COLUMN recruiter_name VARCHAR(100) DEFAULT NULL,
    ADD COLUMN recruiter_email VARCHAR(150) DEFAULT NULL,
    ADD COLUMN contact_details VARCHAR(255) DEFAULT NULL,
    ADD COLUMN salary_range VARCHAR(100) DEFAULT NULL,
    ADD COLUMN salary_currency VARCHAR(8) DEFAULT NULL,
    ADD COLUMN salary_period VARCHAR(20) DEFAULT NULL;
