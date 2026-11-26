package com.forkmyfolio.model.enums;

/**
 * Defines the authentication providers available in the system.
 * LOCAL represents the standard email/password authentication.
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE,
    GITHUB,
    LINKEDIN,
    /** OIDC login via the configured Keycloak realm (registration id
     * {@code keycloak}). IdP roles are never promoted automatically. */
    KEYCLOAK
}