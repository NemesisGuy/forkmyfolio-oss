package com.forkmyfolio.security.oauth2;

import com.forkmyfolio.config.AppProperties;
import com.forkmyfolio.exception.BadRequestException;
import com.forkmyfolio.model.User;
import com.forkmyfolio.repository.UserRepository;
import com.forkmyfolio.security.JwtTokenProvider;
import com.forkmyfolio.service.RefreshTokenService;
import com.forkmyfolio.util.CookieUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static com.forkmyfolio.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME;

/**
 * Handles successful OAuth2/OIDC authentications: resolves the local account,
 * issues a local JWT plus a hardened refresh cookie and redirects with
 * {@code ?token=} (existing callback contract).
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final AppProperties appProperties;
    private final RefreshTokenService refreshTokenService;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            logger.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * Builds the redirect URL: validates the requested redirect, resolves the
     * local user, issues JWT + refresh cookie and appends {@code ?token=}.
     */
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) {
        Optional<String> redirectUri = CookieUtils.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);

        if (redirectUri.isPresent() && !isAuthorizedRedirectUri(redirectUri.get())) {
            throw new BadRequestException("Sorry! We've got an Unauthorized Redirect URI and can't proceed with the authentication");
        }

        String targetUrl = redirectUri.orElse(getDefaultTargetUrl());
        Long userId = resolveLocalUserId(authentication);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "User not found with ID: " + userId + " after successful authentication."));
        if (!user.isActive()) {
            throw new BadRequestException("This account is disabled. Please contact the administrator.");
        }

        // JwtTokenProvider accepts our UserPrincipal; for a plain OIDC
        // principal we rebuild it from the freshly resolved local user.
        UserPrincipal principalForToken = authentication.getPrincipal() instanceof UserPrincipal userPrincipal
                ? userPrincipal
                : UserPrincipal.create(user);
        Authentication tokenAuthentication = new UsernamePasswordAuthenticationToken(
                principalForToken, null, principalForToken.getAuthorities());
        String token = tokenProvider.createToken(tokenAuthentication);

        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();
        ResponseCookie cookie = RefreshTokenCookieFactory.create(appProperties, refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("token", token)
                .build().toUriString();
    }

    /**
     * Supports both principal shapes: our own UserPrincipal (raw OAuth2 and
     * {@link OidcUserPrincipal}) and a foreign OIDC user carrying the local id
     * stamped by {@link CustomOidcUserService}.
     */
    private Long resolveLocalUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getId();
        }
        if (principal instanceof OidcUser oidcUser) {
            Map<String, Object> attributes = oidcUser.getAttributes();
            Object localUserId = attributes == null
                    ? null
                    : attributes.get(OidcUserPrincipal.LOCAL_USER_ID_ATTRIBUTE);
            Long parsed = parseLocalUserId(localUserId);
            if (parsed != null) {
                return parsed;
            }
            throw new BadRequestException(
                    "OIDC principal is missing the local user id; refusing to issue a token.");
        }
        throw new BadRequestException("Unsupported principal type for social login: "
                + (principal == null ? "null" : principal.getClass().getName()));
    }

    private Long parseLocalUserId(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && text.matches("\\d+")) {
            return Long.valueOf(text);
        }
        return null;
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }

    /** Shared allowlist check (also used by the failure handler). */
    private boolean isAuthorizedRedirectUri(String uri) {
        return RedirectUriValidator.isAuthorizedRedirectUri(uri, appProperties);
    }
}
