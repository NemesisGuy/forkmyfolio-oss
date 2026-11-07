package com.forkmyfolio.security;

import com.forkmyfolio.model.User;
import com.forkmyfolio.repository.UserRepository;
import com.forkmyfolio.security.oauth2.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom implementation of {@link UserDetailsService}.
 * Loads user-specific data by querying the {@link UserRepository}.
 */
@Service
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Constructs a {@code CustomUserDetailsService} with the specified
     * {@link UserRepository}.
     *
     * @param userRepository the repository to access user data.
     */
    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads the user by their username (which is email in this application).
     *
     * @param email the email of the user to load.
     * @return a {@link UserDetails} object representing the user.
     * @throws UsernameNotFoundException if the user with the given email is not
     *                                   found.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        // Standardize on UserPrincipal for all authentication paths
        return UserPrincipal.create(user);
    }

    /**
     * Loads a user by their ID. This method is not part of the UserDetailsService
     * interface
     * but can be useful in other parts of the security implementation, like JWT
     * validation.
     *
     * @param id the ID of the user to load.
     * @return a {@link UserDetails} object representing the user.
     * @throws UsernameNotFoundException if the user with the given ID is not found.
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        log.info("CustomUserDetailsService: Loading user by ID: {}", id);
        if (userRepository == null) {
            log.error("CustomUserDetailsService: userRepository is NULL!");
            throw new RuntimeException("userRepository is null");
        }
        User user = userRepository.findById(id).orElseThrow(
                () -> {
                    log.error("CustomUserDetailsService: User NOT FOUND with id: {}", id);
                    return new UsernameNotFoundException("User not found with id : " + id);
                });
        log.info("CustomUserDetailsService: Found user: {}", user.getEmail());
        // Standardize on UserPrincipal for all authentication paths
        return UserPrincipal.create(user);
    }
}
