package com.forkmyfolio.security.oauth2;

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adds OIDC {@code prompt=login} when the authorization URL is called with
 * {@code ?prompt=login} ("Use a different account"). Without it, Keycloak sees
 * an existing SSO session cookie and silently re-authenticates, never showing
 * the login form.
 */
@Component
public class PromptLoginAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver delegate;

    public PromptLoginAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, "/oauth2/authorize");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return withPromptLogin(delegate.resolve(request), request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return withPromptLogin(delegate.resolve(request, clientRegistrationId), request);
    }

    private OAuth2AuthorizationRequest withPromptLogin(OAuth2AuthorizationRequest request,
            HttpServletRequest httpRequest) {
        if (request == null || !"login".equalsIgnoreCase(httpRequest.getParameter("prompt"))) {
            return request;
        }
        Map<String, Object> additional = new LinkedHashMap<>(request.getAdditionalParameters());
        additional.put("prompt", "login");
        // Clear the precomputed URI so the builder re-derives it with prompt.
        return OAuth2AuthorizationRequest.from(request)
                .authorizationRequestUri((String) null)
                .additionalParameters(additional)
                .build();
    }
}
