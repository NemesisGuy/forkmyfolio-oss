package com.forkmyfolio.security.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forkmyfolio.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.Base64;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * v1.2.11 security pin: the OAuth2 authorization-request cookie must be
 * signed JSON, never Java native serialization (deserialization gadgets).
 */
class OAuth2AuthorizationRequestCodecTest {

    private OAuth2AuthorizationRequestCodec codec;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        // 64 bytes of deterministic test key material (HS512-style secret).
        properties.getJwt().setSecret(Base64.getEncoder().encodeToString(new byte[64]));
        codec = new OAuth2AuthorizationRequestCodec(new ObjectMapper(), properties);
    }

    private OAuth2AuthorizationRequest request() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://idp.example.com/authorize")
                .authorizationRequestUri("https://idp.example.com/authorize?client_id=abc&state=xyz")
                .clientId("abc")
                .redirectUri("http://localhost:8080/oauth2/callback/google")
                .state("xyz")
                .scopes(Set.of("openid", "profile"))
                .additionalParameters(Map.of("prompt", "consent"))
                .attributes(attrs -> {
                    attrs.put("registration_id", "google");
                    attrs.put("code_verifier", "pkce-verifier-123");
                })
                .build();
    }

    @Test
    void roundTrip_preservesStateRedirectUriScopesAndPkceAttributes() {
        String encoded = codec.encode(request());

        // Signed format, not Base64 of a Java serialized object.
        assertThat(encoded).contains(".");

        OAuth2AuthorizationRequest decoded = codec.decode(encoded);
        assertThat(decoded).isNotNull();
        assertThat(decoded.getState()).isEqualTo("xyz");
        assertThat(decoded.getRedirectUri()).isEqualTo("http://localhost:8080/oauth2/callback/google");
        assertThat(decoded.getClientId()).isEqualTo("abc");
        assertThat(decoded.getScopes()).containsExactlyInAnyOrder("openid", "profile");
        assertThat(decoded.getAttributes()).containsEntry("registration_id", "google");
        assertThat(decoded.getAttributes()).containsEntry("code_verifier", "pkce-verifier-123");
        assertThat(decoded.getAdditionalParameters()).containsEntry("prompt", "consent");
    }

    @Test
    void tamperedPayload_isRejected() {
        String encoded = codec.encode(request());
        int dot = encoded.indexOf('.');
        // Flip one character of the JSON payload (keep it Base64URL-valid).
        char c = encoded.charAt(1) == 'A' ? 'B' : 'A';
        String tampered = encoded.substring(0, 1) + c + encoded.substring(2, dot) + encoded.substring(dot);

        assertThat(codec.decode(tampered)).isNull();
    }

    @Test
    void wrongSignature_isRejected() {
        String encoded = codec.encode(request());
        int dot = encoded.indexOf('.');
        String forged = encoded.substring(0, dot + 1) + "AAAA";

        assertThat(codec.decode(forged)).isNull();
    }

    @Test
    void malformedAndLegacyValues_areRejectedWithoutThrowing() {
        assertThat(codec.decode(null)).isNull();
        assertThat(codec.decode("")).isNull();
        assertThat(codec.decode("not-base64.")).isNull();
        assertThat(codec.decode("one.two.three")).isNull();
    }
}
