package com.forkmyfolio.service.impl;

import com.forkmyfolio.config.AppProperties;
import com.forkmyfolio.exception.TokenRefreshException;
import com.forkmyfolio.model.RefreshToken;
import com.forkmyfolio.model.User;
import com.forkmyfolio.repository.RefreshTokenRepository;
import com.forkmyfolio.repository.UserRepository;
import com.forkmyfolio.service.RefreshTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of the {@link RefreshTokenService} interface.
 */
@Slf4j
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    /**
     * Total attempts for {@link #createRefreshToken(User)} (1 initial + 2
     * retries). One InnoDB deadlock victim is rolled back and the lock conflict
     * resolves immediately, so two retries are ample.
     */
    private static final int CREATE_MAX_ATTEMPTS = 3;

    /** MySQL error 1213 — deadlock victim; the row is already rolled back. */
    private static final int MYSQL_ERROR_DEADLOCK = 1213;
    /** MySQL error 1205 — lock wait timeout (same retry rationale). */
    private static final int MYSQL_ERROR_LOCK_WAIT_TIMEOUT = 1205;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final Long refreshTokenDurationMs;
    private final TransactionTemplate transactionTemplate;

    /**
     * Constructs a {@code RefreshTokenServiceImpl}.
     *
     * @param refreshTokenRepository Repository for refresh token data.
     * @param userRepository         Repository for user data.
     * @param appProperties          Application configuration properties.
     * @param transactionManager     Transaction manager — used programmatically
     *                               by {@link #createRefreshToken(User)} so each
     *                               deadlock retry starts a FRESH transaction
     *                               (a deadlocked one is fully rolled back by
     *                               InnoDB and marked rollback-only).
     */
    @Autowired
    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository,
                                  AppProperties appProperties, PlatformTransactionManager transactionManager) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.refreshTokenDurationMs = appProperties.getJwt().getRefreshExpirationMs();
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByTokenAndFetchUserWithRoles(token);
    }

    /**
     * {@inheritDoc}
     * This implementation invalidates any existing refresh tokens for the user
     * before creating a new one, enforcing a one-active-refresh-token-per-user
     * policy.
     *
     * <p>Concurrency: the invalidation MUST be the idempotent bulk delete
     * ({@code DELETE ... WHERE user_id = ?}), never a load-then-entity-delete.
     * Two logins for the same user (two tabs/devices, or parallel e2e specs)
     * used to load the same row and race: the loser's entity delete affected 0
     * rows, which Hibernate reports as {@code ObjectOptimisticLockingFailureException}
     * ("delete from refresh_tokens where id=?") and the login failed with HTTP
     * 500 (run D: visual-proof). Bulk DML treats a 0-row delete as success, so
     * concurrent logins both complete; the newest token wins and the transient
     * extra row (if any) is cleared by the next rotation.
     *
     * <p>Deadlock prevention + retry (v1.2.11): two FRESH registrations (or
     * logins) used to deadlock inside this method's single transaction —
     * {@code deleteByUser} on a user with no tokens range-scans the
     * {@code user_id} index up to the supremum and holds that gap X-lock until
     * commit, while the parallel transaction's {@code INSERT} needs the very
     * same gap (insert-intention). InnoDB rolls one victim back with error
     * 1213 and the caller saw HTTP 500 on {@code POST /auth/register}
     * (v1.2.11 guest-pass e2e sweep: 4 parallel registrations). Two layers
     * fix it:
     * <ol>
     *   <li><b>Root cause:</b> token-less users skip the DELETE entirely
     *       (non-locking {@code existsByUser} probe) — with no gap lock taken,
     *       parallel inserts at the index end don't conflict at all;</li>
     *   <li><b>Safety net:</b> any residual transient lock failure is retried
     *       up to {@link #CREATE_MAX_ATTEMPTS} times, each attempt in a NEW
     *       transaction — a deadlocked transaction is rolled back completely
     *       and marked rollback-only, so the retry must not run inside it
     *       (hence the programmatic {@link TransactionTemplate} instead of
     *       {@code @Transactional}). This method is the outermost transactional
     *       boundary: the auth controllers and the OAuth2 success handler call
     *       it directly, never from inside another transaction.</li>
     * </ol>
     *
     * <p>Only transient lock failures are retried; genuine data errors
     * (constraint violations, connectivity) surface immediately after the
     * first attempt, unchanged from before.
     */
    @Override
    public RefreshToken createRefreshToken(User user) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                return transactionTemplate.execute(status -> createRefreshTokenInNewTransaction(user));
            } catch (RuntimeException ex) {
                if (attempt >= CREATE_MAX_ATTEMPTS || !isTransientLockFailure(ex)) {
                    throw ex;
                }
                log.warn("Transient database lock conflict while creating a refresh token for user {} "
                        + "(attempt {}/{}); retrying in a fresh transaction", user.getId(), attempt,
                        CREATE_MAX_ATTEMPTS, ex);
            }
        }
    }

    /**
     * The transactional unit of {@link #createRefreshToken(User)}: invalidate
     * the user's tokens (only if they have any), then insert the new one. Runs
     * inside the {@link TransactionTemplate} so every retry attempt gets its
     * own transaction.
     */
    private RefreshToken createRefreshTokenInNewTransaction(User user) {
        // v1.2.11 root cause: SKIP the invalidation when the user has no
        // tokens. deleteByUser on a token-less user range-scans user_id to the
        // index supremum and holds that gap X-lock until commit — the exact
        // gap a parallel registration's INSERT needs (insert-intention), so
        // two fresh registrations deadlocked (MySQL 1213) and one
        // /auth/register 500'd. The exists probe is a non-locking consistent
        // read (no gap locks under RR), and a token-less user has nothing to
        // invalidate anyway. The probe-then-delete window is benign: a token
        // inserted in between leaves at most a transient duplicate, cleared by
        // the next rotation — the same acceptance as the bulk-delete race.
        if (refreshTokenRepository.existsByUser(user)) {
            // Invalidate existing tokens for the user to enforce one active
            // refresh token per user.
            refreshTokenRepository.deleteByUser(user);
        }

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString());

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Walks the cause chain looking for a transient lock conflict: Spring's
     * {@link ConcurrencyFailureException} family (what repository exception
     * translation emits for a deadlock), JPA's lock exceptions, or the raw
     * {@link SQLException} with MySQL's deadlock / lock-wait-timeout codes
     * (what surfaces when the exception is NOT translated, e.g. at commit
     * time). Anything else — constraint violations, missing tables, closed
     * connections — is NOT retried.
     */
    private static boolean isTransientLockFailure(Throwable ex) {
        Throwable cause = ex;
        int depth = 0;
        while (cause != null && depth++ < 16) {
            if (cause instanceof ConcurrencyFailureException) {
                return true;
            }
            if (cause instanceof jakarta.persistence.PessimisticLockException
                    || cause instanceof jakarta.persistence.LockTimeoutException) {
                return true;
            }
            if (cause instanceof SQLException sqlException) {
                int errorCode = sqlException.getErrorCode();
                if (errorCode == MYSQL_ERROR_DEADLOCK || errorCode == MYSQL_ERROR_LOCK_WAIT_TIMEOUT) {
                    return true;
                }
                String sqlState = sqlException.getSQLState();
                if (sqlState != null && sqlState.startsWith("40")) {
                    // SQLSTATE class 40 = transaction rollback (retryable).
                    return true;
                }
            }
            Throwable next = cause.getCause();
            if (next == cause) {
                break;
            }
            cause = next;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RefreshToken verifyExpiration(RefreshToken token) throws TokenRefreshException {
        // v1.2.11 SECURITY: deactivating an account revokes its refresh tokens
        // (UserServiceImpl), but a token issued before deactivation must also
        // fail closed even if it somehow survived — never mint a new JWT for
        // an inactive user.
        if (token.getUser() != null && !token.getUser().isActive()) {
            // Do NOT delete here: verifyExpiration is deliberately outside a
            // transaction (§20a) and a bulk JPQL delete would fail with
            // TransactionRequiredException -> HTTP 500. Deactivation already
            // revoked the tokens at source (UserServiceImpl); rejecting is
            // enough to fail closed for any surviving row.
            throw new TokenRefreshException(token.getToken(),
                    "User account is deactivated. Please contact an administrator.");
        }
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new sign-in request");
        }
        return token;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int deleteByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId + " - cannot delete refresh tokens."));
        return refreshTokenRepository.deleteByUser(user);
    }

    /**
     * {@inheritDoc}
     * Bulk delete by token string — same race rationale as
     * {@link #createRefreshToken}: the old load-then-entity-delete emitted
     * {@code DELETE ... WHERE id = ?} with a row-count expectation, so a token
     * already invalidated by a concurrent rotation made logout fail.
     */
    @Override
    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken oldToken) {
        // 1. Delete the old token that was just used — by token STRING (idempotent
        //    bulk DML), not as a managed entity: two refreshes presenting the same
        //    token concurrently used to race the entity delete into a
        //    0-row ObjectOptimisticLockingFailureException.
        refreshTokenRepository.deleteByToken(oldToken.getToken());

        // 2. Create a new token for the same user.
        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setUser(oldToken.getUser());
        newRefreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        newRefreshToken.setToken(UUID.randomUUID().toString());

        // 3. Save and return the new token.
        return refreshTokenRepository.save(newRefreshToken);
    }


}
