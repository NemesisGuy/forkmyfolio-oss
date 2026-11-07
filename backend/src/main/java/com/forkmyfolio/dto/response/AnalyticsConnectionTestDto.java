package com.forkmyfolio.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Result of a provider connectivity test initiated from the CMS.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Result of an analytics provider connection test.")
public class AnalyticsConnectionTestDto {

    @Schema(description = "True when the provider API responded successfully.")
    private boolean success;

    @Schema(description = "Human-readable outcome, safe to display in the CMS.")
    private String message;

    @Schema(description = "HTTP status returned by the provider, when applicable.")
    private Integer providerStatus;
}
