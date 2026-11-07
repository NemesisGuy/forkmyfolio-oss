package com.forkmyfolio.model;

import java.time.Instant;

/**
 * Read-model projection for the public sitemap endpoint.
 *
 * <p>Carries only the fields needed to build a sitemap entry: the portfolio
 * slug and the last time the owning user (and therefore the portfolio) was
 * modified. Lives in the domain layer so that repositories and services can
 * return it without violating the architecture rules (NAG-01); the guest
 * controller maps it to {@code PublicPortfolioSummaryDto} at the API boundary.
 *
 * <p>Populated via JPQL constructor expression in
 * {@code UserRepository.findPublicPortfolioSummaries()}.
 *
 * @param slug      the public URL slug of the portfolio
 * @param updatedAt last modification timestamp of the owning user
 */
public record PublicPortfolioSummary(String slug, Instant updatedAt) {
}
