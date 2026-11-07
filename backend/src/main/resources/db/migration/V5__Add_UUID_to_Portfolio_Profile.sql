-- V5__Add_UUID_to_Portfolio_Profile.sql
-- Add uuid column to portfolio_profiles if it doesn't exist

DROP PROCEDURE IF EXISTS add_uuid_to_portfolio_profiles;

DELIMITER //

CREATE PROCEDURE add_uuid_to_portfolio_profiles()
BEGIN
    IF NOT EXISTS (
        SELECT * FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_NAME = 'portfolio_profiles'
        AND COLUMN_NAME = 'uuid'
        AND TABLE_SCHEMA = DATABASE()
    ) THEN
        -- Add as nullable first
        ALTER TABLE portfolio_profiles ADD COLUMN uuid BINARY(16);

        -- Backfill existing rows with random UUIDs
        UPDATE portfolio_profiles SET uuid = UNHEX(REPLACE(UUID(),'-','')) WHERE uuid IS NULL;

        -- Make it NOT NULL
        ALTER TABLE portfolio_profiles MODIFY uuid BINARY(16) NOT NULL;

        -- Add Unique Constraint
        ALTER TABLE portfolio_profiles ADD CONSTRAINT uk_portfolio_profile_uuid UNIQUE (uuid);
    END IF;
END //

DELIMITER ;

CALL add_uuid_to_portfolio_profiles();

DROP PROCEDURE IF EXISTS add_uuid_to_portfolio_profiles;
