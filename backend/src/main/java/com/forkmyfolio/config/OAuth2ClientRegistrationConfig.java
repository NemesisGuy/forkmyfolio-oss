package com.forkmyfolio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provider-neutral OAuth2/OIDC client registrations.
 *
 * <p>Spring Boot normally resolves {@code provider.*.issuer-uri} eagerly over
 * HTTP at bean creation time. That would prevent the app from starting when no
 * Keycloak is configured, so the keycloak registration is built here:
 * <ul>
 *   <li>when {@code KEYCLOAK_ISSUER_URI} is a real value, discovery is used
 *       ({@link ClientRegistrations#fromIssuerLocation(String)});</li>
 *   <li>otherwise a harmless localhost placeholder (with explicit OIDC
 *       endpoints) is registered — no network call, no real URL, no secret;
 *       the button simply cannot complete a login until configured.</li>
 * </ul>
 * Google/GitHub are still mapped from the standard Boot properties, now with
 * harmless defaults so an unset environment does not block startup.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OAuth2ClientProperties.class)
public class OAuth2ClientRegistrationConfig {

    static final String KEYCLOAK_REGISTRATION_ID = "keycloak";
    /** Clearly fake, local-only placeholder — never a real Keycloak URL. */
    static final String PLACEHOLDER_ISSUER_URI = "http://localhost:8080/realms/keycloak-placeholder";

    /**
     * Real issuer, read from a dedicated property so the standard
     * {@code spring.security.oauth2.client.provider.keycloak.issuer-uri} is NOT
     * set — Boot would otherwise try HTTP discovery in every Spring context
     * (including @WebMvcTest slices) and fail on the placeholder.
     */
    @Value("${app.oauth2.keycloak.issuer-uri:}")
    private String configuredIssuerUri;

    @Bean
    @ConditionalOnMissingBean(org.springframework.security.oauth2.client.registration.ClientRegistrationRepository.class)
    public InMemoryClientRegistrationRepository clientRegistrationRepository(
            OAuth2ClientProperties properties) {
        // Map the ordinary (Google/GitHub) registrations exactly as Boot would,
        // but exclude keycloak so its placeholder issuer-uri is not discovered.
        OAuth2ClientProperties standardProperties = copyWithoutKeycloak(properties);
        Map<String, ClientRegistration> registrations =
                new OAuth2ClientPropertiesMapper(standardProperties).asClientRegistrations();

        registrations.put(KEYCLOAK_REGISTRATION_ID, buildKeycloakRegistration(properties));
        return new InMemoryClientRegistrationRepository(new ArrayList<>(registrations.values()));
    }

    private static OAuth2ClientProperties copyWithoutKeycloak(OAuth2ClientProperties source) {
        OAuth2ClientProperties copy = new OAuth2ClientProperties();
        source.getRegistration().forEach((id, registration) -> {
            if (!KEYCLOAK_REGISTRATION_ID.equals(id)) {
                copy.getRegistration().put(id, copyRegistration(registration));
            }
        });
        source.getProvider().forEach((id, provider) -> {
            if (!KEYCLOAK_REGISTRATION_ID.equals(id)) {
                copy.getProvider().put(id, copyProvider(provider));
            }
        });
        return copy;
    }

    private static OAuth2ClientProperties.Registration copyRegistration(
            OAuth2ClientProperties.Registration source) {
        OAuth2ClientProperties.Registration copy = new OAuth2ClientProperties.Registration();
        copy.setProvider(source.getProvider());
        copy.setClientId(source.getClientId());
        copy.setClientSecret(source.getClientSecret());
        copy.setClientAuthenticationMethod(source.getClientAuthenticationMethod());
        copy.setAuthorizationGrantType(source.getAuthorizationGrantType());
        copy.setRedirectUri(source.getRedirectUri());
        copy.setScope(source.getScope());
        copy.setClientName(source.getClientName());
        return copy;
    }

    private static OAuth2ClientProperties.Provider copyProvider(OAuth2ClientProperties.Provider source) {
        OAuth2ClientProperties.Provider copy = new OAuth2ClientProperties.Provider();
        copy.setAuthorizationUri(source.getAuthorizationUri());
        copy.setTokenUri(source.getTokenUri());
        copy.setUserInfoUri(source.getUserInfoUri());
        copy.setUserInfoAuthenticationMethod(source.getUserInfoAuthenticationMethod());
        copy.setUserNameAttribute(source.getUserNameAttribute());
        copy.setJwkSetUri(source.getJwkSetUri());
        copy.setIssuerUri(source.getIssuerUri());
        return copy;
    }

    private ClientRegistration buildKeycloakRegistration(OAuth2ClientProperties properties) {
        OAuth2ClientProperties.Registration registration = properties.getRegistration().get(KEYCLOAK_REGISTRATION_ID);
        boolean configured = StringUtils.hasText(configuredIssuerUri);

        ClientRegistration.Builder builder = configured
                ? ClientRegistrations.fromIssuerLocation(configuredIssuerUri)
                : placeholderBuilder(PLACEHOLDER_ISSUER_URI);

        return builder
                .registrationId(KEYCLOAK_REGISTRATION_ID)
                .clientId(registration.getClientId())
                .clientSecret(registration.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(registration.getRedirectUri())
                .scope(registration.getScope() == null ? java.util.Set.of("openid") : registration.getScope())
                .clientName("Keycloak")
                .build();
    }

    private static ClientRegistration.Builder placeholderBuilder(String issuerUri) {
        String base = StringUtils.hasText(issuerUri) ? issuerUri : PLACEHOLDER_ISSUER_URI;
        return ClientRegistration.withRegistrationId(KEYCLOAK_REGISTRATION_ID)
                .issuerUri(base)
                .authorizationUri(base + "/protocol/openid-connect/auth")
                .tokenUri(base + "/protocol/openid-connect/token")
                .userInfoUri(base + "/protocol/openid-connect/userinfo")
                .jwkSetUri(base + "/protocol/openid-connect/certs")
                // Keycloak's user-info endpoint includes preferred_username.
                .userNameAttributeName("preferred_username");
    }
}
