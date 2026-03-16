package com.dev.education_nearby_server.config.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.SerializationUtils;

import java.util.Base64;
import java.util.Optional;

public final class CookieUtils {

    private CookieUtils() {
    }

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return Optional.of(cookie);
            }
        }
        return Optional.empty();
    }

    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        if (request.getCookies() == null) {
            return;
        }
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                Cookie deleteCookie = new Cookie(name, "");
                deleteCookie.setPath("/");
                deleteCookie.setHttpOnly(true);
                deleteCookie.setSecure(true);
                deleteCookie.setMaxAge(0);
                response.addCookie(deleteCookie);
            }
        }
    }

    public static String serialize(Object object) {
        return Base64.getUrlEncoder()
                .encodeToString(SerializationUtils.serialize(object));
    }

    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        byte[] bytes = Base64.getUrlDecoder().decode(cookie.getValue());
        return cls.cast(SerializationUtils.deserialize(bytes));
    }
}
