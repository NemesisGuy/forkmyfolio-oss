package com.forkmyfolio.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    private static final Logger log = LoggerFactory.getLogger(SecurityUtils.class);

    /**
     * Checks if the current user is anonymous (i.e., not logged in).
     *
     * @return true if the user is anonymous, false otherwise.
     */
    public boolean isUserAnonymous() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authClass = (authentication == null) ? "null" : authentication.getClass().getName();
        // This log helps debug authentication issues by showing the current user's auth type.
        log.debug("SECURITY CHECK: Authentication object is of type: [{}]", authClass);
        return authentication == null || authentication instanceof AnonymousAuthenticationToken;
    }
}