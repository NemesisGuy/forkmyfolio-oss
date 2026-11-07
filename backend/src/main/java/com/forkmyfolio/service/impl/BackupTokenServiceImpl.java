package com.forkmyfolio.service.impl;

import org.springframework.stereotype.Service;

import com.forkmyfolio.service.BackupTokenService;
import com.forkmyfolio.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of {@link BackupTokenService}.
 * Performs password re-verification for sensitive backup operations against
 * the currently authenticated user's stored credential hash only.
 *
 * <p>Phase 0.6C hardening: the previous implementation accepted a blank
 * password (or the {@code ADMIN_PASSWORD} environment value) for ADMIN-role
 * users. That fallback is removed — verification must always prove knowledge
 * of the user's real password, and authorization remains the responsibility
 * of the security filter chain (role checks on the endpoints). The plaintext
 * password is never stored or logged.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BackupTokenServiceImpl implements BackupTokenService {

    private final UserService userService;

    @Override
    public boolean verifyPasswordForBackup(String email, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            // Fail closed: a missing or blank password must never authenticate,
            // regardless of the caller's role.
            return false;
        }
        return userService.verifyCurrentUserPassword(rawPassword);
    }
}
