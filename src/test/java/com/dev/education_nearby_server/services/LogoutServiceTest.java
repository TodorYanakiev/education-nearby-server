package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.models.entity.Token;
import com.dev.education_nearby_server.repositories.TokenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private LogoutService logoutService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logoutDoesNothingWhenHeaderMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        logoutService.logout(request, response, null);

        verify(tokenRepository, never()).findByToken(any());
    }

    @Test
    void logoutMarksTokenAndClearsContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer test-token");

        Token token = Token.builder()
                .tokenValue("test-token")
                .expired(false)
                .revoked(false)
                .build();
        when(tokenRepository.findByToken("test-token")).thenReturn(Optional.of(token));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "credentials"));

        logoutService.logout(request, response, SecurityContextHolder.getContext().getAuthentication());

        assertThat(token.isExpired()).isTrue();
        assertThat(token.isRevoked()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenRepository).save(token);
    }
}
