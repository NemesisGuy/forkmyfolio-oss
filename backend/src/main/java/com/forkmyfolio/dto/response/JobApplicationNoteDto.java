package com.forkmyfolio.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Schema(name = "JobApplicationNoteDto", description = "Represents a private note on a job application in an API response.")
public class JobApplicationNoteDto {

    @Schema(description = "The unique public identifier for the note.", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID uuid;

    @Schema(description = "The detailed content of the note.", example = "Interview with the CTO went well. Focus on RAG next time.")
    private String content;

    @Schema(description = "The timestamp when the note was created.")
    private LocalDateTime createdAt;
}
