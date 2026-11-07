package com.forkmyfolio.dto.update;

import com.forkmyfolio.model.enums.MessagePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * DTO for partially updating a ContactMessage.
 * Null fields will be ignored during the update.
 */
@Data
public class UpdateContactMessageRequest {

    @Schema(description = "The priority level of the message.", example = "HIGH")
    private MessagePriority priority;

    @Schema(description = "Whether the message has been read.", example = "true")
    private Boolean read;

    @Schema(description = "Whether the message has been replied to.", example = "false")
    private Boolean replied;

    @Schema(description = "Whether the message has been archived.", example = "false")
    private Boolean archived;
}