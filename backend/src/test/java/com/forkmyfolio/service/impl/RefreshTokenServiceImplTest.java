package com.forkmyfolio.service.impl;

import com.forkmyfolio.config.AppProperties;
import com.forkmyfolio.model.RefreshToken;
import com.forkmyfolio.model.User;
import com.forkmyfolio.repository.RefreshTokenRepository;
import com.forkmyfolio.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression tests for concurrent-login refresh-token creation.
 *
 * <p>Two logins for the same user (two tabs/devices, or parallel e2e specs)
 * used to race inside {@code createRefreshToken}: both loaded the same token
 * row, and the loser's entity-level delete affected 0 rows — Hibernate reports
 * that as {@code ObjectOptimisticLockingFailureException}
 * ("delete from refresh_tokens where id=?") and the login failed with HTTP 500
 * (run D: visual-proof).
 *
 * <p>Pin: invalidation MUST go through the idempotent bulk delete
 * ({@code DELETE ... WHERE user_id = ?}), never a load-then-entity-delete.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserRepository userRepository;

    private RefreshTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getJwt().setRefreshExpirationMs(604_800_000L); // 7 days
        service = new RefreshTokenServiceImpl(refreshTokenRepository, userRepository, appProperties);
    }

    @Test
    void createRefreshToken_usesIdempotentBulkDelete_insteadOfEntityDelete() {
        User user = new User();
        user.setId(42L);

        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        RefreshToken created = service.createRefreshToken(user);

        assertNotNull(created, "A refresh token must be created for the login");
        assertNotNull(created.getToken(), "The token string must be populated");
        assertNotNull(created.getExpiryDate(), "The expiry must be populated");
        assertTrue(created.getExpiryDate().isAfter(java.time.Instant.now()),
                "The expiry must be in the future");
        org.junit.jupiter.api.Assertions.assertSame(user, created.getUser());

        // The regression pin: invalidation must be the bulk DML delete. The old
        // findByToken(...).ifPresent(delete) loaded the row into the persistence
        // context, and a concurrent second login's delete of the SAME row then
        // affected 0 rows -> ObjectOptimisticLockingFailureException -> login 500.
        verify(refreshTokenRepository).deleteByUser(user);
        verify(refreshTokenRepository, never()).findByUser(any());
        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
        verify(refreshTokenRepository, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteByToken_usesBulkDelete_notEntityDelete() {
        // Logout path: same race class as createRefreshToken — a token already
        // invalidated by a concurrent rotation must not turn the logout into a
        // 0-row entity delete (ObjectOptimisticLockingFailureException).
        service.deleteByToken("some-token");

        verify(refreshTokenRepository).deleteByToken("some-token");
        verify(refreshTokenRepository, never()).findByToken(anyString());
        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }
}
