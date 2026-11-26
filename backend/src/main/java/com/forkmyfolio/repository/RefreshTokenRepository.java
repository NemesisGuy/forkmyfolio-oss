package com.forkmyfolio.repository;

import com.forkmyfolio.model.RefreshToken;
import com.forkmyfolio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link RefreshToken} entities.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Finds a refresh token by its token string.
     *
     * @param token The token string to search for.
     * @return An {@link Optional} containing the refresh token if found, or empty if not.
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Deletes all refresh tokens associated with a specific user in ONE bulk
     * statement ({@code DELETE ... WHERE user_id = ?}).
     *
     * <p>This is deliberately an explicit {@code @Query} bulk delete, NOT a
     * derived {@code deleteByUser}: derived deletes are executed by loading the
     * matching rows first and removing the entities one by one, which emits
     * {@code DELETE ... WHERE id = ?} per row with a row-count expectation.
     * Under concurrent logins for the same user (e2e run D: visual-proof login
     * HTTP 500), the loser's per-row delete affected 0 rows and Hibernate
     * surfaced {@code ObjectOptimisticLockingFailureException}
     * ("delete from refresh_tokens where id=?"). A true bulk delete treats a
     * 0-row match as success, so concurrent logins both complete.
     *
     * @param user The user whose refresh tokens are to be deleted.
     * @return The number of refresh tokens deleted.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user = :user")
    int deleteByUser(@Param("user") User user);

    /**
     * Finds a refresh token by the associated User.
     * This assumes a user might have one active refresh token at a time,
     * or this method would need to return a List<RefreshToken> if multiple are allowed
     * and the logic elsewhere handles which one to use/show.
     * For a strict one-token-per-user policy enforced by application logic (e.g., delete old on new login),
     * this can be useful.
     *
     * @param user The user to find the refresh token for.
     * @return An {@link Optional} containing the refresh token if found.
     */
    Optional<RefreshToken> findByUser(User user); // Useful if one user has one refresh token

    /**
     * --- THIS IS THE FIX ---
     * A new method that finds a refresh token and simultaneously fetches the associated
     * User and their Roles in a single, efficient database query. This prevents
     * LazyInitializationException in downstream code.
     *
     * @param token The refresh token to find.
     * @return An Optional containing the RefreshToken with a fully initialized User object.
     */
    @Query("SELECT rt FROM RefreshToken rt JOIN FETCH rt.user u JOIN FETCH u.roles WHERE rt.token = :token")
    Optional<RefreshToken> findByTokenAndFetchUserWithRoles(@Param("token") String token);

    /**
     * Deletes the token row for a single token string in ONE bulk statement.
     * Same rationale as {@link #deleteByUser}: the derived form loaded the row
     * and removed the entity (row-count expectation), so a token invalidated by
     * a concurrent rotation turned the logout delete into
     * {@code ObjectOptimisticLockingFailureException}. Bulk DML is idempotent.
     *
     * @param token The token string to delete.
     * @return The number of refresh tokens deleted (0 or 1).
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.token = :token")
    int deleteByToken(@Param("token") String token);

    /**
     * Non-locking probe: does this user currently have any refresh token?
     *
     * <p>Used by {@code createRefreshToken} to SKIP the invalidation DELETE for
     * token-less users (fresh registrations/logins) — v1.2.11 root-cause fix.
     * {@code DELETE ... WHERE user_id = ?} on a user with NO rows still
     * range-scans the {@code user_id} index up to the <b>supremum</b> and holds
     * that gap X-lock until commit — exactly the gap a parallel registration's
     * {@code INSERT} needs (insert-intention), so two fresh registrations
     * deadlocked (MySQL error 1213, both waiting at {@code hex73757072656d756d})
     * and one {@code /auth/register} returned HTTP 500. A plain SELECT is a
     * non-locking consistent read under REPEATABLE READ (no gap locks), so the
     * probe is free of locks and lets token-less users skip the delete —
     * parallel inserts at the index end do not conflict with each other.
     *
     * @param user The user to probe.
     * @return {@code true} if at least one refresh token exists for the user.
     */
    boolean existsByUser(User user);
}
