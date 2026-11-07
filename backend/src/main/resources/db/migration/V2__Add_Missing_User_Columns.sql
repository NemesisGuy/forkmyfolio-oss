-- V2__Add_Missing_User_Columns.sql
-- Add columns that were added to the User entity but might be missing in older database instances
-- Using a stored procedure for idempotency in MySQL 8.0 (since ADD COLUMN IF NOT EXISTS is 8.0.29+)

DROP PROCEDURE IF EXISTS add_column_if_not_exists;

DELIMITER //

CREATE PROCEDURE add_column_if_not_exists(
    IN tableName VARCHAR(255),
    IN columnName VARCHAR(255),
    IN columnDefinition VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT * FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_NAME = tableName
        AND COLUMN_NAME = columnName
        AND TABLE_SCHEMA = DATABASE()
    ) THEN
        SET @query = CONCAT('ALTER TABLE ', tableName, ' ADD COLUMN ', columnName, ' ', columnDefinition);
        PREPARE stmt FROM @query;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

DELIMITER ;

CALL add_column_if_not_exists('users', 'password_last_changed_at', 'DATETIME(6) AFTER profile_image_url');
CALL add_column_if_not_exists('users', 'terms_accepted_at', 'DATETIME(6) AFTER uuid');
CALL add_column_if_not_exists('users', 'terms_version', 'VARCHAR(255) AFTER terms_accepted_at');

DROP PROCEDURE IF EXISTS add_column_if_not_exists;
