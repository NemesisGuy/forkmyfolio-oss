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
import org.springframework.cache.annotation.Cacheable;
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

    @GetMapping("/{slug}")
    @Operation(summary = "Get a user's public portfolio by their slug", description = "Retrieves the full public sections of a user's portfolio.")
    @TrackVisitor(VisitorStatType.TOTAL_VISITS)
    public ResponseEntity<ApiResponseWrapper<PortfolioDto>> getPortfolioBySlug(
            @Parameter(description = "The unique, URL-friendly slug of the user.", example = "jane-doe") @PathVariable String slug) {

        log.debug("API request to fetch public portfolio for slug: '{}' (Cache Miss/Bypass via Controller)", slug);
        visitorStatsService.incrementPortfolioView(slug);

        User user = portfolioService.getPublicPortfolioUserBySlug(slug);
        Map<UUID, UserSkill> skillLookup = userSkillService.getUserSkillLookupMap(user);
        PortfolioDto portfolioDto = portfolioMapper.toDto(user, skillLookup);

        return ResponseEntity.ok(new ApiResponseWrapper<>(portfolioDto));
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
