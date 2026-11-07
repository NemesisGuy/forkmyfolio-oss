package com.forkmyfolio.service;

import com.forkmyfolio.model.User;

public interface AuthService {
    /**
     * Registers a new user in the system.
     */
    User registerUser(User user, String rawPassword, String termsVersion);
}