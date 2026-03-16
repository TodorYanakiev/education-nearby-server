package com.dev.education_nearby_server.config.oauth2;

import com.dev.education_nearby_server.models.dto.auth.OAuth2LoginResponse;
import com.dev.education_nearby_server.services.oauth2.OAuth2AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private OAuth2AuthenticationService oauth2AuthenticationService;

    @Mock
    private HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler handler;

    @Test
    void onAuthenticationSuccessReturnsUnauthorizedForNonOAuth2Authentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("user", "pass");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(authorizationRequestRepository).removeAuthorizationRequest(request, response);
        assertThat(response.getStatus()).isEqualTo(MockHttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getErrorMessage()).isEqualTo("Invalid OAuth2 authentication.");
    }

    @Test
    void onAuthenticationSuccessReturnsUnauthorizedForUnsupportedProvider() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationToken authentication = oauth2Token("github", Map.of("sub", "123"));

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(authorizationRequestRepository).removeAuthorizationRequest(request, response);
        assertThat(response.getStatus()).isEqualTo(MockHttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getErrorMessage()).isEqualTo("Unsupported OAuth2 provider.");
    }

    @Test
    void onAuthenticationSuccessRedirectsWithCompleteTokensWhenConfigured() throws Exception {
        ReflectionTestUtils.setField(handler, "redirectUri", "https://client-app.example.com/oauth2/callback");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationToken authentication = oauth2Token("google", Map.of("sub", "123"));
        OAuth2LoginResponse loginResponse = OAuth2LoginResponse.builder()
                .status("COMPLETE")
                .accessToken("acc token")
                .refreshToken("ref token")
                .build();
        when(oauth2AuthenticationService.handleOAuth2Login(any(), any())).thenReturn(loginResponse);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(authorizationRequestRepository).removeAuthorizationRequest(request, response);
        verify(oauth2AuthenticationService).handleOAuth2Login(eq(com.dev.education_nearby_server.enums.AuthProvider.GOOGLE), any());
        assertThat(response.getRedirectedUrl()).isEqualTo(
                "https://client-app.example.com/oauth2/callback#status=complete&access_token=acc+token&refresh_token=ref+token");
    }

    @Test
    void onAuthenticationSuccessRedirectsWithPendingRegistrationWhenConfigured() throws Exception {
        ReflectionTestUtils.setField(handler, "redirectUri", "https://client-app.example.com/oauth2/callback");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationToken authentication = oauth2Token("google", Map.of("sub", "123"));
        OAuth2LoginResponse loginResponse = OAuth2LoginResponse.builder()
                .status("pending")
                .registrationToken("reg token")
                .missingFields(List.of("username", "firstname"))
                .build();
        when(oauth2AuthenticationService.handleOAuth2Login(any(), any())).thenReturn(loginResponse);

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo(
                "https://client-app.example.com/oauth2/callback#status=pending&registration_token=reg+token&missing_fields=username%2Cfirstname");
    }

    @Test
    void onAuthenticationSuccessWritesJsonWhenRedirectNotConfigured() throws Exception {
        handler = new OAuth2AuthenticationSuccessHandler(
                oauth2AuthenticationService,
                new ObjectMapper(),
                authorizationRequestRepository
        );
        ReflectionTestUtils.setField(handler, "redirectUri", "");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationToken authentication = oauth2Token("google", Map.of("sub", "123"));
        OAuth2LoginResponse loginResponse = OAuth2LoginResponse.builder()
                .status("COMPLETE")
                .accessToken("access")
                .refreshToken("refresh")
                .build();
        when(oauth2AuthenticationService.handleOAuth2Login(any(), any())).thenReturn(loginResponse);

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).contains("\"status\":\"COMPLETE\"");
        assertThat(response.getContentAsString()).contains("\"access_token\":\"access\"");
    }

    @Test
    void onAuthenticationSuccessRedirectsPendingWithEmptyMissingFieldsWhenNull() throws Exception {
        ReflectionTestUtils.setField(handler, "redirectUri", "https://client-app.example.com/oauth2/callback");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationToken authentication = oauth2Token("google", Map.of("sub", "123"));
        OAuth2LoginResponse loginResponse = OAuth2LoginResponse.builder()
                .status("pending")
                .registrationToken("reg token")
                .missingFields(null)
                .build();
        when(oauth2AuthenticationService.handleOAuth2Login(any(), any())).thenReturn(loginResponse);

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo(
                "https://client-app.example.com/oauth2/callback#status=pending&registration_token=reg+token&missing_fields=");
    }

    private OAuth2AuthenticationToken oauth2Token(String registrationId, Map<String, Object> attributes) {
        OAuth2User oauth2User = new DefaultOAuth2User(List.of(), attributes, "sub");
        return new OAuth2AuthenticationToken(oauth2User, oauth2User.getAuthorities(), registrationId);
    }
}
