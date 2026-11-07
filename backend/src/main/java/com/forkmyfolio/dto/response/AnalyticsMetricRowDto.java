package com.forkmyfolio.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One ranked row of a metrics breakdown (e.g. top referrer with its count).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A ranked metrics row: dimension value and its count.")
public class AnalyticsMetricRowDto {

    @Schema(description = "Dimension value, e.g. a page path or a country code.")
    private String name;

    @Schema(description = "Count (pageviews/events, or unique visitors for visitor dimensions).")
    private long value;
}
