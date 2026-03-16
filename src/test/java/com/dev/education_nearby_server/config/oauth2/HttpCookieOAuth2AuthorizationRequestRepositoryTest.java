package com.dev.education_nearby_server.config.oauth2;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import static org.assertj.core.api.Assertions.assertThat;

class HttpCookieOAuth2AuthorizationRequestRepositoryTest {

    private final HttpCookieOAuth2AuthorizationRequestRepository repository =
            new HttpCookieOAuth2AuthorizationRequestRepository();

    @Test
    void loadAuthorizationRequestReturnsNullWhenCookieMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        OAuth2AuthorizationRequest result = repository.loadAuthorizationRequest(request);

        assertThat(result).isNull();
    }

    @Test
    void saveAuthorizationRequestStoresSerializedRequestCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthorizationRequest authRequest = sampleRequest();

        repository.saveAuthorizationRequest(authRequest, request, response);

        Cookie cookie = response.getCookie(HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTH_REQUEST_COOKIE_NAME);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isNotBlank();
        assertThat(cookie.getMaxAge()).isEqualTo(180);
        assertThat(cookie.getPath()).isEqualTo("/");
    }

    @Test
    void loadAuthorizationRequestDeserializesFromCookie() {
        OAuth2AuthorizationRequest original = sampleRequest();
        String serialized = CookieUtils.serialize(original);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTH_REQUEST_COOKIE_NAME, serialized));

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getAuthorizationUri()).isEqualTo(original.getAuthorizationUri());
        assertThat(loaded.getClientId()).isEqualTo(original.getClientId());
        assertThat(loaded.getState()).isEqualTo(original.getState());
    }

    @Test
    void saveAuthorizationRequestWithNullDeletesCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTH_REQUEST_COOKIE_NAME, "value"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(null, request, response);

        Cookie cookie = response.getCookie(HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTH_REQUEST_COOKIE_NAME);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getMaxAge()).isZero();
    }

    @Test
    void removeAuthorizationRequestReturnsRequestAndDeletesCookie() {
        OAuth2AuthorizationRequest original = sampleRequest();
        String serialized = CookieUtils.serialize(original);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTH_REQUEST_COOKIE_NAME, serialized));
        MockHttpServletResponse response = new MockHttpServletResponse();

        OAuth2AuthorizationRequest removed = repository.removeAuthorizationRequest(request, response);

        assertThat(removed).isNotNull();
        assertThat(removed.getClientId()).isEqualTo(original.getClientId());
        Cookie deleteCookie = response.getCookie(HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTH_REQUEST_COOKIE_NAME);
        assertThat(deleteCookie).isNotNull();
        assertThat(deleteCookie.getMaxAge()).isZero();
    }

    private OAuth2AuthorizationRequest sampleRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .clientId("client-id")
                .redirectUri("https://example.com/login/oauth2/code/google")
                .state("xyz")
                .authorizationRequestUri("https://accounts.google.com/o/oauth2/v2/auth?state=xyz")
                .build();
    }
}
