-- V9__Add_Project_Key_Features.sql
-- Phase 7.4 (§6): structured key-features collection for projects.
-- ADDITIVE ONLY: creates a new child table; no existing columns or rows are
-- modified or destroyed. Key features were previously embedded as Markdown
-- bullet lines inside project descriptions; those descriptions are left
-- untouched (the public UI falls back to them when the collection is empty).

CREATE TABLE key_features (
                           id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                           value         VARCHAR(255) NOT NULL,
                           display_order INT NOT NULL,
                           project_id    BIGINT NOT NULL,
                           uuid          VARCHAR(36) NOT NULL,
                           UNIQUE (uuid),
                           FOREIGN KEY (project_id) REFERENCES projects(id)
);
