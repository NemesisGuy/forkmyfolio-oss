package com.forkmyfolio.integration;

import com.forkmyfolio.model.RefreshToken;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.Role;
import com.forkmyfolio.repository.RefreshTokenRepository;
import com.forkmyfolio.service.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrent logins for the SAME user must never fail with HTTP 500.
 *
 * <p>{@code createRefreshToken} used to invalidate the previous token with a
 * load-then-entity-delete. When two logins raced, both loaded the same row and
 * the loser's {@code DELETE} affected 0 rows — Hibernate surfaces that as
 * {@code ObjectOptimisticLockingFailureException}
 * ("delete from refresh_tokens where id=?") and the whole login 500'd
 * (e2e run D: visual-proof, which logs in as admin while other specs do the
 * same under {@code fullyParallel}).
 *
 * <p>The fix replaces that with the idempotent bulk delete
 * ({@code DELETE ... WHERE user_id = ?}); a 0-row bulk delete is a success, so
 * both logins complete. This test runs both calls against the REAL MySQL
 * container (no mocks) and pins:
 * <ol>
 *   <li>neither concurrent call throws;</li>
 *   <li>the transient duplicate (if any) self-heals to exactly one active
 *       token on the next rotation.</li>
 * </ol>
 *
 * <p>Deliberately NOT {@code @Transactional}: the race only exists between two
 * committed transactions, exactly like production logins.
 */
public class RefreshTokenConcurrencyIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void concurrentCreateRefreshToken_forSameUser_bothSucceed() throws Exception {
        String suffix = runSuffix();
        User owner = seedUser("rt-concurrency-" + suffix + "@example.com",
                "rt-conc-" + suffix, false, Role.USER);

        // An already-active session token — the row both concurrent logins race
        // to replace.
        assertThat(refreshTokenService.createRefreshToken(owner)).isNotNull();

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<RefreshToken>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return refreshTokenService.createRefreshToken(owner);
            }));
        }
        start.countDown();

        try {
            for (Future<RefreshToken> future : futures) {
                // With the old implementation this rethrew
                // ObjectOptimisticLockingFailureException (wrapped) — the exact
                // shape of the login 500 seen in e2e run D.
                RefreshToken token = future.get(30, TimeUnit.SECONDS);
                assertThat(token).as("both concurrent logins must produce a token").isNotNull();
                assertThat(token.getToken()).isNotBlank();
            }
        } finally {
            pool.shutdownNow();
        }

        // Self-healing: one more sequential rotation collapses any transient
        // duplicate down to exactly one active token per user.
        assertThat(refreshTokenService.createRefreshToken(owner)).isNotNull();

        long activeTokens = refreshTokenRepository.findAll().stream()
                .filter(t -> owner.getId().equals(t.getUser().getId()))
                .count();
        assertThat(activeTokens)
                .as("exactly one active refresh token per user once concurrent logins settle")
                .isEqualTo(1);
    }
}
