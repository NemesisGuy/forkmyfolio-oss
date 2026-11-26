package com.forkmyfolio.security.oauth2;

import com.forkmyfolio.config.AppProperties;
import com.forkmyfolio.exception.BadRequestException;
import com.forkmyfolio.exception.OAuth2AuthenticationProcessingException;
import com.forkmyfolio.model.RefreshToken;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.AuthProvider;
import com.forkmyfolio.model.enums.Role;
import com.forkmyfolio.repository.UserRepository;
import com.forkmyfolio.security.JwtTokenProvider;
import com.forkmyfolio.security.oauth2.OidcUserPrincipal;
import com.forkmyfolio.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the callback contract: redirect allowlist (both handlers), ?token=
 * redirect, and refresh-cookie attribute parity with password login.
 */
@ExtendWith(MockitoExtension.class)
class OAuth2CallbackSecurityTest {

    private static final String ALLOWED = "http://localhost:5173/oauth2/redirect";

    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private HttpCookieOAuth2AuthorizationRequestRepository requestRepository;
    @Mock
    private UserRepository userRepository;

    private AppProperties properties;
    private OAuth2AuthenticationSuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        properties = new AppProperties();
        properties.getJwt().setRefreshCookieName("refreshToken");
        properties.getJwt().setRefreshExpirationMs(604800000L);
        properties.getCookie().setSecure(true);
        properties.getCookie().setSamesite("None");
        properties.getSecurity().setCookieDomain("example.test");
        properties.getOauth2().setAuthorizedRedirectUris(List.of(ALLOWED));
        successHandler = new OAuth2AuthenticationSuccessHandler(
                tokenProvider, properties, refreshTokenService, requestRepository, userRepository);
    }

    private User localUser() {
        User user = new User();
        user.setId(5L);
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

    private MockHttpServletRequest requestWithRedirect(String redirectUri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (redirectUri != null) {
            request.setCookies(new Cookie("redirect_uri", redirectUri));
        }
        return request;
    }

    @Test
    void redirectAllowlist_fallsBackToConfiguredCallbackHost_whenAllowlistIsStale() {
        // Reproduces staging: APP_OAUTH2_AUTHORIZEDREDIRECTURIS still had the
        // localhost default, but the configured Keycloak callback proves the
        // trusted same-host origin.
        properties.getOauth2().setAuthorizedRedirectUris(List.of(ALLOWED));
        properties.getOauth2().setKeycloakRedirectUri(
                "https://forkmyfolio-staging.nemesisnet.co.za/oauth2/callback/keycloak");

        assertThat(RedirectUriValidator.isAuthorizedRedirectUri(
                "https://forkmyfolio-staging.nemesisnet.co.za/oauth2/redirect", properties)).isTrue();
        assertThat(RedirectUriValidator.isAuthorizedRedirectUri(
                "https://evil.example.test/oauth2/redirect", properties)).isFalse();
    }

    @Test
    void redirectAllowlist_acceptsMatchingSchemeHostPortAndRejectsOthers() {
        assertThat(RedirectUriValidator.isAuthorizedRedirectUri(ALLOWED + "?x=1", properties)).isTrue();
        assertThat(RedirectUriValidator.isAuthorizedRedirectUri("http://localhost:5173/deep/link", properties)).isTrue();
        assertThat(RedirectUriValidator.isAuthorizedRedirectUri("http://localhost:9999/oauth2/redirect", properties)).isFalse();
        assertThat(RedirectUriValidator.isAuthorizedRedirectUri("https://evil.example.test/oauth2/redirect", properties)).isFalse();
        assertThat(RedirectUriValidator.isAuthorizedRedirectUri("/oauth2/redirect", properties)).isFalse();
        assertThat(RedirectUriValidator.isAuthorizedRedirectUri("not a uri", properties)).isFalse();
        assertThat(RedirectUriValidator.isAuthorizedRedirectUri(null, properties)).isFalse();
    }

    @Test
    void successHandler_issuesTokenRefreshCookieAndKeepsTokenQueryParam() {
        User user = localUser();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(tokenProvider.createToken(any(Authentication.class))).thenReturn("signed.jwt");
        when(refreshTokenService.createRefreshToken(user))
                .thenReturn(new RefreshToken(user, "refresh-value", Instant.now()));

        var principal = new UserPrincipal(5L, user.getEmail(), null, true,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        MockHttpServletRequest request = requestWithRedirect(ALLOWED);
        MockHttpServletResponse response = new MockHttpServletResponse();

        String targetUrl = successHandler.determineTargetUrl(request, response, authentication);

        assertThat(targetUrl).startsWith(ALLOWED).contains("token=signed.jwt");
        String expectedCookie = RefreshTokenCookieFactory.create(properties, "refresh-value").toString();
        // Expires is stamped from the current clock and can tick over between
        // the handler call and this assertion; compare every stable attribute.
        assertThat(withoutExpires(response.getHeader(HttpHeaders.SET_COOKIE)))
                .isEqualTo(withoutExpires(expectedCookie));
        assertThat(expectedCookie).contains("Secure", "SameSite=None", "Domain=example.test", "HttpOnly", "Path=/");
    }

    @Test
    void successHandler_rejectsUnauthorizedRedirectBeforeIssuingTokens() {
        MockHttpServletRequest request = requestWithRedirect("https://evil.example.test/steal");
        var principal = new UserPrincipal(5L, "ada@example.test", null, true, List.of());
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        assertThatThrownBy(() -> successHandler.determineTargetUrl(request, new MockHttpServletResponse(), authentication))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void successHandler_resolvesPlainOidcPrincipalViaEmbeddedLocalUserId() {
        User user = localUser();
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(tokenProvider.createToken(any(Authentication.class))).thenReturn("oidc.jwt");
        when(refreshTokenService.createRefreshToken(user))
                .thenReturn(new RefreshToken(user, "refresh-value", Instant.now()));

        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getAttributes()).thenReturn(Map.of(
                OidcUserPrincipal.LOCAL_USER_ID_ATTRIBUTE, 5L,
                "sub", "kc-sub-123"));
        Authentication authentication = new UsernamePasswordAuthenticationToken(oidcUser, null, List.of());

        String targetUrl = successHandler.determineTargetUrl(
                requestWithRedirect(null), new MockHttpServletResponse(), authentication);

        assertThat(targetUrl).contains("token=oidc.jwt");
        ArgumentCaptor<Authentication> tokenAuth = ArgumentCaptor.forClass(Authentication.class);
        verify(tokenProvider).createToken(tokenAuth.capture());
        assertThat(tokenAuth.getValue().getPrincipal()).isInstanceOf(UserPrincipal.class);
        assertThat(((UserPrincipal) tokenAuth.getValue().getPrincipal()).getId()).isEqualTo(5L);
    }

    @Test
    void failureHandler_validatesRedirectCookieAgainstSameAllowlist() throws Exception {
        OAuth2AuthenticationFailureHandler failureHandler =
                new OAuth2AuthenticationFailureHandler(requestRepository, properties);
        AuthenticationExceptionStub exception = new AuthenticationExceptionStub("login failed");

        MockHttpServletRequest unauthorized = requestWithRedirect("https://evil.example.test/steal");
        MockHttpServletResponse unauthorizedResponse = new MockHttpServletResponse();
        failureHandler.onAuthenticationFailure(unauthorized, unauthorizedResponse, exception);
        assertThat(unauthorizedResponse.getRedirectedUrl()).startsWith("/?error=");

        MockHttpServletRequest authorized = requestWithRedirect(ALLOWED);
        MockHttpServletResponse authorizedResponse = new MockHttpServletResponse();
        failureHandler.onAuthenticationFailure(authorized, authorizedResponse, exception);
        assertThat(authorizedResponse.getRedirectedUrl()).startsWith(ALLOWED + "?error=");
    }

    @Test
    void refreshCookieFactory_clearsCookieWithZeroMaxAge() {
        assertThat(RefreshTokenCookieFactory.create(properties, "").toString()).contains("Max-Age=0");
    }

    private static String withoutExpires(String cookie) {
        return cookie.replaceAll("Expires=[^;]+; ", "");
    }

    /** Local stand-in so the test does not depend on a Spring exception hierarchy detail. */
    private static final class AuthenticationExceptionStub
            extends org.springframework.security.core.AuthenticationException {
        AuthenticationExceptionStub(String message) {
            super(message);
        }
    }
}
