package com.forkmyfolio.service;

import com.forkmyfolio.model.enums.Role;
import com.forkmyfolio.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service interface for user-related business logic.
 * The implementation of this service also handles Spring Security's
 * UserDetailsService contract.
 */
public interface UserService {

    /**
     * Registers a new user. Can be used for public registration or admin creation.
     * Default roles and active status are applied if not provided.
     *
     * @param email           The user's email.
     * @param password        The user's raw password.
     * @param firstName       The user's first name.
     * @param lastName        The user's last name.
     * @param profileImageUrl The URL for the user's profile image (can be null).
     * @param roles           The roles to assign to the user (can be null for
     *                        default).
     * @param active          The active status of the user (can be null for
     *                        default).
     * @return The created {@link User} entity.
     * @throws com.forkmyfolio.exception.DuplicateResourceException if the email is
     *                                                              already in use.
     */
    User registerUser(String email, String password, String firstName, String lastName, String profileImageUrl,
            Set<Role> roles, Boolean active);

    /**
     * Retrieves the currently authenticated user from the security context.
     *
     * @return The {@link User} object representing the current user.
     * @throws org.springframework.security.core.userdetails.UsernameNotFoundException if
     *                                                                                 no
     *                                                                                 user
     *                                                                                 is
     *                                                                                 authenticated.
     */
    User getCurrentAuthenticatedUser();

    /**
     * Phase 8 (PRV-1): resolves whether the CURRENTLY authenticated principal
     * is the given portfolio owner themselves. Encapsulated in the service
     * layer so controllers never touch entity state directly (NAG-01), and so
     * the identity comparison (account email equality) has exactly one
     * implementation. Anonymous requests yield {@code false} — never owner.
     *
     * @param candidate the portfolio owner to compare against the current
     *                  authenticated principal.
     * @return {@code true} only when an authenticated user is present AND
     *         their account email equals the candidate's.
     */
    boolean isCurrentUser(User candidate);

    /**
     * Retrieves the currently authenticated user WITHOUT throwing when the
     * request is anonymous. Phase 8 (PRV-1): public endpoints that must
     * distinguish "guest" from "the portfolio owner themselves" use this —
     * an unauthenticated (or unrecognized-principal) request yields an
     * empty Optional and therefore can never be treated as an owner.
     *
     * @return an Optional of the authenticated user, or empty for anonymous
     *         requests.
     */
    Optional<User> findCurrentAuthenticatedUser();

    /**
     * Retrieves the currently authenticated user with all portfolio data eagerly
     * fetched.
     * This is the required method for generating complete backups to avoid lazy
     * loading issues.
     *
     * @return The fully initialized {@link User} object.
     */
    User getCurrentAuthenticatedUserWithAllPortfolioData();

    /**
     * Updates the profile information for the currently authenticated user.
     *
     * @param firstName       The new first name.
     * @param lastName        The new last name.
     * @param profileImageUrl The new profile image URL.
     * @return The updated {@link User} entity.
     */
    User updateUserProfile(String firstName, String lastName, String profileImageUrl);

    /**
     * Retrieves a paginated list of all users. For administrative purposes.
     *
     * @param pageable Pagination and sorting information.
     * @return A {@link Page} of {@link User} entities.
     */
    Page<User> getAllUsers(Pageable pageable);

    /**
     * Updates a user's details from an admin context.
     *
     * @param uuid      The UUID of the user to update.
     * @param firstName The user's new first name.
     * @param lastName  The user's new last name.
     * @param slug      The user's new slug.
     * @param roles     The user's new set of roles.
     * @param active    The user's new active status.
     * @return The updated {@link User} entity.
     */
    User updateUserByAdmin(UUID uuid, String firstName, String lastName, String slug, Set<Role> roles, Boolean active);

    /**
     * Deactivates a user's account (soft delete).
     *
     * @param uuid The UUID of the user to deactivate.
     */
    void deactivateUser(UUID uuid);

    /**
     * Finds a user by their email address.
     * Required for flows that resolve the authenticated principal's account,
     * e.g. password re-verification and backup-token issuance.
     *
     * @param email The email to search for.
     * @return An {@link Optional} containing the {@link User} if found.
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds a user by their public portfolio slug.
     *
     * @param slug The slug to search for.
     * @return An {@link Optional} containing the {@link User} if found.
     */
    Optional<User> findBySlug(String slug);

    /**
     * Finds a user by slug with all portfolio data eagerly fetched.
     *
     * @param slug The slug to search for.
     * @return An {@link Optional} containing the fully initialized {@link User}.
     */
    Optional<User> findBySlugWithAllPortfolioData(String slug);

    /**
     * Finds a user by their internal Long ID.
     *
     * @param id The ID of the user to find.
     * @return The found {@link User} entity.
     * @throws com.forkmyfolio.exception.ResourceNotFoundException if no user is
     *                                                             found.
     */
    User getUserById(Long id);

    /**
     * Finds a user by their public UUID.
     *
     * @param uuid The UUID of the user to find.
     * @return The found {@link User} entity.
     * @throws com.forkmyfolio.exception.ResourceNotFoundException if no user is
     *                                                             found.
     */
    User getUserByUuid(UUID uuid);

    /**
     * Ensures the default administrator user exists, creating or updating it as
     * needed
     * based on application properties. This is an idempotent operation.
     *
     * @param email     The admin's email.
     * @param password  The admin's raw password.
     * @param firstName The admin's first name.
     * @param lastName  The admin's last name.
     */
    void createOrUpdateAdminUser(String email, String password, String firstName, String lastName);

    /**
     * Finds all users with their portfolio data eagerly fetched.
     *
     * @return A list of fully initialized User entities.
     */
    List<User> getAllUsersWithPortfolioData();

    /**
     * Changes the password for the currently authenticated user.
     * Also resets the 'mustChangePassword' flag.
     * 
     * @param newPassword The new raw password.
     */
    void changeCurrentUserPassword(String newPassword);

    /**
     * Verifies the supplied raw password against the currently authenticated
     * user's stored encoded password hash.
     *
     * @param rawPassword the plaintext password to check (never stored or logged)
     * @return {@code true} if the password matches the stored hash
     */
    boolean verifyCurrentUserPassword(String rawPassword);

    /**
     * Determines the effective role of the currently authenticated user for
     * feature targeting (ADMIN falls back to USER). This role-derivation rule
     * is business logic and therefore lives in the service layer (NAG-01),
     * not the controller.
     *
     * @return {@link Role#ADMIN} when the current user has the ADMIN role,
     *         otherwise {@link Role#USER}.
     */
    Role getEffectiveRoleOfCurrentUser();

    /**
     * Resets another user's password (administrator recovery path). Used when
     * legacy V2 backups leave accounts without a usable credential.
     *
     * @param uuid       the target user's UUID
     * @param newPassword the new raw password, encoded before storage
     */
    void resetUserPassword(java.util.UUID uuid, String newPassword);

    void acceptTermsForCurrentUser();
}