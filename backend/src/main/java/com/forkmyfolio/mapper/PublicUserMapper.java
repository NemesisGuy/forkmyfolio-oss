package com.forkmyfolio.mapper;

import com.forkmyfolio.dto.response.PublicUserDto;
import com.forkmyfolio.model.User;
import org.springframework.stereotype.Component;

/**
 * Mapper responsible for converting a User entity into a PublicUserDto.
 * This mapper ensures that only fields safe for public consumption are exposed,
 * omitting sensitive data like roles, email, or account status.
 */
@Component
public class PublicUserMapper {

    /**
     * Manually maps a User entity to its public-facing DTO representation.
     *
     * @param user The User entity from the database.
     * @return A PublicUserDto containing only public-safe fields, or null if the input is null.
     */
    public PublicUserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        PublicUserDto dto = new PublicUserDto();
        dto.setSlug(user.getSlug());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setProfileImageUrl(user.getProfileImageUrl());

        return dto;
    }
}