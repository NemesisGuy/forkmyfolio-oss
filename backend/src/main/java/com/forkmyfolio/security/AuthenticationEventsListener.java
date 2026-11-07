package com.forkmyfolio.security;

import com.forkmyfolio.service.impl.VisitorStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationEventsListener {

    private final VisitorStatsService visitorStatsService;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent success) {
        log.info("Successful login detected for user: {}", success.getAuthentication().getName());
        visitorStatsService.incrementLoginSuccess();
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent failure) {
        // Principal might be a string or an object, so handle it carefully
        String username = (failure.getAuthentication().getPrincipal() != null) ? failure.getAuthentication().getPrincipal().toString() : "N/A";
        log.warn("Failed login attempt detected for user: {}. Reason: {}", username, failure.getException().getMessage());
        visitorStatsService.incrementLoginFailure();
    }
}