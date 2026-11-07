package com.forkmyfolio.dto.backup;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupMetaDto {

    @Schema(description = "The version of the application that exported this file.", example = "2.0.0")
    private String version;

    @Schema(description = "The timestamp when the backup was exported.", example = "2025-08-01T12:00:00Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private ZonedDateTime exportedAt;

    @Schema(description = "The type of backup.", example = "user_backup")
    private String type;

    @Schema(description = "Compatibility information for this backup file.")
    private Compatibility compatibility;

    @Schema(description = "The user slug of the person who exported the backup (optional).", example = "nemesis")
    private String exportedBy;

    @Schema(description = "The system that generated the backup.", example = "ForkMyFolio")
    private String system;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Compatibility {
        @Schema(description = "The minimum application version that can read this backup.", example = "2.0.0")
        private String minSupportedVersion;

        @Schema(description = "The maximum application version that can read this backup.", example = "2.x")
        private String maxSupportedVersion;
    }
}