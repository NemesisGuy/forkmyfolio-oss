package com.forkmyfolio.integration;

import com.forkmyfolio.model.PortfolioProfile;
import com.forkmyfolio.model.User;
import com.forkmyfolio.service.PortfolioProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 *   <li>SEC (2026-11-24): an AUTHENTICATED request (real JWT through the
 *       production filter chain) always reports {@code X-Cache: BYPASS} and
 *       never reads nor writes the viewer-agnostic key — otherwise the
 *       owner-bypass view of a PRIVATE portfolio would be stored under
 *       {@code publicPortfolios::<slug>} and served to the next anonymous
 *       reader (the confirmed owner-view cache-poisoning leak).</li>
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
        // Phase 0.31 (H1): visibility is no longer part of the editor path —
        // createOrUpdateProfile never copies it, so no setters here.
        portfolioProfileService.createOrUpdateProfile(updates, owner);

        assertThat(redisTemplate.hasKey(CACHE_KEY_PREFIX + slug)).isFalse();

        mockMvc.perform(get("/api/v1/portfolios/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "MISS"))
                .andExpect(jsonPath("$.data.profile.headline").value(freshHeadline));
    }

    // ------------------------------------------------------------------
    // Account edit (@CacheEvict regression): 500 -> 200 + cache evicted
    // ------------------------------------------------------------------

    @Test
    void ownerAccountEdit_succeedsAndEvictsPublicPortfolioCache() throws Exception {
        String jwt = realLogin(owner.getEmail(), "Password123!");

        // Prime the shared cache with an anonymous view.
        mockMvc.perform(get("/api/v1/portfolios/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "MISS"));
        assertThat(redisTemplate.hasKey(CACHE_KEY_PREFIX + slug)).isTrue();

        // The real endpoint used by /manage/profile/edit-account. Before the
        // fix, Spring's stale @CacheEvict(key = "#currentUser.slug") threw
        // EL1007E and this returned HTTP 500 before the service body ran.
        mockMvc.perform(put("/api/v1/me")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Cache\",\"lastName\":\"Owner\","
                                + "\"profileImageUrl\":\"https://example.com/avatar.png\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Cache"));

        assertThat(redisTemplate.hasKey(CACHE_KEY_PREFIX + slug)).isFalse();
        mockMvc.perform(get("/api/v1/portfolios/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "MISS"));
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

    // ------------------------------------------------------------------
    // SEC (2026-11-24): owner-view cache poisoning — THE regression pin
    // ------------------------------------------------------------------

    @Test
    void ownerViewOfPrivatePortfolio_isServedButNeverCached_anonymousStillGets404() throws Exception {
        String privateSlug = "sec-private-" + runSuffix();
        User privateOwner = seedUser("sec-owner-" + runSuffix() + "@example.com", privateSlug, false);

        // 1. The owner authenticates through the REAL login endpoint and the
        //    request travels the production JWT filter chain (UserDetails
        //    principal), exactly like the confirmed DEV exploit.
        String jwt = realLogin(privateOwner.getEmail(), "Password123!");

        // 2. Owner view: 200 (Phase 7.4 owner-bypass) but X-Cache: BYPASS —
        //    the private payload must NOT be written to publicPortfolios::<slug>.
        mockMvc.perform(get("/api/v1/portfolios/{slug}", privateSlug)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "BYPASS"))
                .andExpect(jsonPath("$.status").value("success"));
        assertThat(redisTemplate.hasKey(CACHE_KEY_PREFIX + privateSlug)).isFalse();

        // 3. THE LEAK PIN: an anonymous reader immediately afterwards must get
        //    the indistinguishable 404 — never the owner's private payload.
        //    (MockMvc runs every request on this thread; a real container gives
        //    each request a fresh SecurityContextHolder, so clear explicitly to
        //    make the anonymous step genuinely anonymous.)
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/portfolios/{slug}", privateSlug))
                .andExpect(status().isNotFound());
        assertThat(redisTemplate.hasKey(CACHE_KEY_PREFIX + privateSlug)).isFalse();

        // 4. A second owner fetch must BYPASS again (no HIT could ever be
        //    served from a stored private payload).
        mockMvc.perform(get("/api/v1/portfolios/{slug}", privateSlug)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "BYPASS"));
        assertThat(redisTemplate.hasKey(CACHE_KEY_PREFIX + privateSlug)).isFalse();
    }

    @Test
    void authenticatedViewOfPublicPortfolio_alsoBypasses_anonymousMissHitCycleStillWorks() throws Exception {
        // An authenticated reader never populates the shared cache — even for a
        // PUBLIC slug — so the anonymous MISS → HIT read-through cycle (the
        // performance contract of Phase 0.20) stays intact and anonymous-only.
        String jwt = realLogin(owner.getEmail(), "Password123!");

        mockMvc.perform(get("/api/v1/portfolios/{slug}", slug)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "BYPASS"));
        assertThat(redisTemplate.hasKey(CACHE_KEY_PREFIX + slug)).isFalse();

        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/portfolios/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "MISS"));
        assertThat(redisTemplate.hasKey(CACHE_KEY_PREFIX + slug)).isTrue();

        mockMvc.perform(get("/api/v1/portfolios/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "HIT"));
    }
}
