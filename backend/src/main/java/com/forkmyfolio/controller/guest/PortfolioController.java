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
import com.forkmyfolio.service.PortfolioService;
import com.forkmyfolio.service.UserSkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    /** Cache name shared with every mutation path's {@code @CacheEvict}. */
    private static final String PORTFOLIO_CACHE = "publicPortfolios";

    @GetMapping("/{slug}")
    @Operation(summary = "Get a user's public portfolio by their slug",
            description = "Retrieves the full public sections of a user's portfolio. "
                    + "Served through the Redis read-through cache (Phase 0.20): the X-Cache response "
                    + "header reports HIT (served from cache), MISS (assembled and now cached) "
                    + "or BYPASS (cache not applicable). A private portfolio is answered with a "
                    + "plain 404, indistinguishable from an unknown slug.")
    @TrackVisitor(VisitorStatType.TOTAL_VISITS)
    public ResponseEntity<ApiResponseWrapper<PortfolioDto>> getPortfolioBySlug(
            @Parameter(description = "The unique, URL-friendly slug of the user.", example = "jane-doe") @PathVariable String slug) {

        visitorStatsService.incrementPortfolioView(slug);

        // Phase 0.20 — read-through cache at the controller boundary (NAG-01:
        // DTO assembly is a controller responsibility, and the DTO is also the
        // cache unit — assembling it twice under load is exactly the cost the
        // cache exists to avoid). Manual get()/put() on the same cache entry
        // ("publicPortfolios::slug") keeps every @CacheEvict valid unchanged,
        // and lets the X-Cache header reflect what ACTUALLY happened.
        Cache cache = cacheManager.getCache(PORTFOLIO_CACHE);

        PortfolioDto portfolioDto = null;
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
            if (wrapper != null && wrapper.get() instanceof PortfolioDto cached) {
                portfolioDto = cached;
                cacheOutcome = "HIT"; // zero assembly work: services and MySQL untouched
            } else {
                cacheOutcome = "MISS";
            }
        } else {
            cacheOutcome = "BYPASS";
        }

        if (portfolioDto == null) {
            User user = portfolioService.getPublicPortfolioUserBySlug(slug);
            Map<UUID, UserSkill> skillLookup = userSkillService.getUserSkillLookupMap(user);
            portfolioDto = portfolioMapper.toDto(user, skillLookup);
            if (cache != null) {
                try {
                    cache.put(slug, portfolioDto);
                } catch (RuntimeException e) {
                    log.warn("Cache write failed for slug '{}' — serving uncached response: {}", slug, e.getMessage());
                }
            }
        }

        log.debug("Public portfolio '{}' served with X-Cache: {}", slug, cacheOutcome);
        return ResponseEntity.ok()
                .header("X-Cache", cacheOutcome)
                .body(new ApiResponseWrapper<>(portfolioDto));
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
