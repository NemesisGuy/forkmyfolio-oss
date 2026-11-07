package com.forkmyfolio.controller;

import com.forkmyfolio.config.AppProperties;
import com.forkmyfolio.dto.request.GuestLoginRequest;
import com.forkmyfolio.dto.request.LoginRequest;
import com.forkmyfolio.dto.request.RegisterRequest;
import com.forkmyfolio.dto.response.AuthResponse;
import com.forkmyfolio.dto.response.UserDto;
import com.forkmyfolio.exception.TokenRefreshException;
import com.forkmyfolio.mapper.UserMapper;
import com.forkmyfolio.model.GuestPass;
import com.forkmyfolio.model.RefreshToken;
import com.forkmyfolio.model.User;
import com.forkmyfolio.security.JwtTokenProvider;
import com.forkmyfolio.security.oauth2.UserPrincipal;
import com.forkmyfolio.service.AuthService;
import com.forkmyfolio.service.BackupTokenService;
import com.forkmyfolio.service.GuestPassService;
import com.forkmyfolio.service.RefreshTokenService;
import com.forkmyfolio.service.impl.VisitorStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration, login, logout, and token refresh.")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final GuestPassService guestPassService;
    private final VisitorStatsService visitorStatsService;
    private final BackupTokenService backupTokenService;
    private final AppProperties appProperties;
    private final com.forkmyfolio.service.UserService userService;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, AuthService authService,
            JwtTokenProvider tokenProvider, RefreshTokenService refreshTokenService, UserMapper userMapper,
            VisitorStatsService visitorStatsService, AppProperties appProperties, GuestPassService guestPassService,
            BackupTokenService backupTokenService, com.forkmyfolio.service.UserService userService) {
        this.authenticationManager = authenticationManager;
        this.authService = authService;
        this.tokenProvider = tokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
        this.visitorStatsService = visitorStatsService;
        this.appProperties = appProperties;
        this.guestPassService = guestPassService;
        this.backupTokenService = backupTokenService;
        this.userService = userService;
    }

    @PostMapping("/verify-password")
    @Operation(summary = "Re-verify the current user's password",
            description = "Backend-enforced re-authentication for sensitive operations such as backup export/restore. "
                    + "The supplied password is never stored or logged.")
    public ResponseEntity<Map<String, Object>> verifyPassword(
            @Valid @RequestBody com.forkmyfolio.dto.request.VerifyPasswordRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null) ? auth.getName() : null;

        // Verification rules (incl. ADMIN fallback) live in the service layer;
        // the backup-token lifecycle stays with JwtTokenProvider, mirroring
        // BackupController's existing pattern.
        boolean valid = backupTokenService.verifyPasswordForBackup(email, request.getPassword());
        String backupToken = (valid && email != null)
                ? userService.findByEmail(email).map(u -> tokenProvider.createBackupToken(u.getId())).orElse(null)
                : null;

        if (backupToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "valid", false));
        }
        return ResponseEntity.ok(Map.of("status", "success", "valid", true, "backupToken", backupToken));
    }

    @PostMapping("/guest-login")
    @Operation(summary = "Exchange a guest pass token for a JWT session")
    public ResponseEntity<AuthResponse> loginAsGuest(@Valid @RequestBody GuestLoginRequest request) {
        logger.info("Received guest login attempt with token");

        GuestPass guestPass = guestPassService.validateToken(request.getToken());

        // Issue a guest JWT tied to the portfolio owner
        String jwt = tokenProvider.createTokenForGuest(guestPass.getOwner().getId(), guestPass.getUuid());

        // Guest user representation (Limited)
        User owner = guestPass.getOwner();
        UserDto ownerDto = userMapper.toDto(owner);

        logger.info("Guest login successful for portfolio of owner: {}", owner.getEmail());
        return ResponseEntity.ok(new AuthResponse(jwt, ownerDto));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account after they accept the terms of service.")
    public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest,
            HttpServletResponse response) {
        logger.info("Received registration request for email: {}", registerRequest.getEmail());

        // Map DTO to Entity at the boundary
        User userTemplate = userMapper.toEntity(registerRequest);

        // Call DTO-free service
        User registeredUser = authService.registerUser(
                userTemplate,
                registerRequest.getPassword(),
                registerRequest.getTermsVersion());

        UserPrincipal userPrincipal = UserPrincipal.create(registeredUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userPrincipal, null,
                userPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.createToken(authentication);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(registeredUser);

        ResponseCookie refreshTokenCookie = createRefreshTokenCookie(refreshToken.getToken());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
        logger.info("Set refresh token cookie for user '{}'.", registeredUser.getEmail());

        UserDto userDto = userMapper.toDto(registeredUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(jwt, userDto));
    }

    @PostMapping("/login")
    @Operation(summary = "Login an existing user")
    public ResponseEntity<AuthResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response) {
        logger.info("Received login request for email: {}", loginRequest.getEmail());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.createToken(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userService.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found after successful authentication"));
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        ResponseCookie refreshTokenCookie = createRefreshTokenCookie(refreshToken.getToken());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
        logger.info("Created new refresh token and set cookie for user '{}'.",
                userPrincipal.getEmail());

        UserDto userDto = userMapper.toDto(user);
        return ResponseEntity.ok(new AuthResponse(jwt, userDto));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<AuthResponse> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        logger.info("Received request to refresh token.");
        Cookie cookie = WebUtils.getCookie(request, appProperties.getJwt().getRefreshCookieName());
        if (cookie == null) {
            throw new TokenRefreshException(null, "Refresh token cookie not found. Please log in again.");
        }
        String requestRefreshToken = cookie.getValue();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(verifiedRefreshToken -> {
                    User user = verifiedRefreshToken.getUser();
                    UserPrincipal userPrincipal = UserPrincipal.create(user);

                    Authentication authentication = new UsernamePasswordAuthenticationToken(userPrincipal, null,
                            userPrincipal.getAuthorities());
                    String newAccessToken = tokenProvider.createToken(authentication);

                    RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(verifiedRefreshToken);

                    ResponseCookie newRefreshTokenCookie = createRefreshTokenCookie(newRefreshToken.getToken());
                    response.addHeader(HttpHeaders.SET_COOKIE, newRefreshTokenCookie.toString());
                    logger.info("Rotated refresh token and set new cookie for user '{}'.", user.getEmail());

                    UserDto userDto = userMapper.toDto(user);
                    return ResponseEntity.ok(new AuthResponse(newAccessToken, userDto));
                })
                .orElseThrow(
                        () -> new TokenRefreshException(requestRefreshToken, "Refresh token not found in database!"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout the current user")
    public ResponseEntity<Map<String, String>> logoutUser(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = (authentication != null && !"anonymousUser".equals(authentication.getPrincipal()))
                ? authentication.getName()
                : "anonymous";
        logger.info("Received logout request from user: {}", userEmail);

        Cookie cookie = WebUtils.getCookie(request, appProperties.getJwt().getRefreshCookieName());
        if (cookie != null && cookie.getValue() != null) {
            refreshTokenService.deleteByToken(cookie.getValue());
        }

        ResponseCookie emptyCookie = createRefreshTokenCookie("");
        response.addHeader(HttpHeaders.SET_COOKIE, emptyCookie.toString());
        logger.info("Cleared refresh token cookie for user '{}'.", userEmail);

        SecurityContextHolder.clearContext();
        visitorStatsService.incrementLogoutSuccess();
        return ResponseEntity.ok(Map.of("message", "User logged out successfully."));
    }

    private ResponseCookie createRefreshTokenCookie(String token) {
        long maxAge = StringUtils.hasText(token) ? appProperties.getJwt().getRefreshExpirationMs() / 1000 : 0;

        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie
                .from(appProperties.getJwt().getRefreshCookieName(), token)
                .httpOnly(true)
                .secure(appProperties.getCookie().isSecure())
                .path("/")
                .maxAge(maxAge)
                .sameSite(appProperties.getCookie().getSamesite());

        String cookieDomain = appProperties.getSecurity().getCookieDomain();
        if (StringUtils.hasText(cookieDomain)) {
            cookieBuilder.domain(cookieDomain);
            logger.debug("Setting cookie with domain: {}", cookieDomain);
        } else {
            logger.debug("Setting cookie without a specific domain.");
        }

        return cookieBuilder.build();
    }
}