package com.dev.education_nearby_server.integration.controllers;

import com.dev.education_nearby_server.enums.TokenType;
import com.dev.education_nearby_server.models.dto.auth.AuthenticationRequest;
import com.dev.education_nearby_server.models.dto.auth.AuthenticationResponse;
import com.dev.education_nearby_server.models.dto.auth.ForgotPasswordRequest;
import com.dev.education_nearby_server.models.dto.auth.RegisterRequest;
import com.dev.education_nearby_server.models.dto.auth.ResetForgottenPasswordRequest;
import com.dev.education_nearby_server.repositories.TokenRepository;
import com.dev.education_nearby_server.repositories.UserRepository;
import com.dev.education_nearby_server.services.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationControllerIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private EmailService emailService;

    @AfterEach
    void cleanUp() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerEndpointCreatesUser() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .firstname("Alice")
                .lastname("Liddell")
                .email("alice@example.com")
                .username("alice@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();

        AuthenticationResponse response = registerViaHttp(request);

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(userRepository.findByEmail(request.getEmail())).isPresent();
    }

    @Test
    void authenticateEndpointReturnsTokens() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .firstname("Bob")
                .lastname("Builder")
                .email("bob@example.com")
                .username("bob@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();
        AuthenticationResponse registration = registerViaHttp(request);
        markExistingAccessTokenAsOld(registration.getAccessToken());

        AuthenticationRequest authRequest = AuthenticationRequest.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthenticationResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), AuthenticationResponse.class);
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
    }

    @Test
    void refreshTokenEndpointIssuesNewAccessToken() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .firstname("Charlie")
                .lastname("Bucket")
                .email("charlie@example.com")
                .username("charlie@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();
        AuthenticationResponse registration = registerViaHttp(request);
        markExistingAccessTokenAsOld(registration.getAccessToken());

        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .header("Authorization", "Bearer " + registration.getRefreshToken()))
                .andExpect(status().isOk())
                .andReturn();

        AuthenticationResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), AuthenticationResponse.class);
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isEqualTo(registration.getRefreshToken());
    }

    @Test
    void forgotPasswordEndpointCreatesResetTokenAndReturnsGenericMessage() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .firstname("Daisy")
                .lastname("Ridley")
                .email("daisy@example.com")
                .username("daisy@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();
        registerViaHttp(request);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ForgotPasswordRequest.builder()
                                .email("daisy@example.com")
                                .build())))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .isEqualTo("If an account with that email exists, we have sent a verification code.");
        assertThat(tokenRepository.findAll().stream()
                .filter(token -> token.getTokenType() == TokenType.PASSWORD_RESET))
                .hasSize(1);
        verify(emailService).sendPasswordResetEmail(anyString(), anyString(), anyLong());
    }

    @Test
    void resetForgottenPasswordEndpointChangesPasswordWithValidCode() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .firstname("Eve")
                .lastname("Polastri")
                .email("eve@example.com")
                .username("eve@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();
        registerViaHttp(request);

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ForgotPasswordRequest.builder()
                                .email("eve@example.com")
                                .build())))
                .andExpect(status().isOk());

        String resetCode = tokenRepository.findAll().stream()
                .filter(token -> token.getTokenType() == TokenType.PASSWORD_RESET)
                .map(token -> token.getTokenValue())
                .findFirst()
                .orElseThrow();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/forgot-password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ResetForgottenPasswordRequest.builder()
                                .email("eve@example.com")
                                .verificationCode(resetCode)
                                .newPassword("UpdatedPassword123")
                                .confirmationPassword("UpdatedPassword123")
                                .build())))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).isEqualTo("Password has been reset successfully.");
        assertThat(passwordEncoder.matches(
                "UpdatedPassword123",
                userRepository.findByEmail("eve@example.com").orElseThrow().getPassword()
        )).isTrue();
    }

    private AuthenticationResponse registerViaHttp(RegisterRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthenticationResponse.class);
    }

    private void markExistingAccessTokenAsOld(String token) {
        tokenRepository.findByToken(token).ifPresent(existing -> {
            existing.setTokenValue(existing.getTokenValue() + "-old");
            tokenRepository.save(existing);
        });
    }
}
