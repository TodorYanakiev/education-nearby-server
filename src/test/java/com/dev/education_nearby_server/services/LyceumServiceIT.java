package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.enums.Role;
import com.dev.education_nearby_server.enums.TokenType;
import com.dev.education_nearby_server.enums.VerificationStatus;
import com.dev.education_nearby_server.exceptions.common.AccessDeniedException;
import com.dev.education_nearby_server.exceptions.common.BadRequestException;
import com.dev.education_nearby_server.exceptions.common.ConflictException;
import com.dev.education_nearby_server.exceptions.common.UnauthorizedException;
import com.dev.education_nearby_server.models.dto.request.LyceumRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumRightsRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumRightsVerificationRequest;
import com.dev.education_nearby_server.models.dto.response.LyceumResponse;
import com.dev.education_nearby_server.models.entity.Lyceum;
import com.dev.education_nearby_server.models.entity.Token;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.repositories.LyceumRepository;
import com.dev.education_nearby_server.repositories.TokenRepository;
import com.dev.education_nearby_server.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LyceumServiceIT {

    @Autowired
    private LyceumService lyceumService;
    @Autowired
    private LyceumRepository lyceumRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TokenRepository tokenRepository;
    @MockitoBean
    private EmailService emailService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createLyceumPersistsEntityAndReturnsResponse() {
        LyceumRequest request = LyceumRequest.builder()
                .name("New Lyceum")
                .town("Varna")
                .email("contact@example.org")
                .build();

        LyceumResponse response = lyceumService.createLyceum(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("New Lyceum");
        assertThat(response.getTown()).isEqualTo("Varna");
        assertThat(response.getVerificationStatus()).isEqualTo(VerificationStatus.NOT_VERIFIED);

        Lyceum persisted = lyceumRepository.findById(response.getId()).orElseThrow();
        assertThat(persisted.getName()).isEqualTo("New Lyceum");
        assertThat(persisted.getTown()).isEqualTo("Varna");
        assertThat(persisted.getEmail()).isEqualTo("contact@example.org");
        assertThat(persisted.getVerificationStatus()).isEqualTo(VerificationStatus.NOT_VERIFIED);
    }

    @Test
    void createLyceumRejectsDuplicates() {
        persistLyceum("Duplicate", "Varna", "mail@example.org");
        LyceumRequest request = LyceumRequest.builder()
                .name("Duplicate")
                .town("Varna")
                .build();

        assertThatThrownBy(() -> lyceumService.createLyceum(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void getAllLyceumsReturnsMappedResponses() {
        persistLyceum("First", "Varna", "first@example.org");
        persistLyceum("Second", "Sofia", "second@example.org");

        List<LyceumResponse> responses = lyceumService.getAllLyceums();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(LyceumResponse::getName)
                .containsExactlyInAnyOrder("First", "Second");
    }

    @Test
    void getLyceumByIdReturnsResponseForVerifiedLyceum() {
        Lyceum lyceum = persistLyceum("Verified", "Varna", "verified@example.org");
        lyceum.setVerificationStatus(VerificationStatus.VERIFIED);
        lyceumRepository.save(lyceum);

        LyceumResponse response = lyceumService.getLyceumById(lyceum.getId());

        assertThat(response.getId()).isEqualTo(lyceum.getId());
        assertThat(response.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
    }

    @Test
    void getLyceumByIdRequiresAuthenticationForNotVerifiedLyceum() {
        Lyceum lyceum = persistLyceum("Restricted", "Varna", "restricted@example.org");
        lyceum.setVerificationStatus(VerificationStatus.NOT_VERIFIED);
        lyceumRepository.save(lyceum);
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> lyceumService.getLyceumById(lyceum.getId()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getLyceumByIdRequiresAdminRoleWhenNotVerified() {
        Lyceum lyceum = persistLyceum("Restricted", "Varna", "restricted@example.org");
        lyceum.setVerificationStatus(VerificationStatus.NOT_VERIFIED);
        lyceumRepository.save(lyceum);
        User user = persistUser("user@example.org", "user");
        authenticate(user);

        assertThatThrownBy(() -> lyceumService.getLyceumById(lyceum.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getLyceumByIdReturnsResponseWhenAdminRequestsNotVerifiedLyceum() {
        Lyceum lyceum = persistLyceum("Restricted", "Varna", "restricted@example.org");
        lyceum.setVerificationStatus(VerificationStatus.NOT_VERIFIED);
        lyceumRepository.save(lyceum);
        User admin = persistUser("admin@example.org", "admin", Role.ADMIN);
        authenticate(admin);

        LyceumResponse response = lyceumService.getLyceumById(lyceum.getId());

        assertThat(response.getId()).isEqualTo(lyceum.getId());
        assertThat(response.getVerificationStatus()).isEqualTo(VerificationStatus.NOT_VERIFIED);
    }

    @Test
    void requestRightsOverLyceumPersistsVerificationTokenAndNotifiesEmail() {
        Lyceum lyceum = persistLyceum("Lyceum", "Varna", "school@example.com");
        User user = persistUser("john@example.com", "johnny");

        Token existing = Token.builder()
                .tokenValue("old-token")
                .tokenType(TokenType.VERIFICATION)
                .revoked(false)
                .expired(false)
                .user(user)
                .lyceum(lyceum)
                .build();
        tokenRepository.save(existing);

        authenticate(user);

        LyceumRightsRequest request = LyceumRightsRequest.builder()
                .lyceumName("Lyceum")
                .town("Varna")
                .build();

        String response = lyceumService.requestRightsOverLyceum(request);

        assertThat(response).isEqualTo("We have sent you an email at school@example.com with a verification code.");

        Token refreshedExisting = tokenRepository.findByToken("old-token").orElseThrow();
        assertThat(refreshedExisting.isExpired()).isTrue();
        assertThat(refreshedExisting.isRevoked()).isTrue();

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendLyceumVerificationEmail(
                eq("school@example.com"),
                eq("Lyceum"),
                eq("Varna"),
                tokenCaptor.capture());
        String generatedCode = tokenCaptor.getValue();

        Optional<Token> savedTokenOpt = tokenRepository.findByToken(generatedCode);
        assertThat(savedTokenOpt).isPresent();
        Token savedToken = savedTokenOpt.orElseThrow();
        assertThat(savedToken.getUser().getId()).isEqualTo(user.getId());
        assertThat(savedToken.getLyceum().getId()).isEqualTo(lyceum.getId());
        assertThat(savedToken.getTokenType()).isEqualTo(TokenType.VERIFICATION);
        assertThat(savedToken.isExpired()).isFalse();
        assertThat(savedToken.isRevoked()).isFalse();
    }

    @Test
    void requestRightsOverLyceumFailsWithoutAuthentication() {
        persistLyceum("Lyceum", "Varna", "school@example.com");

        LyceumRightsRequest request = LyceumRightsRequest.builder()
                .lyceumName("Lyceum")
                .town("Varna")
                .build();

        assertThatThrownBy(() -> lyceumService.requestRightsOverLyceum(request))
                .isInstanceOf(UnauthorizedException.class);

        verify(emailService, never()).sendLyceumVerificationEmail(any(), any(), any(), any());
    }

    @Test
    void verifyRightsOverLyceumAssignsAdministrationAndExpiresToken() {
        Lyceum lyceum = persistLyceum("Lyceum", "Varna", "school@example.com");
        User user = persistUser("john@example.com", "johnny");

        Token verificationToken = Token.builder()
                .tokenValue("verify-token")
                .tokenType(TokenType.VERIFICATION)
                .revoked(false)
                .expired(false)
                .user(user)
                .lyceum(lyceum)
                .build();
        tokenRepository.save(verificationToken);

        authenticate(user);

        LyceumRightsVerificationRequest request = LyceumRightsVerificationRequest.builder()
                .verificationCode("verify-token")
                .build();

        String message = lyceumService.verifyRightsOverLyceum(request);

        assertThat(message).isEqualTo("You are now the administrator of Lyceum in Varna.");

        User managedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(managedUser.getAdministratedLyceum()).isNotNull();
        assertEquals(lyceum.getId(), managedUser.getAdministratedLyceum().getId());

        Lyceum persistedLyceum = lyceumRepository.findById(lyceum.getId()).orElseThrow();
        assertThat(persistedLyceum.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);

        Token updatedToken = tokenRepository.findByToken("verify-token").orElseThrow();
        assertThat(updatedToken.isExpired()).isTrue();
        assertThat(updatedToken.isRevoked()).isTrue();
    }

    @Test
    void verifyRightsOverLyceumThrowsWhenTokenMissing() {
        persistLyceum("Lyceum", "Varna", "school@example.com");
        User user = persistUser("john@example.com", "johnny");

        authenticate(user);

        LyceumRightsVerificationRequest request = LyceumRightsVerificationRequest.builder()
                .verificationCode("missing-token")
                .build();

        assertThatThrownBy(() -> lyceumService.verifyRightsOverLyceum(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void verifyRightsOverLyceumThrowsWhenTokenBelongsToAnotherUser() {
        Lyceum lyceum = persistLyceum("Lyceum", "Varna", "school@example.com");
        User owner = persistUser("owner@example.com", "owner");
        User other = persistUser("other@example.com", "otherUser");

        Token verificationToken = Token.builder()
                .tokenValue("verify-token")
                .tokenType(TokenType.VERIFICATION)
                .revoked(false)
                .expired(false)
                .user(other)
                .lyceum(lyceum)
                .build();
        tokenRepository.save(verificationToken);

        authenticate(owner);

        LyceumRightsVerificationRequest request = LyceumRightsVerificationRequest.builder()
                .verificationCode("verify-token")
                .build();

        assertThatThrownBy(() -> lyceumService.verifyRightsOverLyceum(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    private void authenticate(User user) {
        User principal = userRepository.findById(user.getId()).orElseThrow();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Lyceum persistLyceum(String name, String town, String email) {
        Lyceum lyceum = new Lyceum();
        lyceum.setName(name);
        lyceum.setTown(town);
        lyceum.setEmail(email);
        lyceum.setVerificationStatus(VerificationStatus.NOT_VERIFIED);
        return lyceumRepository.save(lyceum);
    }

    private User persistUser(String email, String username) {
        return persistUser(email, username, Role.USER);
    }

    private User persistUser(String email, String username, Role role) {
        User user = User.builder()
                .firstname("John")
                .lastname("Doe")
                .email(email)
                .username(username)
                .password("password123")
                .role(role)
                .enabled(true)
                .build();
        return userRepository.save(user);
    }
}
