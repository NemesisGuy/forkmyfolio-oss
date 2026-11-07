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
     * Deletes all refresh tokens associated with a specific user.
     * This is useful for scenarios like logout from all devices or when a user's account
     * details (e.g., password) change.
     * The {@link Modifying} annotation is required for DML operations.
     *
     * @param user The user whose refresh tokens are to be deleted.
     * @return The number of refresh tokens deleted.
     */
    @Modifying
    int deleteByUser(User user);

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


    void deleteByToken(String token);

}
