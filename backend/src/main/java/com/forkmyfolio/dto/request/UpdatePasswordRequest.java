package com.forkmyfolio.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePasswordRequest {

    @NotEmpty(message = "New password cannot be empty.")
    @Size(min = 8, message = "Password must be at least 8 characters long.")
    private String newPassword;

}