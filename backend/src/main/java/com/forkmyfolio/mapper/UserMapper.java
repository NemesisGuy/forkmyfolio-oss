package com.forkmyfolio.mapper;

import com.forkmyfolio.dto.backup.UserBackupDto;
import com.forkmyfolio.dto.request.RegisterRequest;
import com.forkmyfolio.dto.response.PublicUserDto;
import com.forkmyfolio.dto.response.UserDto;
import com.forkmyfolio.dto.update.UpdateUserAccountRequest;
import com.forkmyfolio.model.enums.Role;
import com.forkmyfolio.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

/**
 * Mapper class responsible for converting between User domain models and
 * User-related DTOs.
 * This keeps the conversion logic separate from the service and controller
 * layers.
 */
@Component
public class UserMapper {

    /**
     * Converts a User entity to a UserDto.
     *
     * @param user The User entity to convert.
     * @return The corresponding UserDto.
     */
    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }
        UserDto dto = new UserDto();
        dto.setId(user.getUuid());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setSlug(user.getSlug());
        dto.setProfileImageUrl(user.getProfileImageUrl());
        dto.setProvider(user.getProvider());
        dto.setProviderId(user.getProviderId());
        dto.setActive(user.isActive());
        dto.setPasswordLastChangedAt(user.getPasswordLastChangedAt());
        dto.setRoles(resolveRoles(user));
        // Add compliance fields to the DTO
        dto.setTermsAcceptedAt(user.getTermsAcceptedAt());
        dto.setTermsVersion(user.getTermsVersion());

        return dto;
    }

    private Set<Role> resolveRoles(User user) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getAuthorities() != null &&
                    auth.getAuthorities().stream()
                            .anyMatch(a -> a != null && "ROLE_GUEST".equals(a.getAuthority()))) {
                return Collections.singleton(Role.GUEST);
            }
            return user.getRoles() != null ? user.getRoles() : Collections.emptySet();
        } catch (Exception e) {
            // Fallback for detached entities or other mapping issues
            return Collections.singleton(Role.USER);
        }
    }

    /**
     * Converts a User entity to a backup-specific DTO, including the encoded
     * password hash. This is only used by authorized backup/restore workflows.
     */
    public UserBackupDto toBackupDto(User user) {
        if (user == null) {
            return null;
        }
        UserBackupDto dto = new UserBackupDto();
        dto.setId(user.getUuid());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setSlug(user.getSlug());
        dto.setProfileImageUrl(user.getProfileImageUrl());
        dto.setProvider(user.getProvider());
        dto.setProviderId(user.getProviderId());
        dto.setRoles(user.getRoles());
        dto.setActive(user.isActive());
        dto.setPasswordHash(user.getPassword());
        dto.setPasswordLastChangedAt(user.getPasswordLastChangedAt());
        dto.setTermsAcceptedAt(user.getTermsAcceptedAt());
        dto.setTermsVersion(user.getTermsVersion());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }

    /**
     * Converts a backup-specific DTO to a User entity, restoring the encoded
     * password hash verbatim (without re-encoding).
     */
    public User toEntityFromBackup(UserBackupDto dto) {
        if (dto == null) {
            return null;
        }
        User user = new User();
        user.setUuid(dto.getId());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setSlug(dto.getSlug());
        user.setProfileImageUrl(dto.getProfileImageUrl());
        user.setRoles(dto.getRoles());
        user.setActive(dto.isActive());
        user.setProvider(dto.getProvider());
        user.setProviderId(dto.getProviderId());
        user.setTermsAcceptedAt(dto.getTermsAcceptedAt());
        user.setTermsVersion(dto.getTermsVersion());
        if (dto.getPasswordHash() != null && !dto.getPasswordHash().isBlank()) {
            user.setPassword(dto.getPasswordHash());
        }
        return user;
    }

    /**
     * Converts a RegisterRequest to a User entity.
     * Note: Does not handle password encoding or slug generation (service
     * responsibility).
     */
    public User toEntity(RegisterRequest request) {
        if (request == null) {
            return null;
        }
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        return user;
    }

    /**
     * Converts a UserDto (from backup) to a User entity.
     */
    public User toEntity(UserDto dto) {
        if (dto == null) {
            return null;
        }
        User user = new User();
        user.setUuid(dto.getId());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setSlug(dto.getSlug());
        user.setProfileImageUrl(dto.getProfileImageUrl());
        user.setRoles(dto.getRoles());
        user.setActive(dto.isActive());
        user.setProvider(dto.getProvider());
        user.setTermsAcceptedAt(dto.getTermsAcceptedAt());
        user.setTermsVersion(dto.getTermsVersion());
        return user;
    }

    /**
     * Applies updates from an UpdateUserAccountRequest DTO to an existing User
     * entity.
     * This method modifies the passed-in User object directly.
     *
     * @param request The DTO containing the update information.
     * @param user    The User entity to be updated.
     */
    public void applyUpdateFromRequest(UpdateUserAccountRequest request, User user) {
        if (request == null || user == null) {
            return;
        }
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setProfileImageUrl(request.getProfileImageUrl());
    }

    /**
     * Converts a User entity to a PublicUserDto, exposing only public information.
     *
     * @param user The User entity to convert.
     * @return The corresponding PublicUserDto.
     */
    public PublicUserDto toPublicUserDto(User user) {
        if (user == null) {
            return null;
        }
        PublicUserDto dto = new PublicUserDto();
        dto.setSlug(user.getSlug());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setProfileImageUrl(user.getProfileImageUrl());
        dto.setActive(user.isActive());
        dto.setRoles(user.getRoles());

        return dto;
    }
}