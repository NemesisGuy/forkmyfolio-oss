package com.forkmyfolio.security.oauth2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forkmyfolio.config.AppProperties;
import io.jsonwebtoken.io.Decoders;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.Set;

/**
 * Encodes the short-lived OAuth2 authorization-request cookie as HMAC-signed
 * JSON instead of Java native serialization.
 *
 * <p>The old {@code SerializationUtils.deserialize(cookie)} path could execute
 * attacker-supplied gadget chains if the backend was exposed directly (the
 * bundled proxy does not route {@code /oauth2/**}). JSON has no executable
 * type metadata, and the HMAC (keyed with the already-required JWT signing
 * secret) makes a forged payload fail closed — {@link #decode(String)}
 * returns {@code null} on any tampering or malformed input.
 */
@Component
public class OAuth2AuthorizationRequestCodec {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final byte[] key;

    public OAuth2AuthorizationRequestCodec(ObjectMapper objectMapper, AppProperties appProperties) {
        this.objectMapper = objectMapper;
        // Same secret as JWT signing — no new secret to configure or rotate.
        this.key = Decoders.BASE64.decode(appProperties.getJwt().getSecret());
    }

    /** Minimal, JSON-safe projection of OAuth2AuthorizationRequest. */
    public static class Payload {
        public String authorizationUri;
        public String authorizationRequestUri;
        public String clientId;
        public String redirectUri;
        public String state;
        public Set<String> scopes;
        public Map<String, Object> additionalParameters;
        public Map<String, Object> attributes;
    }

    /**
     * Serializes to {@code base64url(json).base64url(hmac)}.
     */
    public String encode(OAuth2AuthorizationRequest request) {
        Payload payload = new Payload();
        payload.authorizationUri = request.getAuthorizationUri().toString();
        payload.authorizationRequestUri = request.getAuthorizationRequestUri() == null
                ? null : request.getAuthorizationRequestUri().toString();
        payload.clientId = request.getClientId();
        payload.redirectUri = request.getRedirectUri();
        payload.state = request.getState();
        payload.scopes = request.getScopes();
        payload.additionalParameters = request.getAdditionalParameters();
        payload.attributes = request.getAttributes();

        try {
            byte[] json = objectMapper.writeValueAsBytes(payload);
            return B64.encodeToString(json) + "." + B64.encodeToString(hmac(json));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot encode OAuth2 authorization request", ex);
        }
    }

    /**
     * Verifies the signature BEFORE parsing, and returns {@code null} for any
     * tampered, malformed or legacy (Java-serialized) value — never throws.
     */
    public OAuth2AuthorizationRequest decode(String value) {
        try {
            if (value == null) {
                return null;
            }
            int dot = value.indexOf('.');
            if (dot <= 0 || dot != value.lastIndexOf('.')) {
                return null;
            }
            byte[] json = B64_DECODER.decode(value.substring(0, dot));
            byte[] signature = B64_DECODER.decode(value.substring(dot + 1));
            if (!MessageDigest.isEqual(signature, hmac(json))) {
                return null;
            }
            Payload payload = objectMapper.readValue(json, Payload.class);
            Map<String, Object> attributes = payload.attributes == null ? Map.of() : payload.attributes;
            Map<String, Object> additional = payload.additionalParameters == null ? Map.of() : payload.additionalParameters;
            return OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri(payload.authorizationUri)
                    .authorizationRequestUri(payload.authorizationRequestUri)
                    .clientId(payload.clientId)
                    .redirectUri(payload.redirectUri)
                    .scopes(payload.scopes == null ? Set.of() : payload.scopes)
                    .state(payload.state)
                    .additionalParameters(additional)
                    .attributes(attrs -> attrs.putAll(attributes))
                    .build();
        } catch (Exception ex) {
            // Includes IllegalArgumentException from the builder, Jackson
            // parse errors, bad Base64, missing/invalid fields. Fail closed.
            return null;
        }
    }

    private byte[] hmac(byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            return mac.doFinal(data);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot sign OAuth2 authorization request", ex);
        }
    }
}
