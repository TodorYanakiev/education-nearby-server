package com.dev.education_nearby_server.config.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationFailureHandlerTest {

    @Mock
    private HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @InjectMocks
    private OAuth2AuthenticationFailureHandler handler;

    @Test
    void onAuthenticationFailureRedirectsWhenRedirectUriConfigured() throws Exception {
        ReflectionTestUtils.setField(handler, "errorRedirectUri", "https://client-app.example.com/oauth2/error");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException exception = new AuthenticationException("OAuth2 failed badly") {
        };

        handler.onAuthenticationFailure(request, response, exception);

        verify(authorizationRequestRepository).removeAuthorizationRequest(request, response);
        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://client-app.example.com/oauth2/error#error=OAuth2+failed+badly");
    }

    @Test
    void onAuthenticationFailureWritesJsonWhenRedirectUriMissing() throws Exception {
        handler = new OAuth2AuthenticationFailureHandler(new ObjectMapper(), authorizationRequestRepository);
        ReflectionTestUtils.setField(handler, "errorRedirectUri", " ");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException exception = new AuthenticationException("Provider rejected account") {
        };

        handler.onAuthenticationFailure(request, response, exception);

        verify(authorizationRequestRepository).removeAuthorizationRequest(request, response);
        assertThat(response.getStatus()).isEqualTo(MockHttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).contains("OAuth2 authentication failed");
        assertThat(response.getContentAsString()).contains("Provider rejected account");
    }
}
