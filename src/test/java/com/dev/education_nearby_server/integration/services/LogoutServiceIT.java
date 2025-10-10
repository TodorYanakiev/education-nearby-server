package com.dev.education_nearby_server.integration.services;

import com.dev.education_nearby_server.models.dto.auth.RegisterRequest;
import com.dev.education_nearby_server.models.entity.Token;
import com.dev.education_nearby_server.repositories.TokenRepository;
import com.dev.education_nearby_server.services.AuthenticationService;
import com.dev.education_nearby_server.services.LogoutService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class LogoutServiceIT {

    @Autowired
    private LogoutService logoutService;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private TokenRepository tokenRepository;

    @Test
    void logoutRevokesStoredToken() {
        var registerResponse = authenticationService.register(RegisterRequest.builder()
                .firstname("Laura")
                .lastname("Palmer")
                .email("laura.palmer@example.com")
                .username("laura.palmer@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + registerResponse.getAccessToken());
        MockHttpServletResponse response = new MockHttpServletResponse();

        logoutService.logout(request, response, null);

        Token stored = tokenRepository.findByToken(registerResponse.getAccessToken()).orElseThrow();
        assertThat(stored.isRevoked()).isTrue();
        assertThat(stored.isExpired()).isTrue();
    }
}
