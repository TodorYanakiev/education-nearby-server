package com.dev.education_nearby_server.integration.services;

import com.dev.education_nearby_server.models.dto.auth.AuthenticationRequest;
import com.dev.education_nearby_server.models.dto.auth.AuthenticationResponse;
import com.dev.education_nearby_server.models.dto.auth.RegisterRequest;
import com.dev.education_nearby_server.models.entity.Token;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.repositories.TokenRepository;
import com.dev.education_nearby_server.repositories.UserRepository;
import com.dev.education_nearby_server.services.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AuthenticationServiceIT {

    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TokenRepository tokenRepository;

    private RegisterRequest buildRegisterRequest() {
        return RegisterRequest.builder()
                .firstname("Jane")
                .lastname("Doe")
                .email("jane.doe@example.com")
                .username("jane.doe@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();
    }

    @Test
    void registerPersistsUserAndToken() {
        AuthenticationResponse response = authenticationService.register(buildRegisterRequest());

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();

        User user = userRepository.findByEmail("jane.doe@example.com").orElseThrow();
        assertThat(user.isEnabled()).isTrue();

        List<Token> tokens = tokenRepository.findAll();
        assertThat(tokens).hasSize(1);
        assertThat(tokens.getFirst().getTokenValue()).isEqualTo(response.getAccessToken());
        assertThat(tokens.getFirst().isExpired()).isFalse();
        assertThat(tokens.getFirst().isRevoked()).isFalse();
    }

    @Test
    void authenticateWithValidCredentialsReturnsNewTokens() {
        RegisterRequest request = buildRegisterRequest();
        AuthenticationResponse registration = authenticationService.register(request);
        tokenRepository.findByToken(registration.getAccessToken()).ifPresent(token -> {
            token.setTokenValue(token.getTokenValue() + "-old");
            tokenRepository.save(token);
        });

        AuthenticationResponse authResponse = authenticationService.authenticate(AuthenticationRequest.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .build());

        assertThat(authResponse.getAccessToken()).isNotBlank();
        assertThat(authResponse.getRefreshToken()).isNotBlank();

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        List<Token> tokens = tokenRepository.findAllValidTokenByUser(user.getId());
        assertThat(tokens).hasSize(1);
        assertThat(tokens.getFirst().getTokenValue()).isEqualTo(authResponse.getAccessToken());
        assertThat(tokens.getFirst().isExpired()).isFalse();
        assertThat(tokens.getFirst().isRevoked()).isFalse();
    }

    @Test
    void refreshTokenProvidesNewAccessToken() throws IOException{
        AuthenticationResponse registration = authenticationService.register(buildRegisterRequest());
        tokenRepository.findByToken(registration.getAccessToken()).ifPresent(token -> {
            token.setTokenValue(token.getTokenValue() + "-old");
            tokenRepository.save(token);
        });
        User user = userRepository.findByEmail("jane.doe@example.com").orElseThrow();
        assertThat(tokenRepository.findAllValidTokenByUser(user.getId())).hasSize(1);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + registration.getRefreshToken());
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticationService.refreshToken(request, response);

        String responseBody = response.getContentAsString();
        assertThat(responseBody).contains("access_token").contains(registration.getRefreshToken());

        List<Token> tokens = tokenRepository.findAllValidTokenByUser(user.getId());
        assertThat(tokens).hasSize(1);
        assertThat(tokens.getFirst().getTokenValue()).isNotBlank();
        assertThat(tokens.getFirst().isExpired()).isFalse();
        assertThat(tokens.getFirst().isRevoked()).isFalse();
    }
}
