-- V7__Add_Guest_Passes.sql
-- Create the guest_passes table for temporary portfolio access support

CREATE TABLE guest_passes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid BINARY(16) NOT NULL UNIQUE,
    token VARCHAR(255) NOT NULL UNIQUE,
    label VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_guest_passes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Index for token lookup performance
CREATE INDEX idx_guest_passes_token ON guest_passes(token);
