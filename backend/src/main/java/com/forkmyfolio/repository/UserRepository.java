package com.forkmyfolio.repository;

import com.forkmyfolio.model.PublicPortfolioSummary;
import com.forkmyfolio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

        Optional<User> findByEmail(String email);

        Optional<User> findBySlug(String slug);

        @Query("SELECT u FROM User u LEFT JOIN FETCH u.portfolioProfile p WHERE u.slug = :slug AND u.active = true")
        Optional<User> findBySlugAndActiveTrue(@Param("slug") String slug);

        @Query("SELECT u FROM User u JOIN u.portfolioProfile p WHERE u.slug = :slug AND p.publiclyVisible = :isPublic AND u.active = true")
        Optional<User> findBySlugAndPortfolioProfileIsPublic(@Param("slug") String slug,
                        @Param("isPublic") boolean isPublic);

        /**
         * SEO sitemap source (v3.1.2): lean constructor-projection of every
         * ACTIVE user whose portfolio profile is publiclyVisible. Returns
         * slug + updatedAt ONLY — no personal data, no private portfolios.
         * Consumed by GET /api/v1/portfolios (guest) for the Nuxt sitemap.
         *
         * P10 (Workstream 2.4): additionally requires the owner's
         * searchIndexable preference, so sitemap entries can never expose a
         * portfolio whose owner opted out of search indexing. The global
         * NUXT_PUBLIC_INDEXABLE switch is enforced upstream (Nuxt serves a
         * 404/disallowed sitemap when it is off).
         */
        @Query("SELECT new com.forkmyfolio.model.PublicPortfolioSummary(u.slug, u.updatedAt) "
                        + "FROM User u JOIN u.portfolioProfile p "
                        + "WHERE p.publiclyVisible = true AND p.searchIndexable = true AND u.active = true "
                        + "ORDER BY u.slug ASC")
        List<PublicPortfolioSummary> findPublicPortfolioSummaries();

        boolean existsBySlug(String candidate);

        // process.
        @Query("SELECT DISTINCT u FROM User u " +
                        "LEFT JOIN FETCH u.portfolioProfile " +
                        "LEFT JOIN FETCH u.roles")
        List<User> findAllForBackup();

        // FIX: Replaces Cartesian-heavy 'FETCH' with lean profile fetch.
        // Relies on Hibernate batch fetching for child collections.
        @Query("SELECT DISTINCT u FROM User u " +
                        "LEFT JOIN FETCH u.portfolioProfile " +
                        "LEFT JOIN FETCH u.roles " +
                        "WHERE u.slug = :slug")
        Optional<User> findBySlugWithProfile(@Param("slug") String slug);

        Optional<User> findByUuid(UUID uuid);

        boolean existsByEmail(String email);

        // FIX: This query now eagerly fetches all related portfolio collections for a
        // specific user by email.
        @Query("SELECT u FROM User u " +
                        "LEFT JOIN FETCH u.portfolioProfile " +
                        "LEFT JOIN FETCH u.roles " +
                        "WHERE u.email = :email")
        Optional<User> findByEmailWithProfile(@Param("email") String email);
}