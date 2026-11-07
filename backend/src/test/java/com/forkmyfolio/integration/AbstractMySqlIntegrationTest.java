package com.forkmyfolio.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.forkmyfolio.model.PortfolioProfile;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.AuthProvider;
import com.forkmyfolio.model.enums.Role;
import com.forkmyfolio.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared base for all integration tests that require a real, prod-compatible
 * environment.
 *
 * <p>
 * Provides disposable, isolated Docker containers (disposable per test run,
 * reused across the whole test JVM):
 * <ul>
 *   <li>{@code mysql:8.0} — the exact production image used in
 *       docker-compose.staging.v3.nginx.yml; real Flyway migrations
 *       (V1-V8) run against it, exactly as in production.</li>
 *   <li>{@code redis:7-alpine} — the production cache stack; needed because
 *       {@code @CacheEvict("publicPortfolios")} runs on every portfolio
 *       mutation and requires a reachable Redis.</li>
 * </ul>
 *
 * <p>
 * H2 is intentionally NOT used anywhere anymore: H2-in-MySQL-mode silently
 * deviates from production (no real migrations, different type semantics,
 * different constraint enforcement), so integration tests must never fall
 * back to it. If Docker is unavailable, tests fail and the limitation is
 * reported instead of pretending success in a fake environment.
 *
 * <p>
 * Container lifecycle: containers are static JVM singletons started from a
 * static initializer, shared by every subclass, and stopped when the JVM
 * ends (Ryuk clean-up). This keeps container start-up cost (~30-60 s once)
 * off each test class.
 */
@SpringBootTest(properties = {
        // Never run classpath data.sql: it is dev-seeding data, and the prod
        // schema is fully owned by Flyway here (matches prod, where MySQL is
        // not an "embedded" database so data.sql would not run either).
        "spring.sql.init.mode=never",
        // Prod-parity schema management: Flyway migrations only, no Hibernate DDL.
        "spring.jpa.hibernate.ddl-auto=none",
        // Test-only values (NOT secrets): the main application.properties no
        // longer ships insecure defaults, so integration contexts must supply
        // an explicit signing key here — valid Base64 decoding to 64 raw
        // bytes (HS512 requirement), exactly like production values.
        "app.jwt.secret=dGVzdC1vbmx5LXNpZ25pbmcta2V5LTAxMjM0NTY3ODlhYmNkZWYwMTIzNDU2Nzg5YWJjZGVmMDEyMzQ1Njc4OQ==",
        "app.admin.password=test-only-admin-password"
})
@AutoConfigureMockMvc
public abstract class AbstractMySqlIntegrationTest {

    /** Production-compatible MySQL image (see docker-compose.staging.v3.nginx.yml). */
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
            DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("forkmyfolio_db")
            .withUsername("test")
            .withPassword("test");

    /** Production-compatible Redis image used for the cache manager. */
    private static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    static {
        REDIS.start();
        MYSQL.start();
    }

    /**
     * Points the application at the containers instead of any real database,
     * and makes the OAuth2 client registration hermetic (otherwise the
     * context needs GOOGLE_CLIENT_ID etc. from the environment).
     */
    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

        registry.add("spring.security.oauth2.client.registration.google.client-id", () -> "test-google-client-id");
        registry.add("spring.security.oauth2.client.registration.google.client-secret", () -> "test-google-client-secret");
        registry.add("spring.security.oauth2.client.registration.github.client-id", () -> "test-github-client-id");
        registry.add("spring.security.oauth2.client.registration.github.client-secret", () -> "test-github-client-secret");

        // DEF-1: analytics credential encryption must be available in tests so
        // secret-bearing configuration can be exercised end to end.
        registry.add("app.analytics.encryption-key", () -> TEST_ANALYTICS_KEY_BASE64);
    }

    /** 32 bytes of deterministic test key material, Base64-encoded. */
    private static final String TEST_ANALYTICS_KEY_BASE64 =
            java.util.Base64.getEncoder().encodeToString(new byte[]{
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                    16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31
            });

    // ------------------------------------------------------------------
    // Shared test plumbing (real HTTP login + user seeding)
    // ------------------------------------------------------------------

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    /**
     * Performs a REAL HTTP login through {@code POST /api/v1/auth/login} and
     * returns the JWT access token. Integration tests must authenticate with
     * real tokens, never {@code @WithMockUser} fakes.
     */
    protected String realLogin(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    /**
     * Seeds a persistent user (with an encoded password + portfolio profile)
     * exactly the way production data is shaped, and returns the persisted
     * entity. Email and slug get a unique run suffix to stay independent of
     * test-ex execution order on the shared container database.
     */
    protected User seedUser(String baseEmail, String baseSlug, boolean publiclyVisible, Role... roles) {
        User user = User.builder()
                .firstName("Owner")
                .lastName("Seed")
                .email(baseEmail)
                .password(passwordEncoder.encode("Password123!"))
                .slug(baseSlug)
                .provider(AuthProvider.LOCAL)
                // Mutable set: Hibernate's merge path calls clear() on the
                // roles collection when a seeded user is re-saved; an
                // immutable Set.of(...) would throw UnsupportedOperationException.
                .roles(new java.util.HashSet<>(Set.of(roles.length > 0 ? roles[0] : Role.USER)))
                .active(true)
                .emailVerified(true)
                .build();

        PortfolioProfile profile = new PortfolioProfile();
        profile.setUser(user);
        profile.setPubliclyVisible(publiclyVisible);
        profile.setVisible(true);
        profile.setHeadline("Headline of " + baseSlug);
        profile.setSummary("Summary of " + baseSlug);
        user.setPortfolioProfile(profile);

        return userRepository.save(user);
    }

    /** Unique per-test-run suffix so class ordering can never collide. */
    protected String runSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
