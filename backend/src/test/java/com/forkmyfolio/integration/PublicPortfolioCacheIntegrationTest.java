package com.forkmyfolio.integration;

import com.forkmyfolio.model.PortfolioProfile;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.PortfolioProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 0.20 — proves the public-portfolio READ-THROUGH cache end to end on
 * the production stack (real redis:7-alpine from {@link AbstractMySqlIntegrationTest}).
 *
 * Contract under test:
 * <ul>
 *   <li>The FIRST public view assembles from MySQL and reports {@code X-Cache: MISS},
 *       and the finished DTO is stored under {@code publicPortfolios::<slug>} in Redis
 *       for the configured TTL.</li>
 *   <li>Subsequent views of the same slug report {@code X-Cache: HIT} and are served
 *       from Redis without re-running the assembly pipeline.</li>
 *   <li>A real EDITOR-path change (createOrUpdateProfile) evicts the entry through
 *       the existing {@code @CacheEvict} wiring — the next view is a MISS again and
 *       serves the FRESH data. No stale reads are possible.</li>
 *   <li>Unknown and private slugs are never cached (nothing is written on a 404).</li>
 * </ul>
 */
public class PublicPortfolioCacheIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String CACHE_KEY_PREFIX = "publicPortfolios::";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PortfolioProfileService portfolioProfileService;

    private String slug;
    private User owner;

    @BeforeEach
    void seedOwnerAndClearCache() {
        // The Redis container is a static JVM single shared by the whole test
        // battery; clear the portfolio cache store so MISS/HIT semantics are
        // never corrupted by leftovers from another test class.
        redisTemplateConnectionFlush();

        String suffix = runSuffix();
        slug = "cache-seed-" + suffix;
        owner = seedUser("cache-owner-" + suffix + "@example.com", slug, true);
    }

    private void redisTemplateConnectionFlush() {
        redisTemplate.execute((org.springframework.data.redis.connection.RedisConnection connection) -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    // ------------------------------------------------------------------
    // MISS -> stored in Redis -> HIT
    // ------------------------------------------------------------------

    @Test
    void firstViewIsMiss_thenSubsequentViewsAreHits_servedFromRedis() throws Exception {
        mockMvc.perform(get("/api/v1/portfolios/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "MISS"))
                .andExpect(jsonPath("$.status").value("success"));

        // The value MUST now live in Redis under the exact documented key.
        assertThat(redisTemplate.hasKey(CACHE_KEY_PREFIX + slug)).isTrue();

        mockMvc.perform(get("/api/v1/portfolios/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "HIT"));
    }

    // ------------------------------------------------------------------
    // Editor-path mutation evicts; next view serves FRESH data
    // ------------------------------------------------------------------

    @Test
    void editorPathEviction_nextViewMissesAndServesFreshHeadline() throws Exception {
        mockMvc.perform(get("/api/v1/portfolios/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "MISS"));
        mockMvc.perform(get("/api/v1/portfolios/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "HIT"));

        // Real mutation through the service bean (proxy): its @CacheEvict on
        // "publicPortfolios" keyed by #user.slug must run exactly as in prod.
        String freshHeadline = "Runway Fresh Headline " + System.nanoTime();
        PortfolioProfile updates = new PortfolioProfile();
        updates.setHeadline(freshHeadline);
        updates.setSummary("Fresh summary (Phase 0.20 eviction proof).");
        updates.setVisible(true);
        updates.setPubliclyVisible(true);
        portfolioProfileService.createOrUpdateProfile(updates, owner);

        assertThat(redisTemplate.hasKey(CACHE_KEY_PREFIX + slug)).isFalse();

        mockMvc.perform(get("/api/v1/portfolios/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "MISS"))
                .andExpect(jsonPath("$.data.profile.headline").value(freshHeadline));
    }

    // ------------------------------------------------------------------
    // Fail-closed: 404s never populate the cache
    // ------------------------------------------------------------------

    @Test
    void unknownSlug_is404_andWritesNothingToRedis() throws Exception {
        String ghost = "ghost-" + runSuffix();
        mockMvc.perform(get("/api/v1/portfolios/{slug}", ghost))
                .andExpect(status().isNotFound());
        assertThat(redisTemplate.hasKey(CACHE_KEY_PREFIX + ghost)).isFalse();
    }

    @Test
    void privatePortfolio_404ForGuests_andNeverCached() throws Exception {
        String privateSlug = "private-" + runSuffix();
        owner = seedUser("private-owner-" + runSuffix() + "@example.com", privateSlug, false);

        mockMvc.perform(get("/api/v1/portfolios/{slug}", privateSlug))
                .andExpect(status().isNotFound());
        assertThat(redisTemplate.hasKey(CACHE_KEY_PREFIX + privateSlug)).isFalse();
    }
}
