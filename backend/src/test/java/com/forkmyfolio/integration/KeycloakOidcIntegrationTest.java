package com.forkmyfolio.integration;

import com.forkmyfolio.exception.OAuth2AuthenticationProcessingException;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.AuthProvider;
import com.forkmyfolio.model.enums.Role;
import com.forkmyfolio.security.oauth2.CustomOidcUserService;
import com.forkmyfolio.security.oauth2.OidcUserPrincipal;
import com.forkmyfolio.security.oauth2.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Real-MySQL verification (Flyway V14 included) of the Keycloak/OIDC user
 * service: a mocked IdP response is provisioned into the production-shaped
 * schema, then re-linked by (provider, provider_id).
 */
class KeycloakOidcIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private CustomOidcUserService customOidcUserService;

    private OidcUserRequest keycloakRequest() {
        ClientRegistration registration = mock(ClientRegistration.class);
        when(registration.getRegistrationId()).thenReturn("keycloak");
        OidcUserRequest request = mock(OidcUserRequest.class);
        when(request.getClientRegistration()).thenReturn(registration);
        return request;
    }

    private OidcUser oidcUser(Map<String, Object> claims) {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getAttributes()).thenReturn(claims);
        when(oidcUser.getIdToken()).thenReturn(null);
        when(oidcUser.getUserInfo()).thenReturn(null);
        return oidcUser;
    }

    private Map<String, Object> claims(String suffix, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "kc-sub-" + suffix);
        claims.put("email", email);
        claims.put("given_name", "Keycloak");
        claims.put("family_name", "User" + suffix);
        claims.put("preferred_username", "kcuser-" + suffix);
        claims.put("realm_access", Map.of("roles", java.util.List.of("admin")));
        return claims;
    }

    @Test
    void oidcLogin_createsDbUserWithKeycloakProviderAndProviderId() {
        String suffix = runSuffix();
        String email = "oidc-create-" + suffix + "@example.test";
        OidcUserRequest request = keycloakRequest();

        OidcUser result = customOidcUserService.processOidcUser(request, oidcUser(claims(suffix, email)));

        assertThat(result).isInstanceOf(OidcUserPrincipal.class).isInstanceOf(UserPrincipal.class);
        Long userId = ((UserPrincipal) result).getId();
        User saved = userRepository.findById(userId).orElseThrow();
        assertThat(saved.getProvider()).isEqualTo(AuthProvider.KEYCLOAK);
        assertThat(saved.getProviderId()).isEqualTo("kc-sub-" + suffix);
        assertThat(saved.getEmail()).isEqualTo(email);
        assertThat(saved.getFirstName()).isEqualTo("Keycloak");
        assertThat(saved.getLastName()).isEqualTo("User" + suffix);
        assertThat(saved.getSlug()).isNotBlank();
        assertThat(saved.getRoles()).containsExactly(Role.USER);
        assertThat(saved.isActive()).isTrue();

        // Second login with the SAME subject links by (provider, provider_id),
        // even if the email claim changes; no second row is created.
        Map<String, Object> changedEmailClaims = claims(suffix, "changed-" + suffix + "@example.test");
        changedEmailClaims.put("given_name", "Relinked");
        OidcUser relinked = customOidcUserService.processOidcUser(request, oidcUser(changedEmailClaims));

        assertThat(((UserPrincipal) relinked).getId()).isEqualTo(userId);
        assertThat(userRepository.findById(userId).orElseThrow().getFirstName()).isEqualTo("Relinked");
        assertThat(userRepository.findById(userId).orElseThrow().getEmail()).isEqualTo(email);
    }

    @Test
    void oidcLogin_rejectsEmailThatBelongsToLocalAccount() {
        String suffix = runSuffix();
        String email = "oidc-conflict-" + suffix + "@example.test";
        User local = seedUser(email, "oidc-conflict-" + suffix, false, Role.USER);
        assertThat(local.getProvider()).isEqualTo(AuthProvider.LOCAL);

        OidcUserRequest request = keycloakRequest();
        Map<String, Object> claims = claims(suffix, email);
        claims.put("sub", "kc-sub-conflict-" + suffix);

        assertThatThrownBy(() -> customOidcUserService.processOidcUser(request, oidcUser(claims)))
                .isInstanceOf(OAuth2AuthenticationProcessingException.class)
                .hasMessageContaining("LOCAL");

        Optional<User> unexpected = userRepository.findByProviderAndProviderId(
                AuthProvider.KEYCLOAK, "kc-sub-conflict-" + suffix);
        assertThat(unexpected).isEmpty();
    }
}
