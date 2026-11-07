package com.forkmyfolio.security.oauth2;

import com.forkmyfolio.exception.OAuth2AuthenticationProcessingException;
import com.forkmyfolio.model.enums.Role;
import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.AuthProvider;
import com.forkmyfolio.repository.UserRepository;
import com.forkmyfolio.service.impl.SlugGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Optional;

/**
 * Custom implementation of OAuth2UserService that handles the logic of
 * processing an OAuth2 user after they have been authenticated by an external provider.
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    /**
     * This method is called by Spring Security after a user successfully authenticates with an OAuth2 provider.
     *
     * @param oAuth2UserRequest Contains the user's access token and client registration details.
     * @return An OAuth2User (our UserPrincipal) that represents the authenticated user.
     * @throws OAuth2AuthenticationException if an error occurs during processing.
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            // Throwing an instance of AuthenticationException will trigger the OAuth2AuthenticationFailureHandler
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    /**
     * Processes the OAuth2 user information, either by updating an existing user
     * or registering a new one.
     *
     * @param userRequest The original request from the OAuth2 client.
     * @param oAuth2User  The user details returned by the OAuth2 provider.
     * @return A UserPrincipal object for Spring Security's context.
     */
    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String email = oAuth2User.getAttribute("email");

        if (!StringUtils.hasText(email)) {
            throw new OAuth2AuthenticationProcessingException("Email not found from OAuth2 provider: " + registrationId);
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());
            if (!user.getProvider().equals(provider)) {
                throw new OAuth2AuthenticationProcessingException("Looks like you're signed up with " +
                        user.getProvider() + " account. Please use your " + user.getProvider() +
                        " account to login.");
            }
            user = updateExistingUser(user, oAuth2User);
        } else {
            user = registerNewUser(userRequest, oAuth2User);
        }

        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }

    /**
     * Registers a new user in the database with the information from the OAuth2 provider.
     *
     * @param userRequest The original request from the OAuth2 client.
     * @param oAuth2User  The user details from the provider.
     * @return The newly created and persisted User entity.
     */
    private User registerNewUser(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        User user = new User();
        AuthProvider provider = AuthProvider.valueOf(userRequest.getClientRegistration().getRegistrationId().toUpperCase());

        user.setProvider(provider);
        user.setProviderId(oAuth2User.getName()); // 'name' is the standard for the principal's unique ID
        user.setEmail(oAuth2User.getAttribute("email"));
        user.setFirstName(oAuth2User.getAttribute("given_name"));
        user.setLastName(oAuth2User.getAttribute("family_name"));
        user.setProfileImageUrl(oAuth2User.getAttribute("picture"));
        user.setRoles(Collections.singleton(Role.USER));
        user.setEmailVerified(true); // Assume email is verified by the provider
        user.setActive(true);
        user.setSlug(SlugGenerator.generateSlug(user.getFirstName() + " " + user.getLastName()));

        return userRepository.save(user);
    }

    /**
     * Updates an existing user's information with the latest data from the OAuth2 provider.
     *
     * @param existingUser The user entity from our database.
     * @param oAuth2User   The user details from the provider.
     * @return The updated and persisted User entity.
     */
    private User updateExistingUser(User existingUser, OAuth2User oAuth2User) {
        // Update fields that might change, like name or profile picture
        existingUser.setFirstName(oAuth2User.getAttribute("given_name"));
        existingUser.setLastName(oAuth2User.getAttribute("family_name"));
        existingUser.setProfileImageUrl(oAuth2User.getAttribute("picture"));
        return userRepository.save(existingUser);
    }
}