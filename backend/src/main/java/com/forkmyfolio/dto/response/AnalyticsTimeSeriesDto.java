package com.forkmyfolio.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Pageview/session counts over time for a date range.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Analytics time series for a date range.")
public class AnalyticsTimeSeriesDto {

    @Schema(description = "Pageview counts bucketed by the requested unit.")
    private List<Point> pageviews;

    @Schema(description = "Session counts bucketed by the requested unit, when available.")
    private List<Point> sessions;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "One bucket in the time series.")
    public static class Point {
        @Schema(description = "Bucket start timestamp (epoch milliseconds).")
        private long timestamp;

        @Schema(description = "Count for this bucket.")
        private long value;
    }
}
