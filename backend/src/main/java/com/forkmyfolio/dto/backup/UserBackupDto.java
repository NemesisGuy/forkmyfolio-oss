package com.forkmyfolio.dto.backup;

import com.forkmyfolio.model.enums.AuthProvider;
import com.forkmyfolio.model.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Backup-specific representation of a user account.
 *
 * <p>This DTO is used only by the authorized backup/restore workflows. It is
 * deliberately separate from the ordinary {@code UserDto} so that password hashes
 * are never exposed through regular user or administrator APIs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UserBackupDto", description = "User account data for backup/restore, including the encoded password hash.")
public class UserBackupDto {

    @Schema(description = "The user's unique identifier.")
    private UUID id;

    @Schema(description = "The user's email address.")
    private String email;

    @Schema(description = "The user's first name.")
    private String firstName;

    @Schema(description = "The user's last name.")
    private String lastName;

    @Schema(description = "The user's URL slug.")
    private String slug;

    @Schema(description = "URL to the user's profile image.")
    private String profileImageUrl;

    @Schema(description = "The authentication provider used to register this user.")
    private AuthProvider provider;

    @Schema(description = "The unique identifier from the external provider.")
    private String providerId;

    @Schema(description = "The user's roles.")
    private Set<Role> roles;

    @Schema(description = "Whether the account is active.")
    private boolean active;

    /**
     * The user's encoded password hash (BCrypt). This is sensitive credential
     * material and is only present in authorized backup exports. It is restored
     * verbatim without re-encoding.
     */
    @Schema(description = "The encoded password hash (BCrypt). Sensitive.")
    private String passwordHash;

    @Schema(description = "Timestamp when the password was last changed.")
    private Instant passwordLastChangedAt;

    @Schema(description = "Timestamp when terms were accepted.")
    private Instant termsAcceptedAt;

    @Schema(description = "The accepted terms version.")
    private String termsVersion;

    @Schema(description = "Timestamp when the user was created.")
    private Instant createdAt;

    @Schema(description = "Timestamp when the user was last updated.")
    private Instant updatedAt;
}
