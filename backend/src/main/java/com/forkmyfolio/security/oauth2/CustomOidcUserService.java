package com.forkmyfolio.security.oauth2;

import com.forkmyfolio.exception.OAuth2AuthenticationProcessingException;
import com.forkmyfolio.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OIDC counterpart of {@link CustomOAuth2UserService}, used for Keycloak (and
 * any other registration with an {@code openid} scope, notably Google).
 *
 * <p>The returned principal is an {@link OidcUserPrincipal} — a
 * {@link UserPrincipal} that also satisfies Spring Security's
 * {@link OidcUser} contract — so both the local JWT pipeline and the OIDC
 * authentication provider see the same local user.
 */
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final SocialUserProvisioner provisioner;

    /**
     * Loads the claims from the IdP, then resolves/creates the local account.
     * Any authentication error reaches {@code OAuth2AuthenticationFailureHandler}.
     */
    // Spring Security 6 names the OIDC entry point loadUser(OidcUserRequest)
    // (there is no DefaultOidcUserService in this version); loadOidcUser below
    // is kept as the provider-neutral alias used by our tests/docs.
    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser;
        try {
            oidcUser = super.loadUser(userRequest);
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }

        try {
            return processOidcUser(userRequest, oidcUser);
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    /** Alias for {@link #loadUser(OidcUserRequest)}. */
    public OidcUser loadOidcUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        return loadUser(userRequest);
    }

    /**
     * Applies our provisioning policy to an already-loaded OIDC user. Public
     * so integration tests can drive it with a mocked IdP response without
     * performing a network call.
     */
    public OidcUser processOidcUser(OidcUserRequest userRequest, OidcUser oidcUser) {
        if (oidcUser == null) {
            throw new OAuth2AuthenticationProcessingException(
                    "No OIDC user info returned by the provider");
        }
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = mergedAttributes(oidcUser);
        String providerId = resolveProviderId(oidcUser, attributes);

        User user = provisioner.process(registrationId, providerId, attributes);
        return OidcUserPrincipal.create(user, attributes, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }

    /**
     * The IdP subject ({@code sub}) is the stable identity. Fall back only for
     * providers that omit it from both the ID token and user-info endpoint.
     */
    private String resolveProviderId(OidcUser oidcUser, Map<String, Object> attributes) {
        OidcIdToken idToken = oidcUser.getIdToken();
        if (idToken != null && StringUtils.hasText(idToken.getSubject())) {
            return idToken.getSubject();
        }
        String sub = SocialUserProvisioner.attribute(attributes, "sub");
        if (StringUtils.hasText(sub)) {
            return sub;
        }
        String preferred = SocialUserProvisioner.attribute(attributes, "preferred_username");
        if (StringUtils.hasText(preferred)) {
            return preferred;
        }
        return SocialUserProvisioner.attribute(attributes, "email");
    }

    /** ID-token claims, user-info claims and the OIDC principal's claims, combined. */
    private Map<String, Object> mergedAttributes(OidcUser oidcUser) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        OidcUserInfo userInfo = oidcUser.getUserInfo();
        if (userInfo != null && userInfo.getClaims() != null) {
            attributes.putAll(userInfo.getClaims());
        }
        OidcIdToken idToken = oidcUser.getIdToken();
        if (idToken != null && idToken.getClaims() != null) {
            attributes.putAll(idToken.getClaims());
        }
        if (oidcUser.getAttributes() != null) {
            attributes.putAll(oidcUser.getAttributes());
        }
        if (idToken != null && StringUtils.hasText(idToken.getSubject())) {
            attributes.put("sub", idToken.getSubject());
        }
        return attributes;
    }
}
