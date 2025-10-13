package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.config.JwtService;
import com.dev.education_nearby_server.enums.Role;
import com.dev.education_nearby_server.exceptions.user.UserCreateException;
import com.dev.education_nearby_server.exceptions.user.UserDisabledException;
import com.dev.education_nearby_server.models.dto.auth.AuthenticationRequest;
import com.dev.education_nearby_server.models.dto.auth.AuthenticationResponse;
import com.dev.education_nearby_server.models.dto.auth.RegisterRequest;
import com.dev.education_nearby_server.models.entity.Token;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.repositories.TokenRepository;
import com.dev.education_nearby_server.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TokenRepository tokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthenticationService authenticationService;

    private RegisterRequest.RegisterRequestBuilder baseRegisterRequestBuilder;

    @BeforeEach
    void setUp() {
        baseRegisterRequestBuilder = RegisterRequest.builder()
                .firstname("John")
                .lastname("Doe")
                .email("john.doe@example.com")
                .username("johnny")
                .password("password123")
                .repeatedPassword("password123");
    }

    @Test
    void registerThrowsWhenEmailExists() {
        RegisterRequest request = baseRegisterRequestBuilder.build();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(User.builder().build()));

        assertThrows(UserCreateException.class, () -> authenticationService.register(request));
        verify(userRepository, never()).save(any(User.class));
        verify(tokenRepository, never()).save(any(Token.class));
    }

    @Test
    void registerThrowsWhenUsernameExists() {
        RegisterRequest request = baseRegisterRequestBuilder.build();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.of(User.builder().build()));

        assertThrows(UserCreateException.class, () -> authenticationService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerThrowsWhenPasswordsDoNotMatch() {
        RegisterRequest request = baseRegisterRequestBuilder
                .repeatedPassword("different")
                .build();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());

        assertThrows(UserCreateException.class, () -> authenticationService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerThrowsWhenPasswordTooShort() {
        RegisterRequest request = baseRegisterRequestBuilder
                .password("short")
                .repeatedPassword("short")
                .build();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());

        assertThrows(UserCreateException.class, () -> authenticationService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerPersistsUserAndTokens() {
        RegisterRequest request = baseRegisterRequestBuilder.build();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPass");

        User persistedUser = User.builder()
                .id(1L)
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .username(request.getUsername())
                .password("encodedPass")
                .role(Role.USER)
                .enabled(true)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(persistedUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");

        AuthenticationResponse response = authenticationService.register(request);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("encodedPass", savedUser.getPassword());
        assertEquals(Role.USER, savedUser.getRole());
        assertThat(savedUser.isEnabled()).isTrue();

        ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        Token savedToken = tokenCaptor.getValue();
        assertEquals("access-token", savedToken.getTokenValue());
        assertThat(savedToken.getTokenType()).isEqualTo(com.dev.education_nearby_server.enums.TokenType.BEARER);
        assertThat(savedToken.isExpired()).isFalse();
        assertThat(savedToken.isRevoked()).isFalse();
        assertThat(savedToken.getUser()).isEqualTo(persistedUser);
    }

    @Test
    void authenticateThrowsWhenUserDisabled() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("john.doe@example.com")
                .password("password123")
                .build();
        User disabledUser = User.builder().enabled(false).build();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(disabledUser));

        assertThrows(UserDisabledException.class, () -> authenticationService.authenticate(request));
    }

    @Test
    void authenticateReturnsTokensAndRevokesOldOnes() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("john.doe@example.com")
                .password("password123")
                .build();
        User user = User.builder()
                .id(42L)
                .email(request.getEmail())
                .username("johnny")
                .enabled(true)
                .password("encoded")
                .build();
        Token existingToken = Token.builder()
                .tokenValue("old-token")
                .expired(false)
                .revoked(false)
                .user(user)
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        Authentication authentication = new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());
        doReturn(authentication).when(authenticationManager).authenticate(any(Authentication.class));
        when(tokenRepository.findAllValidTokenByUser(user.getId())).thenReturn(List.of(existingToken));
        when(jwtService.generateToken(user)).thenReturn("new-access");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh");

        AuthenticationResponse response = authenticationService.authenticate(request);

        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
        assertThat(existingToken.isExpired()).isTrue();
        assertThat(existingToken.isRevoked()).isTrue();

        ArgumentCaptor<Authentication> authCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(authCaptor.capture());
        Authentication authRequest = authCaptor.getValue();
        assertThat(authRequest).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertEquals(request.getEmail(), authRequest.getPrincipal());
        assertEquals(request.getPassword(), authRequest.getCredentials());

        verify(tokenRepository).saveAll(anyList());
        verify(tokenRepository).save(any(Token.class));
    }

    @Test
    void refreshTokenDoesNothingWhenHeaderMissing() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticationService.refreshToken(request, response);

        verifyNoInteractions(jwtService, tokenRepository);
    }

    @Test
    void refreshTokenWritesNewTokensWhenValid() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer refresh-token");

        User user = User.builder()
                .id(10L)
                .username("johnny")
                .password("encoded")
                .enabled(true)
                .build();

        when(jwtService.extractUsername("refresh-token")).thenReturn("johnny");
        when(userRepository.findByUsername("johnny")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("refresh-token", user)).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("new-access-token");
        when(tokenRepository.findAllValidTokenByUser(user.getId())).thenReturn(Collections.emptyList());

        authenticationService.refreshToken(request, response);

        assertThat(response.getContentAsString()).contains("new-access-token");
        assertThat(response.getContentAsString()).contains("refresh-token");

        verify(tokenRepository).save(any(Token.class));
    }
}
