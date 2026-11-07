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
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import static com.forkmyfolio.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME;

/**
 * Handles successful OAuth2 authentications.
 * Creates a JWT for the user and redirects them to the frontend application
 * with the token in the query parameters.
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final AppProperties appProperties;
    private final RefreshTokenService refreshTokenService;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
    private final UserRepository userRepository;

    /**
     * This method is triggered when an OAuth2 authentication attempt is successful.
     *
     * @param request        the request which caused the successful authentication.
     * @param response       the response.
     * @param authentication the <tt>Authentication</tt> object which was created during the authentication process.
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            logger.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * Builds the target URL for redirection. This includes generating the JWT,
     * creating a refresh token, and adding them to the response.
     *
     * @param request        the HTTP request.
     * @param response       the HTTP response.
     * @param authentication the Authentication object.
     * @return The fully formed URL to redirect the user to.
     */
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        Optional<String> redirectUri = CookieUtils.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);

        if (redirectUri.isPresent() && !isAuthorizedRedirectUri(redirectUri.get())) {
            throw new BadRequestException("Sorry! We've got an Unauthorized Redirect URI and can't proceed with the authentication");
        }

        String targetUrl = redirectUri.orElse(getDefaultTargetUrl());

        // The principal from OAuth2 authentication is a UserPrincipal.
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getId();

        // RefreshTokenService requires the full User entity to create a token.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found with ID: " + userId + " after successful authentication."));

        String token = tokenProvider.createToken(authentication);
        // Correctly call createRefreshToken with the User object.
        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();

        // Add refresh token to a secure, HttpOnly cookie
        CookieUtils.addCookie(response, appProperties.getJwt().getRefreshCookieName(), refreshToken, (int) (appProperties.getJwt().getRefreshExpirationMs() / 1000));

        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("token", token)
                .build().toUriString();
    }

    /**
     * Removes temporary authentication-related data that was stored in cookies.
     *
     * @param request  the HTTP request.
     * @param response the HTTP response.
     */
    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }

    /**
     * Checks if the provided redirect URI is authorized in the application's configuration.
     *
     * @param uri The URI to check.
     * @return true if the URI is authorized, false otherwise.
     */
    private boolean isAuthorizedRedirectUri(String uri) {
        URI clientRedirectUri = URI.create(uri);

        return appProperties.getOauth2().getAuthorizedRedirectUris()
                .stream()
                .anyMatch(authorizedUri -> {
                    URI authorizedURI = URI.create(authorizedUri);
                    // Compare host, port, and scheme. Path is not compared to allow for deep linking.
                    return authorizedURI.getHost().equalsIgnoreCase(clientRedirectUri.getHost())
                            && authorizedURI.getPort() == clientRedirectUri.getPort()
                            && authorizedURI.getScheme().equalsIgnoreCase(clientRedirectUri.getScheme());
                });
    }
}