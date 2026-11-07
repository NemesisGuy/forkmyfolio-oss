package com.forkmyfolio.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forkmyfolio.dto.backup.UserBackupDto;
import com.forkmyfolio.mapper.UserMapper;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.AuthProvider;
import com.forkmyfolio.model.enums.Role;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security tests proving that ordinary user DTOs never expose password hashes,
 * while backup-specific DTOs are the only path carrying the encoded hash.
 */
class UserDtoConfidentialityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserMapper userMapper = new UserMapper();
    private final User sampleUser = buildSampleUser();

    private User buildSampleUser() {
        User user = new User();
        user.setUuid(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setSlug("test-user");
        user.setPassword("$2a$10$abcdefghijklmnopqrstuv");
        user.setProvider(AuthProvider.LOCAL);
        user.setActive(true);
        Set<Role> roles = new HashSet<>();
        roles.add(Role.USER);
        user.setRoles(roles);
        return user;
    }

    @Test
    void userDto_shouldNeverSerializePasswordHash() throws Exception {
        UserDto dto = userMapper.toDto(sampleUser);
        String json = objectMapper.writeValueAsString(dto);

        assertFalse(json.contains("$2a$"), "UserDto leaked a BCrypt hash");
        assertFalse(json.contains("$2b$"), "UserDto leaked a BCrypt hash");
        assertFalse(json.contains("passwordHash"), "UserDto must not carry a passwordHash field");
        // UserDto has no getPassword() getter at all — that is the enforcement.
    }

    @Test
    void userBackupDto_shouldCarryPasswordHashOnlyInBackupContext() throws Exception {
        UserBackupDto dto = userMapper.toBackupDto(sampleUser);
        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("passwordHash"), "Backup DTO must carry passwordHash");
        assertTrue(json.contains("$2a$"), "Backup DTO must carry the encoded hash");
        assertEquals("$2a$10$abcdefghijklmnopqrstuv", dto.getPasswordHash());
    }

    @Test
    void userMapper_shouldNotCopyHashFromBackupDtoToEntityWhenBlank() {
        UserBackupDto dto = new UserBackupDto();
        dto.setEmail("legacy@example.com");
        dto.setPasswordHash("   ");

        User entity = userMapper.toEntityFromBackup(dto);
        assertNull(entity.getPassword(), "A blank hash must not produce a usable password state");
    }

    @Test
    void userDto_shouldNotContainPasswordGetterViaReflection() {
        boolean hasPasswordAccessor = java.util.Arrays.stream(UserDto.class.getMethods())
                .anyMatch(m -> m.getName().equalsIgnoreCase("getPassword")
                        || m.getName().equalsIgnoreCase("getPasswordHash"));
        assertFalse(hasPasswordAccessor, "UserDto must not expose any password accessor");
    }

    @Test
    void userMapper_shouldCopyHashVerbatimFromBackupDtoToEntity() {
        UserBackupDto dto = userMapper.toBackupDto(sampleUser);
        User entity = userMapper.toEntityFromBackup(dto);
        assertEquals(sampleUser.getPassword(), entity.getPassword(),
                "The hash must be restored verbatim, never re-encoded");
    }
}
