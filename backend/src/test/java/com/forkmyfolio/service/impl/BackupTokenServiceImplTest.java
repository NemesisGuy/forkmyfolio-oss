package com.forkmyfolio.service.impl;

import com.forkmyfolio.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Phase 0.6C regression tests for backup re-verification.
 *
 * <p>Proves the blank-password / environment-value ADMIN fallback is gone:
 * verification must always require the caller's real password, must fail
 * closed on missing or malformed input, and must never consult user roles or
 * the email-to-user lookup to grant backup access.
 */
@ExtendWith(MockitoExtension.class)
class BackupTokenServiceImplTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private BackupTokenServiceImpl service;

    @Test
    void blankPassword_isRejected_evenForAdminEmail() {
        assertFalse(service.verifyPasswordForBackup("admin@forkmyfolio.com", ""),
                "A blank password must never authenticate, not even for ADMIN users");
        verifyNoInteractions(userService);
    }

    @Test
    void nullPassword_isRejected() {
        assertFalse(service.verifyPasswordForBackup("admin@forkmyfolio.com", null),
                "A missing password must fail closed");
        verifyNoInteractions(userService);
    }

    @Test
    void whitespaceOnlyPassword_isRejected() {
        assertFalse(service.verifyPasswordForBackup("admin@forkmyfolio.com", "   "),
                "A whitespace-only password must fail closed");
        verifyNoInteractions(userService);
    }

    @Test
    void incorrectPassword_isRejected() {
        when(userService.verifyCurrentUserPassword("wrong-password")).thenReturn(false);
        assertFalse(service.verifyPasswordForBackup("admin@forkmyfolio.com", "wrong-password"));
    }

    @Test
    void correctPassword_isAccepted() {
        when(userService.verifyCurrentUserPassword("correct-password")).thenReturn(true);
        assertTrue(service.verifyPasswordForBackup("admin@forkmyfolio.com", "correct-password"),
                "Legitimate re-verification with the real password must keep working");
    }

    @Test
    void nonAdminUser_cannotElevateViaFallback() {
        // The role-based fallback is removed: verification must never consult
        // the email-to-user lookup (the former role check) to grant access.
        when(userService.verifyCurrentUserPassword("wrong-password")).thenReturn(false);
        assertFalse(service.verifyPasswordForBackup("user@example.com", "wrong-password"),
                "No fallback may grant backup privileges");
        verify(userService, never()).findByEmail(anyString());
    }
}
