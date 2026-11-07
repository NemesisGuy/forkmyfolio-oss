package com.forkmyfolio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class ApplicationConfig {

    /**
     * Defines the primary authentication provider.
     * It uses DaoAuthenticationProvider, which is responsible for fetching user
     * details
     * from a UserDetailsService and comparing passwords using a PasswordEncoder.
     * By defining this bean explicitly, we ensure the correct, non-proxied
     * dependencies are injected.
     *
     * @param userDetailsService The service to load user data.
     * @param passwordEncoder    The encoder to validate passwords.
     * @return A fully configured AuthenticationProvider.
     */
    @Bean
    public AuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        // This will now unambiguously use your dedicated security service
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    /**
     * Provides the AuthenticationManager bean. Spring Security will automatically
     * use the
     * AuthenticationProvider bean(s) defined in the context.
     *
     * @param config The authentication configuration from Spring Security.
     * @return The configured AuthenticationManager.
     * @throws Exception if the AuthenticationManager cannot be retrieved.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Provides a PasswordEncoder bean for hashing passwords.
     *
     * @return A BCryptPasswordEncoder instance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}