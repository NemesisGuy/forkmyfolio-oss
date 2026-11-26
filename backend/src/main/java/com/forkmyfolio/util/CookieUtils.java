package com.forkmyfolio.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.util.Optional;

/**
 * Utility class for handling HTTP cookies.
 * Provides helper methods for adding, retrieving, deleting cookies.
 *
 * <p>Native Java serialization helpers were removed in v1.2.11 — they were
 * used to store an unsigned OAuth2 authorization request in a cookie, which
 * exposed a deserialization-gadget attack path. See
 * {@code OAuth2AuthorizationRequestCodec} for the signed-JSON replacement.
 */
public class CookieUtils {

    /**
     * Retrieves a cookie by its name from the request.
     *
     * @param request The HTTP request.
     * @param name    The name of the cookie to retrieve.
     * @return An Optional containing the cookie if found, otherwise an empty Optional.
     */
    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Adds a new cookie to the HTTP response.
     *
     * @param response The HTTP response.
     * @param name     The name of the cookie.
     * @param value    The value of the cookie.
     * @param maxAge   The maximum age of the cookie in seconds.
     */
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    /**
     * Deletes a cookie by setting its max age to 0.
     *
     * @param request  The HTTP request.
     * @param response The HTTP response.
     * @param name     The name of the cookie to delete.
     */
    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
    }

    /**
     * Adds a hardened cookie (Secure + SameSite configurable) via Set-Cookie.
     * Used for short-lived OAuth2 state cookies, which must be HttpOnly and
     * Lax so the IdP's cross-site GET callback still carries them.
     */
    public static void addCookie(HttpServletResponse response, String name, String value,
                                 int maxAge, boolean secure, String sameSite) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}