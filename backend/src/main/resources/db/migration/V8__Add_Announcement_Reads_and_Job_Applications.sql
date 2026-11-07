-- V8: Add tables that were previously only created by Hibernate ddl-auto=update.
-- These complement the V1-V7 Flyway migrations so a clean deployment does not
-- depend on hibernate schema generation.

CREATE TABLE IF NOT EXISTS announcement_reads (
    id BIGINT NOT NULL AUTO_INCREMENT,
    read_at DATETIME(6) NOT NULL,
    announcement_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY UK_announcement_reads_user_announcement (user_id, announcement_id),
    KEY FK_announcement_reads_announcement (announcement_id),
    CONSTRAINT FK_announcement_reads_announcement FOREIGN KEY (announcement_id) REFERENCES announcements (id),
    CONSTRAINT FK_announcement_reads_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS job_applications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uuid VARCHAR(36) NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    job_title VARCHAR(150) NOT NULL,
    job_type VARCHAR(50) DEFAULT NULL,
    job_location VARCHAR(100) DEFAULT NULL,
    application_url VARCHAR(2048) DEFAULT NULL,
    resume_version VARCHAR(100) DEFAULT NULL,
    status ENUM('DRAFT','APPLIED','INTERVIEWING','REJECTED','OFFER','ACCEPTED','WITHDRAWN') NOT NULL,
    applied_at DATETIME(6) DEFAULT NULL,
    status_updated_at DATETIME(6) NOT NULL,
    visible BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY UK_job_applications_uuid (uuid),
    KEY FK_job_applications_user (user_id),
    CONSTRAINT FK_job_applications_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS job_application_notes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uuid VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    job_application_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY UK_job_application_notes_uuid (uuid),
    KEY FK_job_application_notes_application (job_application_id),
    CONSTRAINT FK_job_application_notes_application FOREIGN KEY (job_application_id) REFERENCES job_applications (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
