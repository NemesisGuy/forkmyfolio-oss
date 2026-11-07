package com.forkmyfolio.security;

import java.util.UUID;

import com.forkmyfolio.config.AppProperties;
import com.forkmyfolio.model.User;
import com.forkmyfolio.security.oauth2.UserPrincipal;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.Date;

/**
 * Unified component for creating and validating JWT tokens for both standard
 * login and OAuth2 flows.
 * This is the single source of truth for all JWT operations in the application.
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    /** Backup re-verification tokens are valid for 30 minutes. */
    private static final long BACKUP_TOKEN_TTL_MS = 30 * 60 * 1000L;

    private final AppProperties appProperties;
    private final Key key;

    public JwtTokenProvider(AppProperties appProperties) {
        this.appProperties = appProperties;
        String secret = appProperties.getJwt().getSecret();

        // FIX: Add validation to ensure the JWT secret is configured.
        // This prevents the application from crashing with a cryptic error if the
        // secret is missing.
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException(
                    "JWT secret key is not configured. Please set the 'app.jwt.secret' property in your environment or application properties.");
        }

        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Creates a JWT access token from an Authentication object.
     * The token's subject will be the user's database ID.
     *
     * @param authentication The Authentication object from Spring Security.
     * @return A signed JWT string.
     */
    public String createToken(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        Long userId;

        if (principal instanceof UserPrincipal) {
            userId = ((UserPrincipal) principal).getId();
        } else if (principal instanceof User) {
            userId = ((User) principal).getId();
        } else {
            throw new IllegalArgumentException(
                    "Cannot create token for unsupported principal type: " + principal.getClass().getName());
        }

        return createTokenForUser(userId, null);
    }

    public String createTokenForGuest(Long ownerId, UUID guestPassUuid) {
        return createTokenForUser(ownerId, guestPassUuid);
    }

    /**
     * Creates a short-lived token authorising sensitive backup operations. This
     * is issued only after a successful password re-verification.
     *
     * @param userId the authenticated user's database ID
     * @return a signed JWT with a {@code scope=backup} claim and a 5-minute expiry
     */
    public String createBackupToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + BACKUP_TOKEN_TTL_MS);
        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .claim("scope", "backup")
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Validates a backup re-verification token and checks that it belongs to the
     * given user and carries the backup scope.
     *
     * @param token  the backup token
     * @param userId the expected user ID
     * @return {@code true} if the token is valid, unexpired and scoped for backup
     */
    public boolean isBackupTokenValid(String token, Long userId) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith((javax.crypto.SecretKey) key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return "backup".equals(claims.get("scope", String.class))
                    && userId != null
                    && userId.toString().equals(claims.getSubject());
        } catch (Exception ex) {
            logger.debug("Invalid backup re-verification token");
            return false;
        }
    }

    private String createTokenForUser(Long userId, UUID guestPassUuid) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + appProperties.getJwt().getExpirationMs());

        JwtBuilder builder = Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(new Date())
                .setExpiration(expiryDate);

        if (guestPassUuid != null) {
            builder.claim("guestPass", guestPassUuid.toString());
        }

        return builder.signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Extracts claims from the JWT token.
     *
     * @param token The JWT token.
     * @return The Claims object.
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the user ID from the JWT token's subject.
     *
     * @param token The JWT token.
     * @return The user ID (as Long).
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Validates JWT token.
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().verifyWith((javax.crypto.SecretKey) key).build().parseSignedClaims(authToken);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException ex) {
            logger.error("Invalid JWT signature");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty.");
        }
        return false;
    }
}