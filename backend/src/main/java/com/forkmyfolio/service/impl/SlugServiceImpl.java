package com.forkmyfolio.service.impl;

import com.forkmyfolio.repository.UserRepository;
import com.forkmyfolio.service.SlugService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Implementation of the {@link SlugService}.
 * Handles the logic for creating unique, URL-safe slugs for user profiles.
 */
@Service
@RequiredArgsConstructor
public class SlugServiceImpl implements SlugService {

    // A pattern to match non-alphanumeric characters for slug generation.
    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    // A pattern to match whitespace for replacement with hyphens.
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    // A set of reserved words that cannot be used as slugs to avoid route
    // conflicts.
    private static final Set<String> RESERVED_SLUGS = Set.of(
            "admin", "api", "auth", "login", "logout", "register", "dashboard",
            "settings", "profile", "projects", "skills", "experience",
            "qualifications", "contact", "swagger-ui", "v3", "api-docs", "h2-console",
            "assets", "static", "images", "css", "js", "me", "system", "backup",
            "restore", "error", "404", "config", "internal", "null", "undefined");
    private final UserRepository userRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public String generateUniqueSlug(String baseName) {
        String baseSlug = toSlug(baseName);
        String candidate = baseSlug;
        int counter = 2; // Start appending '-2' if the base slug is taken

        // Loop until we find a slug that doesn't exist in the database AND is not a
        // reserved word.
        while (userRepository.existsBySlug(candidate) || RESERVED_SLUGS.contains(candidate)) {
            candidate = baseSlug + "-" + counter;
            counter++;
        }
        return candidate;
    }

    @Override
    public boolean isValidSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return false;
        }
        // Basic slug format check (lowercase, alphanumeric, hyphens)
        if (!slug.equals(toSlug(slug))) {
            return false;
        }
        // Reserved keyword check
        return !RESERVED_SLUGS.contains(slug.toLowerCase(Locale.ENGLISH));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSlugAvailable(String slug, java.util.UUID currentUserId) {
        if (!isValidSlug(slug)) {
            return false;
        }
        return userRepository.findBySlug(slug)
                .map(user -> user.getUuid().equals(currentUserId))
                .orElse(true);
    }

    /**
     * Converts a string into a URL-friendly slug.
     * This method sanitizes the input by making it lowercase, replacing spaces with
     * hyphens,
     * and removing any characters that are not letters, numbers, or hyphens.
     *
     * @param input The string to convert.
     * @return A sanitized, URL-friendly slug.
     */
    @Override
    public String toSlug(String input) {
        if (input == null) {
            return "user"; // Return a default slug for null input
        }
        // Replace whitespace with a single hyphen
        String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        // Normalize to decompose combined characters (e.g., 'é' to 'e' and '´')
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        // Remove diacritical marks (the combining characters)
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        // Convert to lowercase and remove leading/trailing hyphens
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("^-|-$", "");
    }
}