-- V4__Add_Announcements.sql
-- Create announcements table to match Announcement.java entity
-- Fixes Schema Drift where entity existed but table did not.

CREATE TABLE announcements (
    uuid BINARY(16) NOT NULL PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    message TEXT NOT NULL,
    target_role ENUM('ADMIN', 'USER'),
    title VARCHAR(255) NOT NULL,
    sender_id BIGINT,
    CONSTRAINT fk_announcements_sender FOREIGN KEY (sender_id) REFERENCES users (id)
);
