package com.forkmyfolio.dto.update;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(name = "UpdateUserSettingRequest", description = "A single key-value pair for updating a user's setting, identified by its UUID.")
public class UpdateUserSettingRequest {

    @NotNull(message = "Setting UUID cannot be null.")
    @Schema(description = "The UUID of the setting to update.", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID uuid;

    @NotBlank(message = "Setting value cannot be blank.")
    @Schema(description = "The new value for the setting.", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private String value;
}