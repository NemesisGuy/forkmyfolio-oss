package com.forkmyfolio.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GuestLoginRequest {
    @NotBlank
    private String token;
}
