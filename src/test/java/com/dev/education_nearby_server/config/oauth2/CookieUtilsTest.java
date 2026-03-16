package com.dev.education_nearby_server.config.oauth2;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.Serializable;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CookieUtilsTest {

    @Test
    void getCookieReturnsEmptyWhenNoCookiesPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        Optional<Cookie> result = CookieUtils.getCookie(request, "missing");

        assertThat(result).isEmpty();
    }

    @Test
    void getCookieReturnsMatchingCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("a", "1"), new Cookie("target", "value"));

        Optional<Cookie> result = CookieUtils.getCookie(request, "target");

        assertThat(result).isPresent();
        assertThat(result.get().getValue()).isEqualTo("value");
    }

    @Test
    void getCookieReturnsEmptyWhenTargetNotFound() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("a", "1"), new Cookie("b", "2"));

        Optional<Cookie> result = CookieUtils.getCookie(request, "target");

        assertThat(result).isEmpty();
    }

    @Test
    void addCookieAddsSecureHttpOnlyCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        CookieUtils.addCookie(response, "token", "abc", 120);

        Cookie cookie = response.getCookie("token");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getMaxAge()).isEqualTo(120);
    }

    @Test
    void deleteCookieAddsExpiredSecureHttpOnlyCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("token", "abc"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        CookieUtils.deleteCookie(request, response, "token");

        Cookie cookie = response.getCookie("token");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isZero();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
    }

    @Test
    void deleteCookieDoesNothingWhenRequestHasNoCookies() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        CookieUtils.deleteCookie(request, response, "token");

        assertThat(response.getCookies()).isEmpty();
    }

    @Test
    void serializeAndDeserializeRoundTrip() {
        TestPayload payload = new TestPayload("email@example.com", 42);

        String serialized = CookieUtils.serialize(payload);
        Cookie cookie = new Cookie("oauth", serialized);
        TestPayload restored = CookieUtils.deserialize(cookie, TestPayload.class);

        assertThat(restored).isEqualTo(payload);
    }

    private record TestPayload(String email, int value) implements Serializable {
    }
}
