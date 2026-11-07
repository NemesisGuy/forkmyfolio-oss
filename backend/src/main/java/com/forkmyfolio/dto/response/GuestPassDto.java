package com.forkmyfolio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestPassDto {
    private UUID uuid;
    private Long ownerId;
    private String label;
    private String token;
    private Instant expiresAt;
    private boolean expired;
    private Instant createdAt;
}
