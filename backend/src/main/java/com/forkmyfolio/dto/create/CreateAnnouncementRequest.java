package com.forkmyfolio.dto.create;

import com.forkmyfolio.model.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "CreateAnnouncementRequest", description = "Payload for creating a system announcement.")
public class CreateAnnouncementRequest {

    @NotBlank(message = "Title is required.")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters.")
    @Schema(description = "The title of the announcement.", example = "System Maintenance")
    private String title;

    @NotBlank(message = "Message is required.")
    @Size(min = 10, max = 2000, message = "Message must be between 10 and 2000 characters.")
    @Schema(description = "The content of the announcement.", example = "The system will be down for maintenance on Sunday at 2 AM.")
    private String message;

    @Schema(description = "The target role for this announcement. If null, sent to everyone.", example = "USER")
    private Role targetRole;
}
