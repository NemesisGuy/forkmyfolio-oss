package com.forkmyfolio.security.oauth2;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processes a raw OAuth2 (non-OIDC) login. The account linking/name/role
 * policy is shared with the OIDC service through {@link SocialUserProvisioner}.
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final SocialUserProvisioner provisioner;

    /**
     * Called by Spring Security after the user authenticates with an OAuth2
     * provider. Always returns our {@link UserPrincipal} so the success handler
     * can issue a local JWT and refresh cookie.
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        try {
            String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
            var user = provisioner.process(registrationId, oAuth2User.getName(), oAuth2User.getAttributes());
            return UserPrincipal.create(user, oAuth2User.getAttributes());
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            // Throwing an AuthenticationException triggers the failure handler.
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }
}
