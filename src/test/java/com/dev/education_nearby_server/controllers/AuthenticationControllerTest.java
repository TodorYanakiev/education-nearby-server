package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.models.dto.auth.AuthenticationRequest;
import com.dev.education_nearby_server.models.dto.auth.AuthenticationResponse;
import com.dev.education_nearby_server.models.dto.auth.OAuth2CompleteRegistrationRequest;
import com.dev.education_nearby_server.models.dto.auth.RegisterRequest;
import com.dev.education_nearby_server.services.AuthenticationService;
import com.dev.education_nearby_server.services.oauth2.OAuth2AuthenticationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private OAuth2AuthenticationService oauth2AuthenticationService;

    @InjectMocks
    private AuthenticationController controller;

    @Test
    void registerDelegatesToService() {
        RegisterRequest request = RegisterRequest.builder()
                .email("john.doe@example.com")
                .password("password123")
                .repeatedPassword("password123")
                .build();
        AuthenticationResponse response = AuthenticationResponse.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .build();
        when(authenticationService.register(request)).thenReturn(response);

        ResponseEntity<AuthenticationResponse> result = controller.register(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertThat(result.getBody()).isEqualTo(response);
        verify(authenticationService).register(request);
    }

    @Test
    void authenticateDelegatesToService() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("john.doe@example.com")
                .password("password123")
                .build();
        AuthenticationResponse response = AuthenticationResponse.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .build();
        when(authenticationService.authenticate(request)).thenReturn(response);

        ResponseEntity<AuthenticationResponse> result = controller.authenticate(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertThat(result.getBody()).isEqualTo(response);
        verify(authenticationService).authenticate(request);
    }

    @Test
    void refreshTokenInvokesService() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.refreshToken(request, response);

        verify(authenticationService).refreshToken(any(), any());
    }

    @Test
    void completeOauth2RegistrationDelegatesToService() {
        OAuth2CompleteRegistrationRequest request = OAuth2CompleteRegistrationRequest.builder()
                .registrationToken("token")
                .username("new-user")
                .email("new-user@example.com")
                .firstname("New")
                .lastname("User")
                .build();
        AuthenticationResponse response = AuthenticationResponse.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .build();
        when(oauth2AuthenticationService.completeRegistration(request)).thenReturn(response);

        ResponseEntity<AuthenticationResponse> result = controller.completeOauth2Registration(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertThat(result.getBody()).isEqualTo(response);
        verify(oauth2AuthenticationService).completeRegistration(request);
    }
}
