package com.forkmyfolio.service.impl;

import com.forkmyfolio.config.AppProperties;
import com.forkmyfolio.model.RefreshToken;
import com.forkmyfolio.model.User;
import com.forkmyfolio.repository.RefreshTokenRepository;
import com.forkmyfolio.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression tests for concurrent-login refresh-token creation.
 *
 * <p>Two logins for the same user (two tabs/devices, or parallel e2e specs)
 * used to race inside {@code createRefreshToken}: both loaded the same token
 * row, and the loser's entity-level delete affected 0 rows — Hibernate reports
 * that as {@code ObjectOptimisticLockingFailureException}
 * ("delete from refresh_tokens where id=?") and the login failed with HTTP 500
 * (run D: visual-proof).
 *
 * <p>Pin: invalidation MUST go through the idempotent bulk delete
 * ({@code DELETE ... WHERE user_id = ?}), never a load-then-entity-delete.
 *
 * <p>v1.2.11: also pins the deadlock retry — two fresh registrations used to
 * deadlock inside the create transaction (deleteByUser gap lock at the
 * {@code user_id} index supremum vs. a parallel insert-intention) and the
 * loser's {@code /auth/register} returned HTTP 500. The retry must run the
 * whole unit in a FRESH transaction (programmatic template, not
 * {@code @Transactional}) and must NOT swallow genuine data errors.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserRepository userRepository;
    // No stubbing on purpose: the TransactionTemplate only needs a non-null
    // manager; getTransaction/commit/rollback are void/default no-ops on the
    // mock, and Mockito strict stubs would flag an unused stub here.
    @Mock
    private PlatformTransactionManager transactionManager;

    private RefreshTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getJwt().setRefreshExpirationMs(604_800_000L); // 7 days
        service = new RefreshTokenServiceImpl(refreshTokenRepository, userRepository, appProperties, transactionManager);
    }

    @Test
    void createRefreshToken_usesIdempotentBulkDelete_insteadOfEntityDelete() {
        User user = new User();
        user.setId(42L);

        when(refreshTokenRepository.existsByUser(any(User.class))).thenReturn(true);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        RefreshToken created = service.createRefreshToken(user);

        assertNotNull(created, "A refresh token must be created for the login");
        assertNotNull(created.getToken(), "The token string must be populated");
        assertNotNull(created.getExpiryDate(), "The expiry must be populated");
        assertTrue(created.getExpiryDate().isAfter(java.time.Instant.now()),
                "The expiry must be in the future");
        org.junit.jupiter.api.Assertions.assertSame(user, created.getUser());

        // The regression pin: invalidation must be the bulk DML delete. The old
        // findByToken(...).ifPresent(delete) loaded the row into the persistence
        // context, and a concurrent second login's delete of the SAME row then
        // affected 0 rows -> ObjectOptimisticLockingFailureException -> login 500.
        verify(refreshTokenRepository).deleteByUser(user);
        verify(refreshTokenRepository, never()).findByUser(any());
        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
        verify(refreshTokenRepository, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteByToken_usesBulkDelete_notEntityDelete() {
        // Logout path: same race class as createRefreshToken — a token already
        // invalidated by a concurrent rotation must not turn the logout into a
        // 0-row entity delete (ObjectOptimisticLockingFailureException).
        service.deleteByToken("some-token");

        verify(refreshTokenRepository).deleteByToken("some-token");
        verify(refreshTokenRepository, never()).findByToken(anyString());
        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }

    // ------------------------------------------------------------------
    // v1.2.11: deadlock retry (two parallel registrations / logins)
    // ------------------------------------------------------------------

    /** The exact exception shape MySQL error 1213 surfaces as. */
    private static DeadlockLoserDataAccessException deadlockVictim() {
        return new DeadlockLoserDataAccessException(
                "Deadlock found when trying to get lock; try restarting transaction",
                new SQLException("Deadlock found when trying to get lock; try restarting transaction",
                        "40001", 1213));
    }

    @Test
    void createRefreshToken_retriesTheWholeUnitAfterADeadlock_andSucceeds() {
        User user = new User();
        user.setId(42L);

        when(refreshTokenRepository.existsByUser(any(User.class))).thenReturn(true);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenThrow(deadlockVictim())
                .thenAnswer(invocation -> {
                    RefreshToken token = invocation.getArgument(0);
                    token.setId(1L);
                    return token;
                });

        RefreshToken created = service.createRefreshToken(user);

        assertNotNull(created, "the retry must produce a token instead of rethrowing the deadlock");
        assertNotNull(created.getToken(), "the retried insert must populate the token");
        // The retry re-runs the WHOLE transactional unit in a fresh
        // transaction: delete (rolled back with the victim) + insert.
        verify(refreshTokenRepository, times(2)).deleteByUser(user);
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void createRefreshToken_doesNotRetryAGenuineDataError() {
        User user = new User();
        user.setId(42L);

        when(refreshTokenRepository.existsByUser(any(User.class))).thenReturn(true);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenThrow(new DataIntegrityViolationException("Column 'token' cannot be null"));

        assertThrows(DataIntegrityViolationException.class, () -> service.createRefreshToken(user),
                "a non-lock data error must surface immediately, not be retried");

        verify(refreshTokenRepository, times(1)).deleteByUser(user);
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void createRefreshToken_givesUpAfterThreeAttempts_andRethrowsTheDeadlock() {
        User user = new User();
        user.setId(42L);

        when(refreshTokenRepository.existsByUser(any(User.class))).thenReturn(true);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenThrow(deadlockVictim());

        assertThrows(DeadlockLoserDataAccessException.class, () -> service.createRefreshToken(user),
                "the retry loop must be bounded and rethrow after the last attempt");

        // Bounded: exactly CREATE_MAX_ATTEMPTS (3) transactional units, no
        // infinite loop against a database that keeps deadlocking.
        verify(refreshTokenRepository, times(3)).deleteByUser(user);
        verify(refreshTokenRepository, times(3)).save(any(RefreshToken.class));
    }

    @Test
    void verifyExpiration_deactivatedUser_throwsAndDeletesToken() {
        // v1.2.11 SECURITY: a token that survived deactivation must never
        // mint a fresh access token for an inactive account.
        User user = new User();
        user.setId(42L);
        user.setActive(false);
        RefreshToken token = new RefreshToken();
        token.setToken("inactive-user-token");
        token.setUser(user);
        token.setExpiryDate(java.time.Instant.now().plusMillis(60_000));

        com.forkmyfolio.exception.TokenRefreshException ex = assertThrows(
                com.forkmyfolio.exception.TokenRefreshException.class,
                () -> service.verifyExpiration(token));

        assertTrue(ex.getMessage().contains("deactivated"));
        // No delete inside a non-transactional verification path — revocation
        // happens at deactivation time, not here (would 500 on bulk DML).
        verify(refreshTokenRepository, never()).deleteByToken(anyString());
        verify(refreshTokenRepository, never()).delete(token);
    }

    @Test
    void createRefreshToken_skipsTheDelete_forATokenlessUser() {
        // v1.2.11 ROOT CAUSE pin: a fresh registration's user has no tokens —
        // the invalidation DELETE must be skipped entirely. deleteByUser on a
        // token-less user range-scans user_id to the index supremum and holds
        // that gap X-lock until commit, deadlocking against a parallel
        // registration's INSERT (MySQL error 1213 -> HTTP 500 on
        // /auth/register). Mockito's existsByUser default is false = token-less.
        User user = new User();
        user.setId(42L);

        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        RefreshToken created = service.createRefreshToken(user);

        assertNotNull(created, "a token must still be created for the fresh user");
        assertNotNull(created.getToken());
        verify(refreshTokenRepository, never()).deleteByUser(any(User.class));
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }
}
