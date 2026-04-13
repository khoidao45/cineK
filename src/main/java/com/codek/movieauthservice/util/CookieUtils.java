package com.codek.movieauthservice.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/**
 * Cookie helpers used by the OAuth2 authorization request repository
 * to persist the OAuth2 state across the redirect round-trip without
 * needing an HTTP session (STATELESS mode).
 */
public class CookieUtils {

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> c.getName().equals(name))
                .findFirst();
    }

    public static void addCookie(HttpServletResponse response, String name,
                                 String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAgeSeconds);
        response.addCookie(cookie);
    }

    public static void deleteCookie(HttpServletRequest request,
                                    HttpServletResponse response, String name) {
        if (request.getCookies() == null) return;
        Arrays.stream(request.getCookies())
                .filter(c -> c.getName().equals(name))
                .forEach(c -> {
                    // Create a new Cookie instead of mutating c in-place;
                    // mutating c would corrupt subsequent getCookie() calls on the same request
                    Cookie expired = new Cookie(name, "");
                    expired.setPath("/");
                    expired.setMaxAge(0);
                    response.addCookie(expired);
                });
    }

    public static String serialize(Object object) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            // withoutPadding() avoids '=' chars in cookie value which confuse nginx/browsers
            return Base64.getUrlEncoder().withoutPadding().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize cookie value", e);
        }
    }

    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        String value = cookie.getValue();
        if (value == null || value.isEmpty()) return null;
        byte[] bytes = Base64.getUrlDecoder().decode(value);
        if (bytes.length == 0) return null;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return cls.cast(ois.readObject());
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to deserialize cookie value", e);
        }
    }
}
