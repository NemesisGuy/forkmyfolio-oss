package com.forkmyfolio.security;

import com.forkmyfolio.model.GuestPass;
import com.forkmyfolio.model.User;
import com.forkmyfolio.repository.GuestPassRepository;
import com.forkmyfolio.repository.UserRepository;
import com.forkmyfolio.security.oauth2.UserPrincipal;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * Custom JWT authentication filter that intercepts requests to validate JWT
 * tokens.
 * If a valid token is found, it sets the authentication in Spring Security's
 * context.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private GuestPassRepository guestPassRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private UserRepository userRepository;

    /**
     * Constructs a {@code JwtAuthenticationFilter} with necessary services.
     *
     * @param tokenProvider            Service for JWT operations.
     * @param customUserDetailsService Service for loading user details.
     */
    @Autowired
    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, CustomUserDetailsService customUserDetailsService) {
        this.tokenProvider = tokenProvider;
        this.customUserDetailsService = customUserDetailsService;
    }

    /**
     * Performs the filtering logic for JWT authentication.
     * Extracts token from request, validates it, loads user details, and sets
     * authentication.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();
        logger.debug("JwtFilter: Processing {} request to {}", method, path);

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                Claims claims = tokenProvider.getClaimsFromToken(jwt);
                String sub = claims.getSubject();
                logger.debug("JwtFilter: Valid token for subject {}", sub);
                Long userId = Long.parseLong(sub);

                UserDetails userDetails;
                if (claims.containsKey("guestPass")) {
                    String guestPassUuidStr = claims.get("guestPass", String.class);
                    logger.debug("JwtFilter: Validating guest pass claim for UUID string: {}", guestPassUuidStr);
                    if (guestPassUuidStr == null) {
                        logger.error("JwtFilter: guestPass claim is present but value is NULL");
                        filterChain.doFilter(request, response);
                        return;
                    }

                    UUID guestPassUuid = null;
                    boolean validateGuestPass = (guestPassRepository != null);
                    if (validateGuestPass) {
                        try {
                            guestPassUuid = UUID.fromString(guestPassUuidStr);
                        } catch (IllegalArgumentException e) {
                            logger.warn("JwtFilter: Invalid guest pass UUID: {}", guestPassUuidStr);
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter()
                                    .write("{\"error\": \"Unauthorized\", \"message\": \"Guest pass invalid\"}");
                            return;
                        }
                        logger.debug("JwtFilter: Parsed UUID: {}", guestPassUuid);

                        Optional<GuestPass> guestPassOpt = guestPassRepository.findByUuid(guestPassUuid);
                        if (guestPassOpt.isEmpty()) {
                            logger.warn("JwtFilter: Guest pass {} NOT FOUND in database. Revoking access.", guestPassUuid);
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter()
                                    .write("{\"error\": \"Unauthorized\", \"message\": \"Guest pass revoked or invalid\"}");
                            return;
                        }

                        GuestPass pass = guestPassOpt.get();
                        if (pass.isExpired()) {
                            logger.warn("JwtFilter: Guest pass {} is EXPIRED (expires at {}).", guestPassUuid,
                                    pass.getExpiresAt());
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter()
                                    .write("{\"error\": \"Unauthorized\", \"message\": \"Guest pass expired\"}");
                            return;
                        }
                    }

                    // Create a "Virtual" guest user tied to the owner's ID.
                    if (userRepository != null) {
                        User owner = userRepository.findById(userId)
                                .orElseThrow(
                                        () -> new RuntimeException("Owner not found for guest pass subject: " + userId));
                        userDetails = UserPrincipal.create(owner, Collections.singletonList(
                                new SimpleGrantedAuthority("ROLE_GUEST")));
                        logger.debug("JwtFilter: Access GRANTED via Guest Pass {} for owner: {}", guestPassUuid,
                                owner.getEmail());
                    } else {
                        // Fallback for environments without the repository (e.g. unit tests).
                        UserDetails loaded = customUserDetailsService.loadUserById(userId);
                        String ownerEmail = (loaded != null) ? loaded.getUsername() : String.valueOf(userId);
                        userDetails = new UserPrincipal(userId, ownerEmail, null, true,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_GUEST")));
                        logger.debug("JwtFilter: Access GRANTED via Guest Pass {} for owner: {}", guestPassUuid,
                                ownerEmail);
                    }
                } else {
                    userDetails = customUserDetailsService.loadUserById(userId);
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("JwtFilter: Authentication SET for user: {}", sub);
            }
        } catch (Exception ex) {
            logger.error("JwtFilter: ERROR setting user authentication: {} - {}", ex.getClass().getName(),
                    ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT from the "Authorization" header of the request.
     * The token is expected to be prefixed with "Bearer ".
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
