package com.forkmyfolio.controller.guest;

import com.forkmyfolio.advice.ApiResponseWrapper;
import com.forkmyfolio.aop.TrackVisitor;
import com.forkmyfolio.dto.response.PortfolioDto;
import com.forkmyfolio.dto.response.PublicPortfolioSummaryDto;
import com.forkmyfolio.mapper.PortfolioMapper;
import com.forkmyfolio.model.PublicPortfolioSummary;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.UserSkill;
import com.forkmyfolio.model.enums.VisitorStatType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forkmyfolio.service.PortfolioService;
import com.forkmyfolio.service.UserSkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portfolios")
@Tag(name = "Public Portfolios", description = "Endpoints for viewing public user portfolios.")
@RequiredArgsConstructor
@Slf4j
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioMapper portfolioMapper;
    private final UserSkillService userSkillService;
    private final com.forkmyfolio.service.impl.VisitorStatsService visitorStatsService;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;

    /** Cache name shared with every mutation path's {@code @CacheEvict}. */
    private static final String PORTFOLIO_CACHE = "publicPortfolios";

    @GetMapping("/{slug}")
    @Operation(summary = "Get a user's public portfolio by their slug",
            description = "Retrieves the full public sections of a user's portfolio. "
                    + "Served through the Redis read-through cache (Phase 0.20, response-body "
                    + "caching Phase 0.23): the X-Cache response header reports HIT (served from "
                    + "cache), MISS (assembled and now cached) or BYPASS (cache not applicable). "
                    + "A private portfolio is answered with a plain 404, indistinguishable from "
                    + "an unknown slug.")
    @TrackVisitor(VisitorStatType.TOTAL_VISITS)
    public ResponseEntity<byte[]> getPortfolioBySlug(
            @Parameter(description = "The unique, URL-friendly slug of the user.", example = "jane-doe") @PathVariable String slug) {

        visitorStatsService.incrementPortfolioView(slug);

        // Phase 0.20 — read-through cache at the controller boundary (NAG-01:
        // DTO assembly is a controller responsibility). Phase 0.23 upgrade:
        // the cache unit is now the FULLY SERIALIZED response JSON, not the
        // DTO tree. Rationale: the capacity rerun showed the backend pinned
        // at ~70 req/s = exactly its 1-CPU x ~14 ms per-request cost, and the
        // dominant component of that cost was Jackson re-serializing the SAME
        // 99 KB DTO tree for every request — the cache avoided reassembly but
        // not re-serialization. Storing the exact wire bytes (identical shape:
        // same ApiResponseWrapper JSON, same ObjectMapper the MVC layer uses)
        // makes a HIT a straight memcpy to the socket, with the single
        // remaining per-request work being the Spring Security filter chain.
        // The key space is UNCHANGED ("publicPortfolios::slug"), so every
        // @CacheEvict in the mutation paths keeps working exactly as before;
        // the String cache unit simply supersedes any PortfolioDto objects
        // cached by the previous release (they are re-assembled and rewritten
        // as JSON on the next MISS).
        Cache cache = cacheManager.getCache(PORTFOLIO_CACHE);

        byte[] responseBody = null;
        String cacheOutcome;
        if (cache != null) {
            Cache.ValueWrapper wrapper;
            try {
                wrapper = cache.get(slug);
            } catch (RuntimeException e) {
                // A Redis hiccup must degrade to DB assembly, never fail the view.
                log.warn("Cache read failed for slug '{}' — falling back to assembly: {}", slug, e.getMessage());
                wrapper = null;
            }
            if (wrapper != null && wrapper.get() instanceof String cachedJson) {
                responseBody = cachedJson.getBytes(StandardCharsets.UTF_8);
                cacheOutcome = "HIT"; // zero assembly AND zero serialization work
            } else {
                cacheOutcome = "MISS";
            }
        } else {
            cacheOutcome = "BYPASS";
        }

        if (responseBody == null) {
            User user = portfolioService.getPublicPortfolioUserBySlug(slug);
            Map<UUID, UserSkill> skillLookup = userSkillService.getUserSkillLookupMap(user);
            PortfolioDto portfolioDto = portfolioMapper.toDto(user, skillLookup);
            String json;
            try {
                json = objectMapper.writeValueAsString(new ApiResponseWrapper<>(portfolioDto));
            } catch (JsonProcessingException e) {
                // Never expected for generated DTO trees; if it ever happens,
                // fail loudly rather than cache a broken representation.
                throw new IllegalStateException("Failed to serialize public portfolio response for slug '" + slug + "'", e);
            }
            responseBody = json.getBytes(StandardCharsets.UTF_8);
            if (cache != null) {
                try {
                    cache.put(slug, json);
                } catch (RuntimeException e) {
                    log.warn("Cache write failed for slug '{}' — serving uncached response: {}", slug, e.getMessage());
                }
            }
        }

        log.debug("Public portfolio '{}' served with X-Cache: {}", slug, cacheOutcome);
        return ResponseEntity.ok()
                .header("X-Cache", cacheOutcome)
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseBody);
    }

    /**
     * SEO sitemap source (v3.1.2). Lists the slugs of all publicly visible,
     * active portfolios. Deliberately lean — slug + updatedAt only, no
     * personal data — so it is safe to expose unauthenticated and to consume
     * from the Nuxt SSR sitemap generator. Private portfolios are never
     * included (repository-level filter on publiclyVisible + active).
     */
    @GetMapping
    @Operation(summary = "List public portfolio slugs", description = "Returns the slugs and last-update timestamps of all publicly visible portfolios. Used for sitemap generation; contains no personal data.")
    public ResponseEntity<ApiResponseWrapper<List<PublicPortfolioSummaryDto>>> listPublicPortfolios() {
        List<PublicPortfolioSummary> summaries = portfolioService.getPublicPortfolioSummaries();
        List<PublicPortfolioSummaryDto> dtos = summaries.stream()
                .map(summary -> new PublicPortfolioSummaryDto(summary.slug(), summary.updatedAt()))
                .toList();
        return ResponseEntity.ok(new ApiResponseWrapper<>(dtos));
    }
}
