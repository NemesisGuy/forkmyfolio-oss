package com.forkmyfolio.security.oauth2;

import com.forkmyfolio.config.AppProperties;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.List;

/**
 * Shared allowlist check for OAuth2/OIDC redirects. Scheme, host and port must
 * match an entry in {@code app.oauth2.authorized-redirect-uris}; the path is
 * intentionally free so SPAs can deep-link. Used by BOTH the success and the
 * failure handler — an unvalidated failure redirect is an open redirect.
 */
public final class RedirectUriValidator {

    private RedirectUriValidator() {
    }

    public static boolean isAuthorizedRedirectUri(String uri, AppProperties appProperties) {
        if (!StringUtils.hasText(uri) || appProperties.getOauth2().getAuthorizedRedirectUris() == null) {
            return false;
        }

        final URI clientRedirectUri;
        try {
            clientRedirectUri = URI.create(uri);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        if (clientRedirectUri.getScheme() == null || clientRedirectUri.getHost() == null) {
            return false;
        }

        List<String> authorizedUris = appProperties.getOauth2().getAuthorizedRedirectUris();
        for (String authorizedUri : authorizedUris) {
            if (!StringUtils.hasText(authorizedUri)) {
                continue;
            }
            try {
                URI authorized = URI.create(authorizedUri);
                if (authorized.getHost() == null || authorized.getScheme() == null) {
                    continue;
                }
                if (authorized.getHost().equalsIgnoreCase(clientRedirectUri.getHost())
                        && authorized.getPort() == clientRedirectUri.getPort()
                        && authorized.getScheme().equalsIgnoreCase(clientRedirectUri.getScheme())) {
                    return true;
                }
            } catch (IllegalArgumentException ex) {
                // A misconfigured allowlist entry must never authorize a redirect.
            }
        }
        return false;
    }
}
