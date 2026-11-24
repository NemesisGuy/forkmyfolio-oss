package com.forkmyfolio.mapper;

import com.forkmyfolio.dto.backup.UserBackupDto;
import com.forkmyfolio.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Regression pins for backup fidelity in {@link UserMapper#toEntityFromBackup}
 * (surfaced by the KNOWN_ISSUES §10 system-restore UAT on DEV).
 *
 * <p>The export direction ({@code toBackupDto}) always carried
 * {@code passwordLastChangedAt} and {@code createdAt}, but the import
 * direction dropped them — so every system restore reset the field, the admin
 * frontend read {@code !user.passwordLastChangedAt}, and all users were
 * forced through a password change after every restore.
 */
class UserMapperBackupFidelityTest {

    private final UserMapper userMapper = new UserMapper();

    @Test
    void toEntityFromBackup_preservesPasswordLastChangedAtAndCreatedAt() {
        UserBackupDto dto = new UserBackupDto();
        dto.setEmail("fidelity@example.com");
        Instant passwordChangedAt = Instant.parse("2026-11-20T10:15:30.123456Z");
        Instant createdAt = Instant.parse("2026-11-01T08:00:00.000000Z");
        dto.setPasswordLastChangedAt(passwordChangedAt);
        dto.setCreatedAt(createdAt);

        User user = userMapper.toEntityFromBackup(dto);

        assertNotNull(user);
        assertEquals(passwordChangedAt, user.getPasswordLastChangedAt(),
                "restore must carry the password-change timestamp, otherwise the UI "
                        + "forces a password change after every system restore");
        assertEquals(createdAt, user.getCreatedAt(),
                "restore must carry the original account creation time");
    }

    @Test
    void toEntityFromBackup_legacyBackupWithoutTimestamps_keepsThemNull() {
        // Legacy (V2) files predate these fields: they must map to null so the
        // existing behaviour (fresh stamp at persist) is preserved.
        UserBackupDto dto = new UserBackupDto();
        dto.setEmail("legacy@example.com");

        User user = userMapper.toEntityFromBackup(dto);

        assertNotNull(user);
        assertNull(user.getPasswordLastChangedAt());
        assertNull(user.getCreatedAt());
    }

    @Test
    void toEntityFromBackup_restoresPasswordHashVerbatim() {
        // Existing contract, re-pinned alongside the fidelity fixes: the hash
        // must never be re-encoded on restore.
        UserBackupDto dto = new UserBackupDto();
        dto.setEmail("hash@example.com");
        dto.setPasswordHash("$2a$10$restoredHashVerbatim00000000000000000000000000000");

        User user = userMapper.toEntityFromBackup(dto);

        assertEquals("$2a$10$restoredHashVerbatim00000000000000000000000000000",
                user.getPassword());
    }
}
