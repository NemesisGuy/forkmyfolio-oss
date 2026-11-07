package com.forkmyfolio.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Aggregated summary statistics for a date range, with the comparison period
 * (previous equal-length period) when the provider supports it.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated analytics summary for a date range.")
public class AnalyticsSummaryDto {

    @Schema(description = "Total pageviews in the range.")
    private long pageviews;

    @Schema(description = "Unique visitors in the range.")
    private long visitors;

    @Schema(description = "Number of visits (sessions) in the range.")
    private long visits;

    @Schema(description = "Visits with a single pageview (bounces).")
    private long bounces;

    @Schema(description = "Total time on site in seconds.")
    private long totalTimeSeconds;

    @Schema(description = "The previous equal-length period, when available.")
    private Comparison comparison;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Summary for the comparison period.")
    public static class Comparison {
        private long pageviews;
        private long visitors;
        private long visits;
        private long bounces;
        private long totalTimeSeconds;
    }
}
