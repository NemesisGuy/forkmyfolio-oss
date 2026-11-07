package com.forkmyfolio.service;

/**
 * Service for generating unique, URL-friendly slugs for users.
 */
public interface SlugService {

    /**
     * Generates a unique, URL-friendly slug from a base name (e.g., a user's full
     * name).
     * <p>
     * The process involves:
     * 1. Sanitizing the base name to be URL-safe (lowercase, hyphens for spaces,
     * removes special characters).
     * 2. Checking for collisions with existing user slugs in the database.
     * 3. Checking for collisions with a list of reserved keywords (e.g., 'admin',
     * 'api').
     * 4. Appending a numerical suffix if a collision is found, until a unique slug
     * is generated.
     *
     * @param baseName The string to base the slug on, typically a user's first and
     *                 last name.
     * @return A unique, URL-safe slug string.
     */
    String generateUniqueSlug(String baseName);

    /**
     * Converts a string into a URL-friendly slug.
     */
    String toSlug(String input);

    /**
     * Checks if a slug is valid (URL-safe and not reserved).
     */
    boolean isValidSlug(String slug);

    /**
     * Checks if a slug is available for a specific user (not taken by another
     * user).
     */
    boolean isSlugAvailable(String slug, java.util.UUID currentUserId);
}