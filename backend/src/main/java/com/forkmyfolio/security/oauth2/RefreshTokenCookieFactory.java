package com.forkmyfolio.security.oauth2;

import com.forkmyfolio.config.AppProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;

/**
 * Single source of truth for the refresh-token cookie attributes.
 *
 * <p>Both password login ({@code AuthController}) and social/OIDC callbacks
 * ({@code OAuth2AuthenticationSuccessHandler}) use this factory so Secure,
 * SameSite, Domain, HttpOnly, path and max-age can never drift apart.
 */
public final class RefreshTokenCookieFactory {

    private RefreshTokenCookieFactory() {
    }

    public static ResponseCookie create(AppProperties appProperties, String token) {
        long maxAge = StringUtils.hasText(token)
                ? appProperties.getJwt().getRefreshExpirationMs() / 1000
                : 0;

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie
                .from(appProperties.getJwt().getRefreshCookieName(), token)
                .httpOnly(true)
                .secure(appProperties.getCookie().isSecure())
                .path("/")
                .maxAge(maxAge)
                .sameSite(appProperties.getCookie().getSamesite());

        String domain = appProperties.getSecurity().getCookieDomain();
        if (StringUtils.hasText(domain)) {
            builder.domain(domain);
        }
        return builder.build();
    }
}
