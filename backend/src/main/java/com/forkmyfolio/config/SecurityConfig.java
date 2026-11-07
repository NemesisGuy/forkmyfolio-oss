package com.forkmyfolio.config;

import com.forkmyfolio.security.JwtAuthenticationEntryPoint;
import com.forkmyfolio.security.JwtAuthenticationFilter;
import com.forkmyfolio.security.oauth2.CustomOAuth2UserService;
import com.forkmyfolio.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.forkmyfolio.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.forkmyfolio.security.oauth2.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Main security configuration class for the application.
 * Configures Spring Security, including password encoding, JWT authentication,
 * and HTTP security rules.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final AuthenticationProvider authenticationProvider;
        private final JwtAuthenticationEntryPoint unauthorizedHandler;
        private final com.forkmyfolio.security.RateLimitingFilter rateLimitingFilter;

        // OAuth2 Components
        private final CustomOAuth2UserService customOAuth2UserService;
        private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
        private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
        private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

        @Value("${app.cors.allowed-origins}")
        private String[] allowedOrigins;

        /**
         * Configures a CorsConfigurationSource bean for handling Cross-Origin Resource
         * Sharing (CORS).
         * This is the standard way to integrate CORS with Spring Security 6's DSL.
         * It correctly allows credentials, which is essential for the refresh token
         * cookie.
         *
         * @return A CorsConfigurationSource instance.
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowCredentials(true);

                if (allowedOrigins != null) {
                        List<String> origins = new java.util.ArrayList<>();
                        for (String origin : allowedOrigins) {
                                if (origin.contains(",")) {
                                        for (String s : origin.split(",")) {
                                                if (s != null && !s.isBlank()) {
                                                        origins.add(s.trim());
                                                }
                                        }
                                } else if (origin != null && !origin.isBlank()) {
                                        origins.add(origin.trim());
                                }
                        }
                        config.setAllowedOrigins(origins);
                        System.out.println("CORS Allowed Origins (Standardized): " + origins);
                }

                config.setAllowedHeaders(
                                List.of("Origin", "Content-Type", "Accept", "Authorization", "X-Requested-With", "X-Backup-Token"));
                config.setExposedHeaders(List.of("Content-Disposition", "Content-Length", "X-Backup-Token"));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
                source.registerCorsConfiguration("/**", config);
                return source;
        }

        /**
         * Defines the security filter chain for HTTP requests.
         * Configures CSRF, session management, exception handling, and authorization
         * rules.
         *
         * @param http The HttpSecurity object to configure.
         * @return The configured SecurityFilterChain.
         * @throws Exception If an error occurs during configuration.
         */
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(Customizer.withDefaults())
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(unauthorizedHandler))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authenticationProvider(authenticationProvider)
                                .authorizeHttpRequests(authorize -> authorize
                                                .requestMatchers(
                                                                "/api/v1/auth/**",
                                                                "/oauth2/**",
                                                                "/swagger-ui/**",
                                                                "/v3/api-docs/**",
                                                                "/actuator/**",
                                                                // FIX: Allow public access to policy endpoints for
                                                                // registration and viewing.
                                                                "/api/v1/policies/**",
                                                                "/api/v1/policy/**")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET,

                                                                "/api/v1/settings",
                                                                "/api/v1/settings/pdf-templates",
                                                                "/api/v1/portfolios/**",
                                                                // DEF-1: public, non-credential tracking config
                                                                // for portfolio pages (script URL + public website
                                                                // id only; unknown/private slugs concealed as 404).
                                                                "/api/v1/public/analytics/*/tracking-config")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST,
                                                                // THIS IS THE FIX: Correctly match the contact message
                                                                // endpoint with a wildcard
                                                                "/api/v1/portfolios/*/contact-messages",
                                                                "/api/v1/stats/**")
                                                .permitAll()
                                                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                                                // SECURITY (Phase 7.2): guest passes are read-only viewing
                                                // credentials. GUEST JWTs carry the OWNER's principal, so any
                                                // mutation under /api/v1/me/** or /api/v1/user/** executed
                                                // with ROLE_GUEST would write to the owner's portfolio.
                                                // Reads stay as-is; all other methods require USER/ADMIN.
                                                .requestMatchers(HttpMethod.GET, "/api/v1/me/**", "/api/v1/user/**")
                                                .hasAnyRole("USER", "ADMIN", "GUEST")
                                                .requestMatchers("/api/v1/me/**", "/api/v1/user/**")
                                                .hasAnyRole("USER", "ADMIN")
                                                // Guest passes are the owner's credentials; guests must
                                                // never list or revoke them.
                                                .requestMatchers("/api/v1/management/**")
                                                .hasAnyRole("USER", "ADMIN")
                                                .anyRequest().authenticated())
                                .oauth2Login(oauth2 -> oauth2
                                                .authorizationEndpoint(authz -> authz
                                                                .baseUri("/oauth2/authorize")
                                                                .authorizationRequestRepository(
                                                                                httpCookieOAuth2AuthorizationRequestRepository))
                                                .redirectionEndpoint(redirection -> redirection
                                                                .baseUri("/oauth2/callback/*"))
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService))
                                                .successHandler(oAuth2AuthenticationSuccessHandler)
                                                .failureHandler(oAuth2AuthenticationFailureHandler));

                http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
                http.addFilterAfter(rateLimitingFilter, JwtAuthenticationFilter.class);
                http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.deny()));

                return http.build();
        }
}