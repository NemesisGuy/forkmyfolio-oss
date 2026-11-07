package com.forkmyfolio.dto.update;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UpdateSettingRequest {
    @NotNull
    private UUID uuid;
    /*   @NotBlank
       private String name;*/
    @NotNull(message = "Setting value cannot be null")
    private String value;
}