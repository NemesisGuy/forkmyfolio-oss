package com.forkmyfolio.security.oauth2;

import com.forkmyfolio.model.User;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The principal returned by {@link CustomOidcUserService}.
 *
 * <p>Spring Security's OIDC authentication provider casts the result of
 * {@code loadOidcUser} to {@link OidcUser}, while the rest of ForkMyFolio (JWT
 * provider, success handler) works with {@link UserPrincipal}. This class is
 * both: it carries the original ID-token/user-info claims *and* the local
 * database user id, so the callback can resolve the account reliably.
 */
public final class OidcUserPrincipal extends UserPrincipal implements OidcUser {

    /** Claim carrying our local user id; used as a defensive fallback in the success handler. */
    public static final String LOCAL_USER_ID_ATTRIBUTE = "fmf_local_user_id";
    /** Claim carrying the provider's subject, for diagnostics/linking checks. */
    public static final String PROVIDER_ID_ATTRIBUTE = "fmf_provider_id";

    private final OidcIdToken idToken;
    private final OidcUserInfo userInfo;
    private final Map<String, Object> claims;

    private OidcUserPrincipal(User user, Map<String, Object> claims,
            OidcIdToken idToken, OidcUserInfo userInfo) {
        super(user.getId(), user.getEmail(), user.getPassword(), user.isEnabled(), user.getAuthorities());
        this.claims = Collections.unmodifiableMap(new LinkedHashMap<>(claims));
        this.idToken = idToken;
        this.userInfo = userInfo;
        setAttributes(this.claims);
    }

    /**
     * Merges ID-token claims, user-info claims and the attributes supplied by
     * the OIDC user service, then stamps our local identity onto the result.
     */
    public static OidcUserPrincipal create(User user, Map<String, Object> sourceClaims,
            OidcIdToken idToken, OidcUserInfo userInfo) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (userInfo != null) {
            merged.putAll(userInfo.getClaims());
        }
        if (idToken != null) {
            merged.putAll(idToken.getClaims());
        }
        if (sourceClaims != null) {
            merged.putAll(sourceClaims);
        }
        merged.put(LOCAL_USER_ID_ATTRIBUTE, user.getId());
        merged.put(PROVIDER_ID_ATTRIBUTE, user.getProviderId() == null ? "" : user.getProviderId());
        return new OidcUserPrincipal(user, merged, idToken, userInfo);
    }

    @Override
    public Map<String, Object> getClaims() {
        return claims;
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return userInfo;
    }

    @Override
    public OidcIdToken getIdToken() {
        return idToken;
    }
}
