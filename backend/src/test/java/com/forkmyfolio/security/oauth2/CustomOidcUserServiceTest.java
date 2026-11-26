package com.forkmyfolio.security.oauth2;

import com.forkmyfolio.exception.OAuth2AuthenticationProcessingException;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.AuthProvider;
import com.forkmyfolio.model.enums.Role;
import com.forkmyfolio.repository.UserRepository;
import com.forkmyfolio.service.SlugService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit pins for the OIDC provisioning contract: identity resolution, name
 * fallbacks, role policy, linking conflicts and disabled users.
 */
@ExtendWith(MockitoExtension.class)
class CustomOidcUserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SlugService slugService;
    @Mock
    private OidcUserRequest userRequest;
    @Mock
    private ClientRegistration clientRegistration;
    @Mock
    private OidcUser oidcUser;

    private CustomOidcUserService service;

    @BeforeEach
    void setUp() {
        service = new CustomOidcUserService(new SocialUserProvisioner(userRepository, slugService));
        // Some tests exercise only the static name-fallback helpers.
        org.mockito.Mockito.lenient().when(userRequest.getClientRegistration()).thenReturn(clientRegistration);
        org.mockito.Mockito.lenient().when(clientRegistration.getRegistrationId()).thenReturn("keycloak");
    }

    private Map<String, Object> claims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "kc-sub-123");
        claims.put("email", "ada@example.test");
        claims.put("given_name", "Ada");
        claims.put("family_name", "Lovelace");
        claims.put("preferred_username", "ada");
        claims.put("name", "Ada Lovelace");
        claims.put("picture", "https://idp.example.test/ada.png");
        return claims;
    }

    private OidcUser oidcWith(Map<String, Object> claims) {
        when(oidcUser.getAttributes()).thenReturn(claims);
        when(oidcUser.getIdToken()).thenReturn(null);
        when(oidcUser.getUserInfo()).thenReturn(null);
        return oidcUser;
    }

    private User persistedUser(long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("ada@example.test");
        user.setProvider(AuthProvider.KEYCLOAK);
        user.setProviderId("kc-sub-123");
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setSlug("ada-lovelace");
        user.setRoles(new HashSet<>(Set.of(Role.USER)));
        user.setActive(true);
        return user;
    }

    @Test
    void newOidcUser_isCreatedWithKeycloakProviderUserRoleAndNames() {
        Map<String, Object> claims = claims();
        OidcUser principal = oidcWith(claims);
        when(userRepository.findByProviderAndProviderId(AuthProvider.KEYCLOAK, "kc-sub-123"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("ada@example.test")).thenReturn(Optional.empty());
        when(slugService.generateUniqueSlug("Ada Lovelace")).thenReturn("ada-lovelace");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        OidcUser result = service.processOidcUser(userRequest, principal);

        assertThat(result).isInstanceOf(OidcUserPrincipal.class).isInstanceOf(UserPrincipal.class);
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getProvider()).isEqualTo(AuthProvider.KEYCLOAK);
        assertThat(saved.getValue().getProviderId()).isEqualTo("kc-sub-123");
        assertThat(saved.getValue().getRoles()).containsExactly(Role.USER);
        assertThat(saved.getValue().getFirstName()).isEqualTo("Ada");
        assertThat(saved.getValue().getLastName()).isEqualTo("Lovelace");
        assertThat(saved.getValue().getSlug()).isEqualTo("ada-lovelace");
        Map<String, Object> attributes = result.getAttributes();
        assertThat(attributes).containsEntry(OidcUserPrincipal.LOCAL_USER_ID_ATTRIBUTE, 1L)
                .containsEntry(OidcUserPrincipal.PROVIDER_ID_ATTRIBUTE, "kc-sub-123");
    }

    @Test
    void idpAdminRoleClaim_neverProducesAdminRole() {
        Map<String, Object> claims = claims();
        // Even a Keycloak realm-admin claim must not elevate the local account.
        claims.put("realm_access", Map.of("roles", java.util.List.of("admin", "offline_access")));
        OidcUser principal = oidcWith(claims);
        when(userRepository.findByProviderAndProviderId(AuthProvider.KEYCLOAK, "kc-sub-123"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("ada@example.test")).thenReturn(Optional.empty());
        when(slugService.generateUniqueSlug(anyString())).thenReturn("ada-lovelace");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });

        OidcUser result = service.processOidcUser(userRequest, principal);

        assertThat(((UserPrincipal) result).getAuthorities())
                .extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    void lookupByProviderIdFirst_doesNotConsultEmail_andLinksExistingAccount() {
        Map<String, Object> claims = claims();
        claims.put("given_name", "Augusta");
        claims.put("family_name", "King");
        OidcUser principal = oidcWith(claims);
        User existing = persistedUser(7L);
        when(userRepository.findByProviderAndProviderId(AuthProvider.KEYCLOAK, "kc-sub-123"))
                .thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        OidcUser result = service.processOidcUser(userRequest, principal);

        verify(userRepository, never()).findByEmail(anyString());
        assertThat(((UserPrincipal) result).getId()).isEqualTo(7L);
        assertThat(result.getAttributes()).containsEntry(OidcUserPrincipal.LOCAL_USER_ID_ATTRIBUTE, 7L);
        assertThat(existing.getFirstName()).isEqualTo("Augusta");
        assertThat(existing.getLastName()).isEqualTo("King");
    }

    @Test
    void emailMatchingLocalAccount_isRejectedNeverMerged() {
        OidcUser principal = oidcWith(claims());
        User local = persistedUser(9L);
        local.setProvider(AuthProvider.LOCAL);
        when(userRepository.findByProviderAndProviderId(AuthProvider.KEYCLOAK, "kc-sub-123"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("ada@example.test")).thenReturn(Optional.of(local));

        assertThatThrownBy(() -> service.processOidcUser(userRequest, principal))
                .isInstanceOf(OAuth2AuthenticationProcessingException.class)
                .hasMessageContaining("LOCAL");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void emailMatchingSameProviderDifferentSubject_isRejected() {
        OidcUser principal = oidcWith(claims());
        User otherKeycloakUser = persistedUser(10L);
        otherKeycloakUser.setProviderId("kc-sub-different");
        when(userRepository.findByProviderAndProviderId(AuthProvider.KEYCLOAK, "kc-sub-123"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("ada@example.test")).thenReturn(Optional.of(otherKeycloakUser));

        assertThatThrownBy(() -> service.processOidcUser(userRequest, principal))
                .isInstanceOf(OAuth2AuthenticationProcessingException.class)
                .hasMessageContaining("different");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void disabledAccount_isRejectedAndNeverUpdated() {
        OidcUser principal = oidcWith(claims());
        User disabled = persistedUser(11L);
        disabled.setActive(false);
        when(userRepository.findByProviderAndProviderId(AuthProvider.KEYCLOAK, "kc-sub-123"))
                .thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> service.processOidcUser(userRequest, principal))
                .isInstanceOf(OAuth2AuthenticationProcessingException.class)
                .hasMessageContaining("disabled");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void nameFallbackChain_coversAllNotNullCases() {
        assertThat(SocialUserProvisioner.resolveFirstName(Map.of("given_name", "Ada", "family_name", "Lovelace")))
                .isEqualTo("Ada");
        assertThat(SocialUserProvisioner.resolveLastName(Map.of("given_name", "Ada", "family_name", "Lovelace")))
                .isEqualTo("Lovelace");

        Map<String, Object> preferredAndName = Map.of(
                "preferred_username", "ada", "name", "Ada Lovelace", "email", "ada@example.test");
        assertThat(SocialUserProvisioner.resolveFirstName(preferredAndName)).isEqualTo("ada");
        assertThat(SocialUserProvisioner.resolveLastName(preferredAndName)).isEqualTo("Lovelace");

        Map<String, Object> fullNameOnly = Map.of("name", "Grace Hopper", "email", "grace@example.test");
        assertThat(SocialUserProvisioner.resolveFirstName(fullNameOnly)).isEqualTo("Grace");
        assertThat(SocialUserProvisioner.resolveLastName(fullNameOnly)).isEqualTo("Hopper");

        Map<String, Object> emailOnly = Map.of("email", "linus@example.test");
        assertThat(SocialUserProvisioner.resolveFirstName(emailOnly)).isEqualTo("linus");
        assertThat(SocialUserProvisioner.resolveLastName(emailOnly)).isNotBlank();
    }
}
