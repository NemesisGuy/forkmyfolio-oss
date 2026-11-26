package com.forkmyfolio.security.oauth2;

import com.forkmyfolio.exception.OAuth2AuthenticationProcessingException;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.AuthProvider;
import com.forkmyfolio.model.enums.Role;
import com.forkmyfolio.repository.UserRepository;
import com.forkmyfolio.service.SlugService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Shared account-provisioning policy for both raw OAuth2 (Google/GitHub) and
 * OIDC (Keycloak) logins.
 *
 * <p>Linking order is deliberate:
 * <ol>
 *   <li>look up by {@code (provider, provider_id)} — the IdP's subject is the
 *       authoritative identity;</li>
 *   <li>only then look up by email. A match is LINKED to the current provider:
 *       the IdP has proven ownership of that email, so the account keeps its
 *       password hash, roles, slug and id and can be used with EITHER login
 *       method (owner decision 2026-11-26).</li>
 * </ol>
 * Disabled accounts are always rejected. New social accounts always get
 * {@link Role#USER}; IdP-supplied roles are deliberately ignored (no
 * auto-admin provisioning).
 */
@Service
@RequiredArgsConstructor
public class SocialUserProvisioner {

    private final UserRepository userRepository;
    private final SlugService slugService;

    /**
     * Resolves (and, for a new user, persists) the local account for a
     * successfully authenticated external identity.
     *
     * @param registrationId Spring's client registration id (e.g. {@code keycloak})
     * @param providerId     the provider's stable subject/user id
     * @param attributes     the claims/user-info attributes from the provider
     * @return the persisted local user
     * @throws OAuth2AuthenticationProcessingException on any unsafe linking or
     *         disabled account
     */
    @Transactional
    public User process(String registrationId, String providerId, Map<String, Object> attributes) {
        AuthProvider provider = parseProvider(registrationId);
        Map<String, Object> claims = attributes == null ? Map.of() : attributes;

        if (!StringUtils.hasText(providerId)) {
            throw new OAuth2AuthenticationProcessingException(
                    "Provider id (sub) not found from OAuth2 provider: " + registrationId);
        }
        String email = attribute(claims, "email");
        if (!StringUtils.hasText(email)) {
            throw new OAuth2AuthenticationProcessingException(
                    "Email not found from OAuth2 provider: " + registrationId);
        }

        Optional<User> byProviderId = userRepository.findByProviderAndProviderId(provider, providerId);
        User user;

        if (byProviderId.isPresent()) {
            user = byProviderId.get();
            rejectIfDisabled(user);
            // Provider id wins: the IdP has already proven this identity.
            return updateExistingUser(user, claims);
        }

        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            rejectIfDisabled(existing);
            // Email-based linking: the IdP has verified this email, so attach
            // the new identity to the SAME account. Password hash, roles,
            // slug and id are deliberately untouched — the user can keep
            // logging in with their password as well as with SSO.
            return linkExistingUser(existing, provider, providerId, claims);
        }

        return registerNewUser(provider, providerId, email, claims);
    }

    /** Turns a Spring registration id into our persisted provider enum. */
    private AuthProvider parseProvider(String registrationId) {
        if (!StringUtils.hasText(registrationId)) {
            throw new OAuth2AuthenticationProcessingException("Missing OAuth2 registration id");
        }
        try {
            return AuthProvider.valueOf(registrationId.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new OAuth2AuthenticationProcessingException(
                    "Unsupported OAuth2 provider: " + registrationId);
        }
    }

    private void rejectIfDisabled(User user) {
        if (!user.isActive()) {
            throw new OAuth2AuthenticationProcessingException(
                    "This account is disabled. Please contact the administrator.");
        }
    }

    private User registerNewUser(AuthProvider provider, String providerId, String email,
            Map<String, Object> claims) {
        String firstName = resolveFirstName(claims);
        String lastName = resolveLastName(claims);

        User user = new User();
        user.setProvider(provider);
        user.setProviderId(providerId);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setProfileImageUrl(attribute(claims, "picture"));
        // Roles are never taken from the IdP — always the least-privileged role.
        user.setRoles(Collections.singleton(Role.USER));
        user.setEmailVerified(true); // the IdP is authoritative for email ownership
        user.setActive(true);
        user.setSlug(slugService.generateUniqueSlug(firstName + " " + lastName));
        return userRepository.save(user);
    }

    /** Links an IdP identity onto an existing local account without touching its password/roles. */
    private User linkExistingUser(User existing, AuthProvider provider, String providerId,
            Map<String, Object> claims) {
        existing.setProvider(provider);
        existing.setProviderId(providerId);
        existing.setEmailVerified(true);
        return updateExistingUser(existing, claims);
    }

    private User updateExistingUser(User existingUser, Map<String, Object> claims) {
        String firstName = resolveFirstName(claims);
        String lastName = resolveLastName(claims);
        existingUser.setFirstName(firstName);
        existingUser.setLastName(lastName);
        existingUser.setProfileImageUrl(attribute(claims, "picture"));
        return userRepository.save(existingUser);
    }

    /**
     * First-name fallback chain: given_name → preferred_username → first token
     * of name → email local part. Always non-blank so the NOT NULL column is
     * satisfied for GitHub/Keycloak users without given_name.
     */
    static String resolveFirstName(Map<String, Object> claims) {
        String givenName = attribute(claims, "given_name");
        if (StringUtils.hasText(givenName)) {
            return givenName;
        }
        String preferredUsername = attribute(claims, "preferred_username");
        if (StringUtils.hasText(preferredUsername)) {
            return preferredUsername;
        }
        String[] nameParts = nameParts(claims);
        if (nameParts.length > 0) {
            return nameParts[0];
        }
        String emailLocalPart = emailLocalPart(claims);
        return StringUtils.hasText(emailLocalPart) ? emailLocalPart : "User";
    }

    /**
     * Last-name fallback chain: family_name → second token of name →
     * preferred_username (when not already used as first name) → email local
     * part. Never null or blank.
     */
    static String resolveLastName(Map<String, Object> claims) {
        String familyName = attribute(claims, "family_name");
        if (StringUtils.hasText(familyName)) {
            return familyName;
        }
        String[] nameParts = nameParts(claims);
        if (nameParts.length > 1) {
            return nameParts[1];
        }
        String firstName = resolveFirstName(claims);
        String preferredUsername = attribute(claims, "preferred_username");
        if (StringUtils.hasText(preferredUsername)
                && !preferredUsername.equalsIgnoreCase(firstName)) {
            return preferredUsername;
        }
        String emailLocalPart = emailLocalPart(claims);
        if (StringUtils.hasText(emailLocalPart) && !emailLocalPart.equalsIgnoreCase(firstName)) {
            return emailLocalPart;
        }
        return "User";
    }

    private static String[] nameParts(Map<String, Object> claims) {
        String name = attribute(claims, "name");
        return StringUtils.hasText(name) ? name.trim().split("\\s+") : new String[0];
    }

    private static String emailLocalPart(Map<String, Object> claims) {
        String email = attribute(claims, "email");
        if (!StringUtils.hasText(email)) {
            return "";
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at).trim() : email.trim();
    }

    /** Reads a claim defensively (some providers return non-string values). */
    static String attribute(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }
}
