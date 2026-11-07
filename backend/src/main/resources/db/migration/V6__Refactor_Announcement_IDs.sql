-- V6__Refactor_Announcement_IDs.sql
-- Refactor announcements table to follow "Internal Long ID / External UUID ID" standard

-- 1. Drop existing primary key on UUID first
ALTER TABLE announcements DROP PRIMARY KEY;

-- 2. Add the temporary ID column and make it the new PRIMARY KEY
ALTER TABLE announcements ADD COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST;

-- 3. Set UUID as a unique index (it remains BINARY(16) NOT NULL)
ALTER TABLE announcements ADD CONSTRAINT uk_announcements_uuid UNIQUE (uuid);

-- 4. Re-enable serial ID correctly if any data exists
SET @count = 0;
UPDATE announcements SET id = (@count := @count + 1);
