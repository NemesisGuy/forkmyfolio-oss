package com.forkmyfolio.security.oauth2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Pins the "Use a different account" prompt=login authorization request. */
class PromptLoginAuthorizationRequestResolverTest {

    private PromptLoginAuthorizationRequestResolver resolver;

    @BeforeEach
    void setUp() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("keycloak")
                .clientId("client")
                .clientSecret("secret")
                .clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/oauth2/callback/keycloak")
                .authorizationUri("https://idp.example.test/auth")
                .tokenUri("https://idp.example.test/token")
                .userNameAttributeName("sub")
                .build();
        ClientRegistrationRepository repository = mock(ClientRegistrationRepository.class);
        when(repository.findByRegistrationId("keycloak")).thenReturn(registration);
        resolver = new PromptLoginAuthorizationRequestResolver(repository);
    }

    private MockHttpServletRequest request(String prompt) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/oauth2/authorize/keycloak");
        request.setServletPath("/oauth2/authorize/keycloak");
        request.setContextPath("");
        if (prompt != null) {
            request.setParameter("prompt", prompt);
        }
        return request;
    }

    @Test
    void promptLogin_isForwardedToTheIdpAuthorizationUri() {
        OAuth2AuthorizationRequest result = resolver.resolve(request("login"));

        assertThat(result).isNotNull();
        assertThat(result.getAuthorizationRequestUri().toString()).contains("prompt=login");
        assertThat(result.getAdditionalParameters()).containsEntry("prompt", "login");
    }

    @Test
    void normalSsoButton_doesNotForceTheLoginForm() {
        OAuth2AuthorizationRequest result = resolver.resolve(request(null));

        assertThat(result).isNotNull();
        assertThat(result.getAuthorizationRequestUri().toString()).doesNotContain("prompt=login");
    }
}
