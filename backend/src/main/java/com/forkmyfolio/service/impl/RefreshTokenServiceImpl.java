package com.forkmyfolio.service.impl;

import com.forkmyfolio.config.AppProperties;
import com.forkmyfolio.exception.TokenRefreshException;
import com.forkmyfolio.model.RefreshToken;
import com.forkmyfolio.model.User;
import com.forkmyfolio.repository.RefreshTokenRepository;
import com.forkmyfolio.repository.UserRepository;
import com.forkmyfolio.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of the {@link RefreshTokenService} interface.
 */
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final Long refreshTokenDurationMs;

    /**
     * Constructs a {@code RefreshTokenServiceImpl}.
     *
     * @param refreshTokenRepository Repository for refresh token data.
     * @param userRepository         Repository for user data.
     * @param appProperties          Application configuration properties.
     */
    @Autowired
    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository, AppProperties appProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.refreshTokenDurationMs = appProperties.getJwt().getRefreshExpirationMs();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByTokenAndFetchUserWithRoles(token);
    }

    /**
     * {@inheritDoc}
     * This implementation will delete any existing refresh token for the user before creating a new one,
     * enforcing a one-active-refresh-token-per-user policy.
     */
    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Invalidate existing tokens for the user to enforce one active refresh token per user
        refreshTokenRepository.findByUser(user).ifPresent(refreshTokenRepository::delete);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString());

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RefreshToken verifyExpiration(RefreshToken token) throws TokenRefreshException {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new sign-in request");
        }
        return token;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int deleteByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId + " - cannot delete refresh tokens."));
        return refreshTokenRepository.deleteByUser(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken oldToken) {
        // 1. Delete the old token that was just used.
        refreshTokenRepository.delete(oldToken);

        // 2. Create a new token for the same user.
        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setUser(oldToken.getUser());
        newRefreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        newRefreshToken.setToken(UUID.randomUUID().toString());

        // 3. Save and return the new token.
        return refreshTokenRepository.save(newRefreshToken);
    }


}
