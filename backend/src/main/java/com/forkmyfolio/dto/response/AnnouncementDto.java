package com.forkmyfolio.dto.response;

import com.forkmyfolio.model.enums.Role;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AnnouncementDto {
    private UUID uuid;
    private String title;
    private String message;
    private Role targetRole;
    private LocalDateTime createdAt;
    private String senderName;
    private boolean read;
}
