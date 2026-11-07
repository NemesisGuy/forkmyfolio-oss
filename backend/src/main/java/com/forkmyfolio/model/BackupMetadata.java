package com.forkmyfolio.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupMetadata {
    private String version;
    private ZonedDateTime exportedAt;
    private String type;
    private Compatibility compatibility;
    private String exportedBy;
    private String system;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Compatibility {
        private String minSupportedVersion;
        private String maxSupportedVersion;
    }
}
