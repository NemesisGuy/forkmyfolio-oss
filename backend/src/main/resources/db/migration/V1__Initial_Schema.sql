-- V1__Initial_Schema.sql
-- Initial schema aligned with updated DEV database structure, without explicit constraint names

-- User and Security Related Tables
CREATE TABLE users (
                       id                BIGINT AUTO_INCREMENT PRIMARY KEY,
                       active            BIT NOT NULL,
                       created_at        DATETIME(6) NOT NULL,
                       email             VARCHAR(255) NOT NULL,
                       email_verified    BIT NOT NULL,
                       first_name        VARCHAR(255) NOT NULL,
                       last_name         VARCHAR(255) NOT NULL,
                       password          VARCHAR(255),
                       profile_image_url VARCHAR(255),
                       password_last_changed_at DATETIME(6),
                       provider          ENUM('GITHUB', 'GOOGLE', 'LINKEDIN', 'LOCAL') NOT NULL,
                       provider_id       VARCHAR(255),
                       slug              VARCHAR(50) NOT NULL,
                       updated_at        DATETIME(6) NOT NULL,
                       uuid              BINARY(16) NOT NULL,
                       terms_accepted_at DATETIME(6),
                       terms_version     VARCHAR(255),
                       UNIQUE (email),
                       UNIQUE (uuid),
                       UNIQUE (slug)
);

CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            role    ENUM('ADMIN', 'USER') NOT NULL,
                            PRIMARY KEY (user_id, role),
                            FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE refresh_tokens (
                                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                                expiry_date DATETIME(6) NOT NULL,
                                token       VARCHAR(512) NOT NULL,
                                user_id     BIGINT NOT NULL,
                                UNIQUE (token),
                                FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Portfolio and Content Tables
CREATE TABLE portfolio_profiles (
                                    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    cover_letter_template TEXT,
                                    created_at            DATETIME(6),
                                    github_url            VARCHAR(255),
                                    headline              VARCHAR(100),
                                    linkedin_url          VARCHAR(255),
                                    location              VARCHAR(50),
                                    public_email          VARCHAR(255),
                                    resume_image_url      VARCHAR(255),
                                    resume_url            VARCHAR(255),
                                    summary               TEXT,
                                    updated_at            DATETIME(6),
                                    visible               BIT NOT NULL,
                                    website_url           VARCHAR(255),
                                    user_id               BIGINT NOT NULL,
                                    is_public             BIT NOT NULL,
                                    UNIQUE (user_id),
                                    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE projects (
                          id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                          created_at    DATETIME(6) NOT NULL,
                          description   TEXT NOT NULL,
                          display_order INT NOT NULL,
                          image_url     VARCHAR(255),
                          live_url      VARCHAR(255),
                          repo_url      VARCHAR(255),
                          title         VARCHAR(100) NOT NULL,
                          updated_at    DATETIME(6) NOT NULL,
                          uuid          VARCHAR(36) NOT NULL,
                          visible       BIT NOT NULL,
                          user_id       BIGINT NOT NULL,
                          UNIQUE (uuid),
                          FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE skills (
                        id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                        category    VARCHAR(255),
                        created_at  DATETIME(6),
                        description LONGTEXT,
                        icon        VARCHAR(255),
                        level       ENUM('ADVANCED', 'BEGINNER', 'EXPERT', 'INTERMEDIATE'),
                        name        VARCHAR(255) NOT NULL,
                        updated_at  DATETIME(6),
                        uuid        BINARY(16) NOT NULL,
                        visible     BIT NOT NULL,
                        UNIQUE (name),
                        UNIQUE (uuid)
);

CREATE TABLE user_skills (
                             id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                             created_at  DATETIME(6) NOT NULL,
                             description LONGTEXT,
                             level       ENUM('ADVANCED', 'BEGINNER', 'EXPERT', 'INTERMEDIATE') NOT NULL,
                             updated_at  DATETIME(6) NOT NULL,
                             uuid        BINARY(16) NOT NULL,
                             visible     BIT NOT NULL,
                             skill_id    BIGINT NOT NULL,
                             user_id     BIGINT NOT NULL,
                             UNIQUE (user_id, skill_id),
                             UNIQUE (uuid),
                             FOREIGN KEY (skill_id) REFERENCES skills(id),
                             FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE project_skills (
                                project_id BIGINT NOT NULL,
                                skill_id   BIGINT NOT NULL,
                                PRIMARY KEY (project_id, skill_id),
                                FOREIGN KEY (project_id) REFERENCES projects(id),
                                FOREIGN KEY (skill_id) REFERENCES skills(id)
);

CREATE TABLE experiences (
                             id               BIGINT AUTO_INCREMENT PRIMARY KEY,
                             achievements     TEXT,
                             company_logo_url VARCHAR(255),
                             company_name     VARCHAR(100) NOT NULL,
                             company_url      VARCHAR(255),
                             created_at       DATETIME(6),
                             description      TEXT,
                             display_order    INT NOT NULL,
                             employment_type  ENUM('APPRENTICESHIP', 'CONTRACT', 'FREELANCE', 'FULL_TIME', 'INTERNSHIP', 'PART_TIME'),
                             end_date         DATE,
                             job_title        VARCHAR(100) NOT NULL,
                             location         VARCHAR(100),
                             location_type    ENUM('HYBRID', 'ON_SITE', 'REMOTE'),
                             start_date       DATE NOT NULL,
                             updated_at       DATETIME(6),
                             uuid             VARCHAR(36) NOT NULL,
                             visible          BIT NOT NULL,
                             user_id          BIGINT NOT NULL,
                             UNIQUE (uuid),
                             FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE experience_skills (
                                   experience_id BIGINT NOT NULL,
                                   skill_id      BIGINT NOT NULL,
                                   PRIMARY KEY (experience_id, skill_id),
                                   FOREIGN KEY (experience_id) REFERENCES experiences(id),
                                   FOREIGN KEY (skill_id) REFERENCES skills(id)
);

CREATE TABLE qualifications (
                                id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
                                completion_year      INT,
                                created_at           DATETIME(6),
                                credential_url       VARCHAR(512),
                                field_of_study       VARCHAR(255),
                                grade                VARCHAR(255),
                                institution_logo_url VARCHAR(512),
                                institution_name     VARCHAR(255) NOT NULL,
                                institution_website  VARCHAR(255),
                                level                ENUM('ADVANCED_DIPLOMA', 'ASSOCIATE', 'BACHELORS', 'BTECH', 'CERTIFICATE', 'DIPLOMA', 'DOCTORATE', 'HIGH_SCHOOL', 'HONOURS', 'MASTERS', 'POST_GRADUATE_DIPLOMA'),
                                qualification_name   VARCHAR(255) NOT NULL,
                                start_year           INT,
                                still_studying       BIT NOT NULL,
                                updated_at           DATETIME(6),
                                uuid                 VARCHAR(36) NOT NULL,
                                visible              BIT NOT NULL,
                                user_id              BIGINT NOT NULL,
                                UNIQUE (uuid),
                                FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE testimonials (
                              id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                              author_name  VARCHAR(100) NOT NULL,
                              author_title VARCHAR(100),
                              created_at   DATETIME(6),
                              quote        TEXT NOT NULL,
                              updated_at   DATETIME(6),
                              uuid         VARCHAR(36) NOT NULL,
                              visible      BIT NOT NULL,
                              user_id      BIGINT NOT NULL,
                              UNIQUE (uuid),
                              FOREIGN KEY (user_id) REFERENCES users(id)
);

-- System and Message Tables
CREATE TABLE contact_messages (
                                  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  created_at  DATETIME(6) NOT NULL,
                                  email       VARCHAR(255) NOT NULL,
                                  is_archived BIT NOT NULL,
                                  is_read     BIT NOT NULL,
                                  is_replied  BIT NOT NULL,
                                  message     TEXT NOT NULL,
                                  name        VARCHAR(100) NOT NULL,
                                  priority    ENUM('HIGH', 'LOW', 'MEDIUM') NOT NULL,
                                  uuid        VARCHAR(36) NOT NULL,
                                  user_id     BIGINT NOT NULL,
                                  UNIQUE (uuid),
                                  FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE settings (
                          id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                          description VARCHAR(512),
                          name        VARCHAR(255) NOT NULL,
                          uuid        VARCHAR(36) NOT NULL,
                          value       VARCHAR(255) NOT NULL,
                          UNIQUE (uuid),
                          UNIQUE (name)
);

CREATE TABLE user_settings (
                               id      BIGINT AUTO_INCREMENT PRIMARY KEY,
                               name    VARCHAR(255) NOT NULL,
                               uuid    BINARY(16) NOT NULL,
                               value   VARCHAR(255) NOT NULL,
                               user_id BIGINT NOT NULL,
                               UNIQUE (uuid),
                               UNIQUE (user_id, name),
                               FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE visitor_stats (
                               id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                               count      BIGINT NOT NULL,
                               ref_id     VARCHAR(255) NOT NULL,
                               type       ENUM('CONTACT_MESSAGE_SUBMISSION', 'EXPERIENCE_SECTION_VIEW', 'LOGIN_FAILURE', 'LOGIN_SUCCESS', 'LOGOUT_SUCCESS', 'MARKDOWN_DOWNLOAD', 'PDF_DOWNLOAD', 'PROJECTS_SECTION_VIEW', 'PROJECT_VIEW', 'QUALIFICATIONS_SECTION_VIEW', 'SKILLS_SECTION_VIEW', 'TESTIMONIALS_SECTION_VIEW', 'TOTAL_VISITS', 'VCARD_DOWNLOAD') NOT NULL,
                               updated_at DATETIME(6)
);