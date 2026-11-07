package com.forkmyfolio.mapper;

import com.forkmyfolio.dto.response.GuestPassDto;
import com.forkmyfolio.model.GuestPass;
import org.springframework.stereotype.Component;

@Component
public class GuestPassMapper {

    public GuestPassDto toDto(GuestPass guestPass) {
        if (guestPass == null)
            return null;

        return GuestPassDto.builder()
                .uuid(guestPass.getUuid())
                .ownerId(guestPass.getOwner().getId())
                .label(guestPass.getLabel())
                .token(guestPass.getToken())
                .expiresAt(guestPass.getExpiresAt())
                .expired(guestPass.isExpired())
                .createdAt(guestPass.getCreatedAt())
                .build();
    }
}
