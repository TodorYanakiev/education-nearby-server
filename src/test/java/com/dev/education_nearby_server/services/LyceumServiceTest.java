package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.enums.Role;
import com.dev.education_nearby_server.enums.TokenType;
import com.dev.education_nearby_server.enums.VerificationStatus;
import com.dev.education_nearby_server.exceptions.common.BadRequestException;
import com.dev.education_nearby_server.exceptions.common.ConflictException;
import com.dev.education_nearby_server.exceptions.common.UnauthorizedException;
import com.dev.education_nearby_server.models.dto.request.LyceumRightsRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumRightsVerificationRequest;
import com.dev.education_nearby_server.models.entity.Lyceum;
import com.dev.education_nearby_server.models.entity.Token;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.repositories.LyceumRepository;
import com.dev.education_nearby_server.repositories.TokenRepository;
import com.dev.education_nearby_server.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LyceumServiceTest {

    @Mock
    private LyceumRepository lyceumRepository;
    @Mock
    private TokenRepository tokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private LyceumService lyceumService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestRightsReturnsMessageWhenLyceumMissing() {
        LyceumRightsRequest request = LyceumRightsRequest.builder()
                .lyceumName("Unknown")
                .town("Nowhere")
                .build();
        when(lyceumRepository.findFirstByNameIgnoreCaseAndTownIgnoreCase("Unknown", "Nowhere"))
                .thenReturn(Optional.empty());

        String result = lyceumService.requestRightsOverLyceum(request);

        assertThat(result).isEqualTo("We are sorry, we could not find such lyceum. Please contact us.");
        verify(emailService, never()).sendLyceumVerificationEmail(any(), any(), any(), any());
    }

    @Test
    void requestRightsReturnsMessageWhenLyceumEmailMissing() {
        Lyceum lyceum = createLyceum(10L, "Test Lyceum", "Sofia", null);
        LyceumRightsRequest request = LyceumRightsRequest.builder()
                .lyceumName("Test Lyceum")
                .town("Sofia")
                .build();
        when(lyceumRepository.findFirstByNameIgnoreCaseAndTownIgnoreCase("Test Lyceum", "Sofia"))
                .thenReturn(Optional.of(lyceum));

        String result = lyceumService.requestRightsOverLyceum(request);

        assertThat(result).isEqualTo("We could not reach the lyceum via email. Please contact us.");
        verify(emailService, never()).sendLyceumVerificationEmail(any(), any(), any(), any());
    }

    @Test
    void requestRightsReturnsMessageWhenUserAlreadyAdminOfSameLyceum() {
        Lyceum lyceum = createLyceum(5L, "Lyceum", "Varna", "school@example.com");
        User user = createUser(1L);
        user.setAdministratedLyceum(lyceum);

        LyceumRightsRequest request = LyceumRightsRequest.builder()
                .lyceumName("Lyceum")
                .town("Varna")
                .build();

        when(lyceumRepository.findFirstByNameIgnoreCaseAndTownIgnoreCase("Lyceum", "Varna"))
                .thenReturn(Optional.of(lyceum));
        mockAuthenticatedUser(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        String result = lyceumService.requestRightsOverLyceum(request);

        assertThat(result).isEqualTo("You already administrate this lyceum.");
        verify(tokenRepository, never()).save(any(Token.class));
        verify(emailService, never()).sendLyceumVerificationEmail(any(), any(), any(), any());
    }

    @Test
    void requestRightsThrowsWhenUserAlreadyAdminOfDifferentLyceum() {
        Lyceum target = createLyceum(5L, "Lyceum", "Varna", "school@example.com");
        Lyceum other = createLyceum(11L, "Other", "Varna", "other@example.com");
        User user = createUser(1L);
        user.setAdministratedLyceum(other);

        LyceumRightsRequest request = LyceumRightsRequest.builder()
                .lyceumName("Lyceum")
                .town("Varna")
                .build();

        when(lyceumRepository.findFirstByNameIgnoreCaseAndTownIgnoreCase("Lyceum", "Varna"))
                .thenReturn(Optional.of(target));
        mockAuthenticatedUser(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThrows(ConflictException.class, () -> lyceumService.requestRightsOverLyceum(request));
        verify(emailService, never()).sendLyceumVerificationEmail(any(), any(), any(), any());
    }

    @Test
    void requestRightsThrowsWhenUserNotAuthenticated() {
        Lyceum lyceum = createLyceum(7L, "Lyceum", "Varna", "school@example.com");

        LyceumRightsRequest request = LyceumRightsRequest.builder()
                .lyceumName("Lyceum")
                .town("Varna")
                .build();

        when(lyceumRepository.findFirstByNameIgnoreCaseAndTownIgnoreCase("Lyceum", "Varna"))
                .thenReturn(Optional.of(lyceum));

        assertThrows(UnauthorizedException.class, () -> lyceumService.requestRightsOverLyceum(request));
        verify(tokenRepository, never()).save(any(Token.class));
        verify(emailService, never()).sendLyceumVerificationEmail(any(), any(), any(), any());
    }

    @Test
    void requestRightsNormalizesInputBeforeLookupAndNotification() {
        Lyceum lyceum = createLyceum(8L, "Lyceum", "Varna", "school@example.com");
        User user = createUser(10L);

        LyceumRightsRequest request = LyceumRightsRequest.builder()
                .lyceumName("   Lyceum   ")
                .town("  Varna  ")
                .build();

        when(lyceumRepository.findFirstByNameIgnoreCaseAndTownIgnoreCase("Lyceum", "Varna"))
                .thenReturn(Optional.of(lyceum));
        when(tokenRepository.findAllValidTokenByUser(user.getId())).thenReturn(List.of());
        mockAuthenticatedUser(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        lyceumService.requestRightsOverLyceum(request);

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> townCaptor = ArgumentCaptor.forClass(String.class);
        verify(lyceumRepository).findFirstByNameIgnoreCaseAndTownIgnoreCase(nameCaptor.capture(), townCaptor.capture());
        assertThat(nameCaptor.getValue()).isEqualTo("Lyceum");
        assertThat(townCaptor.getValue()).isEqualTo("Varna");

        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> normalizedNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> normalizedTownCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendLyceumVerificationEmail(
                toCaptor.capture(),
                normalizedNameCaptor.capture(),
                normalizedTownCaptor.capture(),
                tokenCaptor.capture());

        assertThat(toCaptor.getValue()).isEqualTo("school@example.com");
        assertThat(normalizedNameCaptor.getValue()).isEqualTo("Lyceum");
        assertThat(normalizedTownCaptor.getValue()).isEqualTo("Varna");
        assertThat(tokenCaptor.getValue()).isNotBlank();
    }

    @Test
    void requestRightsCreatesTokenInvalidatesPreviousAndSendsEmail() {
        Lyceum lyceum = createLyceum(5L, "Lyceum", "Varna", "school@example.com");
        User user = createUser(1L);
        User managedUser = createUser(1L);
        Token previousToken = Token.builder()
                .tokenType(TokenType.VERIFICATION)
                .expired(false)
                .revoked(false)
                .user(managedUser)
                .build();

        LyceumRightsRequest request = LyceumRightsRequest.builder()
                .lyceumName("Lyceum")
                .town("Varna")
                .build();

        when(lyceumRepository.findFirstByNameIgnoreCaseAndTownIgnoreCase("Lyceum", "Varna"))
                .thenReturn(Optional.of(lyceum));
        when(tokenRepository.findAllValidTokenByUser(managedUser.getId()))
                .thenReturn(List.of(previousToken));
        mockAuthenticatedUser(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(managedUser));

        ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);

        String result = lyceumService.requestRightsOverLyceum(request);

        assertThat(result).isEqualTo("We have sent you an email at school@example.com with a verification code.");
        assertThat(previousToken.isExpired()).isTrue();
        assertThat(previousToken.isRevoked()).isTrue();

        verify(tokenRepository).saveAll(List.of(previousToken));
        verify(tokenRepository).save(tokenCaptor.capture());
        Token savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser()).isEqualTo(managedUser);
        assertThat(savedToken.getLyceum()).isEqualTo(lyceum);
        assertThat(savedToken.getTokenType()).isEqualTo(TokenType.VERIFICATION);
        assertThat(savedToken.isExpired()).isFalse();
        assertThat(savedToken.isRevoked()).isFalse();

        verify(emailService).sendLyceumVerificationEmail(
                "school@example.com", "Lyceum", "Varna", savedToken.getTokenValue());
    }

    @Test
    void verifyRightsThrowsWhenCodeMissing() {
        LyceumRightsVerificationRequest request = LyceumRightsVerificationRequest.builder()
                .verificationCode(" ")
                .build();

        assertThrows(BadRequestException.class, () -> lyceumService.verifyRightsOverLyceum(request));
    }

    @Test
    void verifyRightsThrowsWhenTokenMissing() {
        User user = createUser(1L);
        mockAuthenticatedUser(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(tokenRepository.findByToken("code")).thenReturn(Optional.empty());

        LyceumRightsVerificationRequest request = LyceumRightsVerificationRequest.builder()
                .verificationCode("code")
                .build();

        assertThrows(BadRequestException.class, () -> lyceumService.verifyRightsOverLyceum(request));
    }

    @Test
    void verifyRightsTrimsVerificationCodeBeforeLookup() {
        User user = createUser(1L);
        User managedUser = createUser(1L);
        Lyceum lyceum = createLyceum(3L, "Lyceum", "Varna", "mail@example.com");
        Token token = Token.builder()
                .tokenValue("trimmed-code")
                .tokenType(TokenType.VERIFICATION)
                .expired(false)
                .revoked(false)
                .user(managedUser)
                .lyceum(lyceum)
                .build();

        mockAuthenticatedUser(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(managedUser));
        when(tokenRepository.findByToken("trimmed-code")).thenReturn(Optional.of(token));

        LyceumRightsVerificationRequest request = LyceumRightsVerificationRequest.builder()
                .verificationCode("  trimmed-code  ")
                .build();

        String message = lyceumService.verifyRightsOverLyceum(request);

        assertThat(message).isEqualTo("You are now the administrator of Lyceum in Varna.");
    }

    @Test
    void verifyRightsThrowsWhenTokenHasDifferentType() {
        User user = createUser(1L);
        User managedUser = createUser(1L);
        Lyceum lyceum = createLyceum(3L, "Lyceum", "Varna", "mail@example.com");
        Token token = Token.builder()
                .tokenValue("code")
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .user(managedUser)
                .lyceum(lyceum)
                .build();

        mockAuthenticatedUser(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(managedUser));
        when(tokenRepository.findByToken("code")).thenReturn(Optional.of(token));

        LyceumRightsVerificationRequest request = LyceumRightsVerificationRequest.builder()
                .verificationCode("code")
                .build();

        assertThrows(BadRequestException.class, () -> lyceumService.verifyRightsOverLyceum(request));
    }

    @Test
    void verifyRightsThrowsWhenTokenExpiredOrRevoked() {
        User user = createUser(1L);
        User managedUser = createUser(1L);
        Lyceum lyceum = createLyceum(3L, "Lyceum", "Varna", "mail@example.com");
        Token token = Token.builder()
                .tokenValue("code")
                .tokenType(TokenType.VERIFICATION)
                .expired(true)
                .revoked(true)
                .user(managedUser)
                .lyceum(lyceum)
                .build();

        mockAuthenticatedUser(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(managedUser));
        when(tokenRepository.findByToken("code")).thenReturn(Optional.of(token));

        LyceumRightsVerificationRequest request = LyceumRightsVerificationRequest.builder()
                .verificationCode("code")
                .build();

        assertThrows(BadRequestException.class, () -> lyceumService.verifyRightsOverLyceum(request));
    }

    @Test
    void verifyRightsThrowsWhenTokenBelongsToDifferentUser() {
        User user = createUser(1L);
        User otherUser = createUser(2L);
        Lyceum lyceum = createLyceum(3L, "Lyceum", "Varna", "mail@example.com");
        Token token = Token.builder()
                .tokenValue("code")
                .tokenType(TokenType.VERIFICATION)
                .expired(false)
                .revoked(false)
                .user(otherUser)
                .lyceum(lyceum)
                .build();

        mockAuthenticatedUser(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(tokenRepository.findByToken("code")).thenReturn(Optional.of(token));

        LyceumRightsVerificationRequest request = LyceumRightsVerificationRequest.builder()
                .verificationCode("code")
                .build();

        assertThrows(UnauthorizedException.class, () -> lyceumService.verifyRightsOverLyceum(request));
    }

    @Test
    void verifyRightsThrowsWhenUserAdministratesDifferentLyceum() {
        User user = createUser(1L);
        Lyceum current = createLyceum(100L, "Current", "Varna", "curr@example.com");
        user.setAdministratedLyceum(current);

        Lyceum target = createLyceum(3L, "Lyceum", "Varna", "mail@example.com");
        Token token = Token.builder()
                .tokenValue("code")
                .tokenType(TokenType.VERIFICATION)
                .expired(false)
                .revoked(false)
                .user(user)
                .lyceum(target)
                .build();

        mockAuthenticatedUser(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(tokenRepository.findByToken("code")).thenReturn(Optional.of(token));

        LyceumRightsVerificationRequest request = LyceumRightsVerificationRequest.builder()
                .verificationCode("code")
                .build();

        assertThrows(ConflictException.class, () -> lyceumService.verifyRightsOverLyceum(request));
    }

    @Test
    void verifyRightsAssignsLyceumAndExpiresToken() {
        User user = createUser(1L);
        User managedUser = createUser(1L);
        Lyceum lyceum = createLyceum(3L, "Lyceum", "Varna", "mail@example.com");
        Token token = Token.builder()
                .tokenValue("code")
                .tokenType(TokenType.VERIFICATION)
                .expired(false)
                .revoked(false)
                .user(managedUser)
                .lyceum(lyceum)
                .build();

        mockAuthenticatedUser(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(managedUser));
        when(tokenRepository.findByToken("code")).thenReturn(Optional.of(token));

        LyceumRightsVerificationRequest request = LyceumRightsVerificationRequest.builder()
                .verificationCode("code")
                .build();

        String message = lyceumService.verifyRightsOverLyceum(request);

        assertThat(message).isEqualTo("You are now the administrator of Lyceum in Varna.");
        assertThat(managedUser.getAdministratedLyceum()).isEqualTo(lyceum);
        assertThat(lyceum.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(token.isExpired()).isTrue();
        assertThat(token.isRevoked()).isTrue();

        verify(lyceumRepository).save(lyceum);
        verify(userRepository).save(managedUser);

        ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        Token savedToken = tokenCaptor.getValue();
        assertEquals(token, savedToken);
    }

    private void mockAuthenticatedUser(User user) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User createUser(Long id) {
        return User.builder()
                .id(id)
                .firstname("John")
                .lastname("Doe")
                .email("john" + id + "@example.com")
                .username("john" + id)
                .password("password123")
                .role(Role.USER)
                .enabled(true)
                .build();
    }

    private Lyceum createLyceum(Long id, String name, String town, String email) {
        Lyceum lyceum = new Lyceum();
        lyceum.setId(id);
        lyceum.setName(name);
        lyceum.setTown(town);
        lyceum.setEmail(email);
        lyceum.setVerificationStatus(VerificationStatus.NOT_VERIFIED);
        return lyceum;
    }
}
