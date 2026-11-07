package com.forkmyfolio.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Minimal public summary of an indexable portfolio. Used exclusively by the
 * SSR sitemap generator: it intentionally contains NO personal data beyond
 * the public slug — only portfolios whose profile is publiclyVisible AND
 * whose account is active are returned by the repository query that builds
 * these.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PublicPortfolioSummaryDto", description = "Minimal public summary of an indexable portfolio (slug + last-updated only). Contains no personal information.")
public class PublicPortfolioSummaryDto {

    @Schema(description = "The public URL slug of the portfolio owner.", example = "jane-doe")
    private String slug;

    /**
     * When the owner's account was last updated (used as the sitemap <lastmod>
     * hint). Phase 10 fix: typed as {@link Instant} to match {@code User.updatedAt}
     * exactly — Hibernate 6 dynamic instantiation rejects the previous
     * LocalDateTime signature (Instant is not assignment-compatible), which
     * failed repository query validation at context startup.
     */
    @Schema(description = "When the owner's account was last updated (used as the sitemap <lastmod> hint).")
    private Instant updatedAt;
}
