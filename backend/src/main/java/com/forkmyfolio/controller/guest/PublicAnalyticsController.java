package com.forkmyfolio.controller.guest;

import com.forkmyfolio.advice.ApiResponseWrapper;
import com.forkmyfolio.dto.response.PublicAnalyticsConfigDto;
import com.forkmyfolio.service.UserAnalyticsConfigService;
import com.forkmyfolio.service.servicemodel.TrackingSettings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public analytics tracking configuration (DEF-1).
 *
 * <p>Returns only non-credential values (script URL, public website ID,
 * recorder flag) for a portfolio slug, resolved server-side from the owning
 * user's configuration. Unknown or private portfolios are concealed with 404,
 * exactly like the public portfolio endpoint (Phase 7.4 policy). No API keys
 * or reporting credentials ever appear here.</p>
 */
@RestController
@RequestMapping("/api/v1/public/analytics")
@Tag(name = "Public Analytics", description = "Public tracking configuration for portfolio pages (no credentials).")
@RequiredArgsConstructor
@Slf4j
public class PublicAnalyticsController {

    private final UserAnalyticsConfigService userAnalyticsConfigService;

    @GetMapping("/{slug}/tracking-config")
    @Operation(summary = "Public tracking configuration for a portfolio (non-credential values only)")
    public ResponseEntity<ApiResponseWrapper<PublicAnalyticsConfigDto>> getTrackingConfig(
            @PathVariable String slug) {
        TrackingSettings settings = userAnalyticsConfigService.getPublicTrackingConfigForSlug(slug);
        return ResponseEntity.ok(new ApiResponseWrapper<>(new PublicAnalyticsConfigDto(
                settings.provider(),
                settings.enabled(),
                settings.scriptUrl(),
                settings.websiteId(),
                settings.recorderEnabled(),
                settings.recorderUrl())));
    }
}
