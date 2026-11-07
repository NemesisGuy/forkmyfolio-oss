package com.forkmyfolio.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.SerializationUtils;

import java.util.Base64;
import java.util.Optional;

/**
 * Utility class for handling HTTP cookies.
 * Provides helper methods for adding, retrieving, deleting, and serializing/deserializing cookies.
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
     * Serializes an object into a Base64-encoded string.
     *
     * @param object The object to serialize.
     * @return A string representation of the serialized object.
     */
    public static String serialize(Object object) {
        return Base64.getUrlEncoder()
                .encodeToString(SerializationUtils.serialize(object));
    }

    /**
     * Deserializes a cookie's value back into an object.
     *
     * @param cookie The cookie to deserialize.
     * @param cls    The class of the target object.
     * @param <T>    The type of the target object.
     * @return The deserialized object.
     */
    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        byte[] data = Base64.getUrlDecoder().decode(cookie.getValue());
        return cls.cast(SerializationUtils.deserialize(data));
    }
}