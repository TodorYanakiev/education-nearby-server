package com.dev.education_nearby_server.services.oauth2;

import com.dev.education_nearby_server.config.JwtService;
import com.dev.education_nearby_server.enums.AuthProvider;
import com.dev.education_nearby_server.exceptions.common.BadRequestException;
import com.dev.education_nearby_server.exceptions.common.ConflictException;
import com.dev.education_nearby_server.exceptions.common.UnauthorizedException;
import com.dev.education_nearby_server.models.dto.auth.AuthenticationResponse;
import com.dev.education_nearby_server.models.dto.auth.OAuth2CompleteRegistrationRequest;
import com.dev.education_nearby_server.models.dto.auth.OAuth2LoginResponse;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.repositories.UserRepository;
import com.dev.education_nearby_server.services.AuthenticationService;
import com.dev.education_nearby_server.services.LyceumService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private JwtService jwtService;
    @Mock
    private LyceumService lyceumService;

    @InjectMocks
    private OAuth2AuthenticationService service;

    @Test
    void handleOAuth2LoginThrowsWhenOauthUserMissing() {
        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> service.handleOAuth2Login(AuthProvider.GOOGLE, null));

        assertThat(ex.getMessage()).isEqualTo("Missing OAuth2 user information.");
    }

    @Test
    void handleOAuth2LoginThrowsWhenProviderIdMissing() {
        OAuth2User oauth2User = mock(OAuth2User.class);
        when(oauth2User.getName()).thenReturn(" ");

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> service.handleOAuth2Login(AuthProvider.GOOGLE, oauth2User));

        assertThat(ex.getMessage()).isEqualTo("Missing OAuth2 provider user id.");
    }

    @Test
    void handleOAuth2LoginReturnsCompleteForExistingProviderUser() {
        OAuth2User oauth2User = oauth2User("provider-id", Map.of(
                "email", "existing@example.com",
                "given_name", "John",
                "family_name", "Doe"
        ));
        User existing = User.builder()
                .id(10L)
                .enabled(true)
                .registrationComplete(true)
                .authProvider(AuthProvider.GOOGLE)
                .authProviderId("provider-id")
                .build();
        AuthenticationResponse tokens = AuthenticationResponse.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .build();
        when(userRepository.findByAuthProviderAndAuthProviderId(AuthProvider.GOOGLE, "provider-id"))
                .thenReturn(Optional.of(existing));
        when(authenticationService.issueTokens(existing)).thenReturn(tokens);

        OAuth2LoginResponse result = service.handleOAuth2Login(AuthProvider.GOOGLE, oauth2User);

        assertThat(result.getStatus()).isEqualTo("COMPLETE");
        assertThat(result.getAccessToken()).isEqualTo("access");
        assertThat(result.getRefreshToken()).isEqualTo("refresh");
        verify(jwtService, never()).generateOauth2RegistrationToken(any(), any());
    }

    @Test
    void handleOAuth2LoginReturnsPendingForExistingIncompleteProviderUser() {
        OAuth2User oauth2User = oauth2User("provider-id", Map.of(
                "email", "partial@example.com"
        ));
        User existing = User.builder()
                .id(11L)
                .enabled(true)
                .registrationComplete(false)
                .authProvider(AuthProvider.GOOGLE)
                .authProviderId("provider-id")
                .build();
        when(userRepository.findByAuthProviderAndAuthProviderId(AuthProvider.GOOGLE, "provider-id"))
                .thenReturn(Optional.of(existing));
        when(jwtService.generateOauth2RegistrationToken(any(), eq("oauth2:GOOGLE:provider-id")))
                .thenReturn("registration-token");

        OAuth2LoginResponse result = service.handleOAuth2Login(AuthProvider.GOOGLE, oauth2User);

        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getRegistrationToken()).isEqualTo("registration-token");
        assertThat(result.getMissingFields()).containsExactly("username", "firstname", "lastname");
    }

    @Test
    void handleOAuth2LoginThrowsWhenExistingProviderUserIsDisabled() {
        OAuth2User oauth2User = oauth2User("provider-id", Map.of());
        User existing = User.builder()
                .enabled(false)
                .registrationComplete(true)
                .authProvider(AuthProvider.GOOGLE)
                .authProviderId("provider-id")
                .build();
        when(userRepository.findByAuthProviderAndAuthProviderId(AuthProvider.GOOGLE, "provider-id"))
                .thenReturn(Optional.of(existing));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> service.handleOAuth2Login(AuthProvider.GOOGLE, oauth2User));

        assertThat(ex.getMessage()).isEqualTo("The user is disabled");
    }

    @Test
    void handleOAuth2LoginLinksExistingEmailAccount() {
        OAuth2User oauth2User = oauth2User("provider-id", Map.of(
                "email", "email@example.com",
                "email_verified", true
        ));
        User existingByEmail = User.builder()
                .id(15L)
                .enabled(true)
                .registrationComplete(false)
                .authProvider(AuthProvider.LOCAL)
                .emailVerified(false)
                .build();
        AuthenticationResponse tokens = AuthenticationResponse.builder()
                .accessToken("new-access")
                .refreshToken("new-refresh")
                .build();
        when(userRepository.findByAuthProviderAndAuthProviderId(AuthProvider.GOOGLE, "provider-id"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("email@example.com")).thenReturn(Optional.of(existingByEmail));
        when(authenticationService.issueTokens(existingByEmail)).thenReturn(tokens);

        OAuth2LoginResponse result = service.handleOAuth2Login(AuthProvider.GOOGLE, oauth2User);

        assertThat(result.getStatus()).isEqualTo("COMPLETE");
        assertThat(existingByEmail.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(existingByEmail.getAuthProviderId()).isEqualTo("provider-id");
        assertThat(existingByEmail.isEmailVerified()).isTrue();
        assertThat(existingByEmail.isRegistrationComplete()).isTrue();
        verify(userRepository).save(existingByEmail);
    }

    @Test
    void handleOAuth2LoginThrowsWhenEmailAccountLinkedToDifferentIdentity() {
        OAuth2User oauth2User = oauth2User("provider-id", Map.of(
                "email", "email@example.com"
        ));
        User existingByEmail = User.builder()
                .id(16L)
                .enabled(true)
                .registrationComplete(true)
                .authProvider(AuthProvider.GOOGLE)
                .authProviderId("another-id")
                .build();
        when(userRepository.findByAuthProviderAndAuthProviderId(AuthProvider.GOOGLE, "provider-id"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("email@example.com")).thenReturn(Optional.of(existingByEmail));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.handleOAuth2Login(AuthProvider.GOOGLE, oauth2User));

        assertThat(ex.getMessage()).isEqualTo("Account is already linked to another google identity.");
    }

    @Test
    void handleOAuth2LoginReturnsPendingForNewUserAndBuildsClaims() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("name", "Alice Cooper");
        attributes.put("email_verified", "true");
        OAuth2User oauth2User = oauth2User("provider-id", attributes);
        when(userRepository.findByAuthProviderAndAuthProviderId(AuthProvider.GOOGLE, "provider-id"))
                .thenReturn(Optional.empty());
        when(jwtService.generateOauth2RegistrationToken(any(), eq("oauth2:GOOGLE:provider-id")))
                .thenReturn("pending-token");

        OAuth2LoginResponse result = service.handleOAuth2Login(AuthProvider.GOOGLE, oauth2User);

        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getRegistrationToken()).isEqualTo("pending-token");
        assertThat(result.getMissingFields()).containsExactly("username", "email");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jwtService).generateOauth2RegistrationToken(claimsCaptor.capture(), eq("oauth2:GOOGLE:provider-id"));
        assertThat(claimsCaptor.getValue()).containsEntry("type", "OAUTH2_REGISTRATION");
        assertThat(claimsCaptor.getValue()).containsEntry("provider", "GOOGLE");
        assertThat(claimsCaptor.getValue()).containsEntry("firstname", "Alice");
        assertThat(claimsCaptor.getValue()).containsEntry("lastname", "Cooper");
        assertThat(claimsCaptor.getValue()).containsEntry("email_verified", true);
    }

    @Test
    void completeRegistrationThrowsWhenRegistrationTokenMissing() {
        OAuth2CompleteRegistrationRequest request = OAuth2CompleteRegistrationRequest.builder()
                .registrationToken(" ")
                .username("user123")
                .build();

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> service.completeRegistration(request));

        assertThat(ex.getMessage()).isEqualTo("Missing registration token.");
    }

    @Test
    void completeRegistrationThrowsWhenTokenCannotBeParsed() {
        OAuth2CompleteRegistrationRequest request = OAuth2CompleteRegistrationRequest.builder()
                .registrationToken("token")
                .username("user123")
                .build();
        when(jwtService.extractAllClaims("token")).thenThrow(new RuntimeException("broken token"));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> service.completeRegistration(request));

        assertThat(ex.getMessage()).isEqualTo("Invalid registration token.");
    }

    @Test
    void completeRegistrationThrowsWhenTokenHasWrongType() {
        OAuth2CompleteRegistrationRequest request = OAuth2CompleteRegistrationRequest.builder()
                .registrationToken("token")
                .username("user123")
                .build();
        Claims claims = claims("WRONG_TYPE", "GOOGLE", "provider-id", "email@example.com", "A", "B", true);
        when(jwtService.extractAllClaims("token")).thenReturn(claims);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> service.completeRegistration(request));

        assertThat(ex.getMessage()).isEqualTo("Invalid registration token.");
    }

    @Test
    void completeRegistrationThrowsWhenTokenExpired() {
        OAuth2CompleteRegistrationRequest request = OAuth2CompleteRegistrationRequest.builder()
                .registrationToken("token")
                .username("user123")
                .build();
        Claims claims = claims("OAUTH2_REGISTRATION", "GOOGLE", "provider-id", "email@example.com", "A", "B", true);
        when(jwtService.extractAllClaims("token")).thenReturn(claims);
        when(jwtService.isTokenExpired("token")).thenReturn(true);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> service.completeRegistration(request));

        assertThat(ex.getMessage()).isEqualTo("Registration token expired.");
    }

    @Test
    void completeRegistrationThrowsWhenUsernameIsBlank() {
        OAuth2CompleteRegistrationRequest request = OAuth2CompleteRegistrationRequest.builder()
                .registrationToken("token")
                .username("  ")
                .email("mail@example.com")
                .firstname("A")
                .lastname("B")
                .build();
        Claims claims = claims("OAUTH2_REGISTRATION", "GOOGLE", "provider-id", "email@example.com", "A", "B", true);
        when(jwtService.extractAllClaims("token")).thenReturn(claims);
        when(jwtService.isTokenExpired("token")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.completeRegistration(request));

        assertThat(ex.getMessage()).isEqualTo("Username must not be blank.");
    }

    @Test
    void completeRegistrationThrowsWhenUsernameLengthInvalid() {
        OAuth2CompleteRegistrationRequest request = OAuth2CompleteRegistrationRequest.builder()
                .registrationToken("token")
                .username("ab")
                .email("mail@example.com")
                .firstname("A")
                .lastname("B")
                .build();
        Claims claims = claims("OAUTH2_REGISTRATION", "GOOGLE", "provider-id", "email@example.com", "A", "B", true);
        when(jwtService.extractAllClaims("token")).thenReturn(claims);
        when(jwtService.isTokenExpired("token")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.completeRegistration(request));

        assertThat(ex.getMessage()).isEqualTo("Username must be between 3 and 50 characters.");
    }

    @Test
    void completeRegistrationThrowsWhenRequiredFieldsMissingFromRequestAndToken() {
        OAuth2CompleteRegistrationRequest request = OAuth2CompleteRegistrationRequest.builder()
                .registrationToken("token")
                .username("validusername")
                .build();
        Claims claims = claims("OAUTH2_REGISTRATION", "GOOGLE", "provider-id", null, null, null, false);
        when(jwtService.extractAllClaims("token")).thenReturn(claims);
        when(jwtService.isTokenExpired("token")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.completeRegistration(request));

        assertThat(ex.getMessage()).isEqualTo("Email is required to complete registration.");
    }

    @Test
    void completeRegistrationThrowsWhenUsernameAlreadyExists() {
        OAuth2CompleteRegistrationRequest request = OAuth2CompleteRegistrationRequest.builder()
                .registrationToken("token")
                .username("existing-user")
                .email("mail@example.com")
                .firstname("A")
                .lastname("B")
                .build();
        Claims claims = claims("OAUTH2_REGISTRATION", "GOOGLE", "provider-id", "email@example.com", "A", "B", true);
        when(jwtService.extractAllClaims("token")).thenReturn(claims);
        when(jwtService.isTokenExpired("token")).thenReturn(false);
        when(userRepository.findByUsername("existing-user")).thenReturn(Optional.of(User.builder().id(1L).build()));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.completeRegistration(request));

        assertThat(ex.getMessage()).isEqualTo("User with such username already exists!");
    }

    @Test
    void completeRegistrationThrowsWhenEmailAlreadyExists() {
        OAuth2CompleteRegistrationRequest request = OAuth2CompleteRegistrationRequest.builder()
                .registrationToken("token")
                .username("new-user")
                .email("existing@example.com")
                .firstname("A")
                .lastname("B")
                .build();
        Claims claims = claims("OAUTH2_REGISTRATION", "GOOGLE", "provider-id", "email@example.com", "A", "B", true);
        when(jwtService.extractAllClaims("token")).thenReturn(claims);
        when(jwtService.isTokenExpired("token")).thenReturn(false);
        when(userRepository.findByUsername("new-user")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("existing@example.com"))
                .thenReturn(Optional.of(User.builder().id(2L).build()));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.completeRegistration(request));

        assertThat(ex.getMessage()).isEqualTo("User with such email already exists!");
    }

    @Test
    void completeRegistrationReturnsTokensForExistingLinkedProviderUser() {
        OAuth2CompleteRegistrationRequest request = OAuth2CompleteRegistrationRequest.builder()
                .registrationToken("token")
                .username("new-user")
                .email("email@example.com")
                .firstname("A")
                .lastname("B")
                .build();
        Claims claims = claims("OAUTH2_REGISTRATION", "GOOGLE", "provider-id", "email@example.com", "A", "B", true);
        User existingByProvider = User.builder()
                .id(3L)
                .enabled(true)
                .authProvider(AuthProvider.GOOGLE)
                .authProviderId("provider-id")
                .build();
        AuthenticationResponse tokens = AuthenticationResponse.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .build();

        when(jwtService.extractAllClaims("token")).thenReturn(claims);
        when(jwtService.isTokenExpired("token")).thenReturn(false);
        when(userRepository.findByUsername("new-user")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("email@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByAuthProviderAndAuthProviderId(AuthProvider.GOOGLE, "provider-id"))
                .thenReturn(Optional.of(existingByProvider));
        when(authenticationService.issueTokens(existingByProvider)).thenReturn(tokens);

        AuthenticationResponse result = service.completeRegistration(request);

        assertThat(result.getAccessToken()).isEqualTo("access");
        assertThat(result.getRefreshToken()).isEqualTo("refresh");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void completeRegistrationThrowsWhenExistingLinkedProviderUserDisabled() {
        OAuth2CompleteRegistrationRequest request = OAuth2CompleteRegistrationRequest.builder()
                .registrationToken("token")
                .username("new-user")
                .email("email@example.com")
                .firstname("A")
                .lastname("B")
                .build();
        Claims claims = claims("OAUTH2_REGISTRATION", "GOOGLE", "provider-id", "email@example.com", "A", "B", true);
        User existingByProvider = User.builder()
                .enabled(false)
                .authProvider(AuthProvider.GOOGLE)
                .authProviderId("provider-id")
                .build();

        when(jwtService.extractAllClaims("token")).thenReturn(claims);
        when(jwtService.isTokenExpired("token")).thenReturn(false);
        when(userRepository.findByUsername("new-user")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("email@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByAuthProviderAndAuthProviderId(AuthProvider.GOOGLE, "provider-id"))
                .thenReturn(Optional.of(existingByProvider));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> service.completeRegistration(request));

        assertThat(ex.getMessage()).isEqualTo("The user is disabled");
    }

    @Test
    void completeRegistrationCreatesNewUserAndReturnsTokens() {
        OAuth2CompleteRegistrationRequest request = OAuth2CompleteRegistrationRequest.builder()
                .registrationToken("token")
                .username(" new-user ")
                .email(" new-email@example.com ")
                .firstname(" New ")
                .lastname(" User ")
                .description(" About me ")
                .build();
        Claims claims = claims("OAUTH2_REGISTRATION", "GOOGLE", "provider-id", "from-token@example.com",
                "TokenFirst", "TokenLast", true);
        AuthenticationResponse tokens = AuthenticationResponse.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .build();

        when(jwtService.extractAllClaims("token")).thenReturn(claims);
        when(jwtService.isTokenExpired("token")).thenReturn(false);
        when(userRepository.findByUsername("new-user")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("new-email@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByAuthProviderAndAuthProviderId(AuthProvider.GOOGLE, "provider-id"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(99L);
            return user;
        });
        when(authenticationService.issueTokens(any(User.class))).thenReturn(tokens);

        AuthenticationResponse result = service.completeRegistration(request);

        assertThat(result.getAccessToken()).isEqualTo("access");
        assertThat(result.getRefreshToken()).isEqualTo("refresh");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getUsername()).isEqualTo("new-user");
        assertThat(saved.getEmail()).isEqualTo("new-email@example.com");
        assertThat(saved.getFirstname()).isEqualTo("New");
        assertThat(saved.getLastname()).isEqualTo("User");
        assertThat(saved.getDescription()).isEqualTo("About me");
        assertThat(saved.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(saved.getAuthProviderId()).isEqualTo("provider-id");
        assertThat(saved.isEmailVerified()).isTrue();
        assertThat(saved.isRegistrationComplete()).isTrue();
        assertThat(saved.isEnabled()).isTrue();

        verify(lyceumService).acceptLecturerInvitationsFor(saved);
    }

    @Test
    void completeRegistrationUsesTokenFallbackValues() {
        OAuth2CompleteRegistrationRequest request = OAuth2CompleteRegistrationRequest.builder()
                .registrationToken("token")
                .username("new-user")
                .build();
        Claims claims = claims("OAUTH2_REGISTRATION", "GOOGLE", "provider-id",
                "from-token@example.com", "TokenFirst", "TokenLast", false);
        AuthenticationResponse tokens = AuthenticationResponse.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .build();

        when(jwtService.extractAllClaims("token")).thenReturn(claims);
        when(jwtService.isTokenExpired("token")).thenReturn(false);
        when(userRepository.findByUsername("new-user")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("from-token@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByAuthProviderAndAuthProviderId(AuthProvider.GOOGLE, "provider-id"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authenticationService.issueTokens(any(User.class))).thenReturn(tokens);

        AuthenticationResponse result = service.completeRegistration(request);

        assertThat(result.getAccessToken()).isEqualTo("access");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("from-token@example.com");
        assertThat(saved.getFirstname()).isEqualTo("TokenFirst");
        assertThat(saved.getLastname()).isEqualTo("TokenLast");
        assertThat(saved.isEmailVerified()).isFalse();
    }

    @Test
    void completeRegistrationThrowsWhenTokenProviderInvalid() {
        OAuth2CompleteRegistrationRequest request = OAuth2CompleteRegistrationRequest.builder()
                .registrationToken("token")
                .username("new-user")
                .build();
        Claims claims = claims("OAUTH2_REGISTRATION", "INVALID_PROVIDER", "provider-id",
                "mail@example.com", "A", "B", true);
        when(jwtService.extractAllClaims("token")).thenReturn(claims);
        when(jwtService.isTokenExpired("token")).thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> service.completeRegistration(request));

        assertThat(ex.getMessage()).isEqualTo("Invalid registration token provider.");
        verifyNoInteractions(authenticationService);
    }

    @Test
    void completeRegistrationThrowsWhenTokenProviderIdMissing() {
        OAuth2CompleteRegistrationRequest request = OAuth2CompleteRegistrationRequest.builder()
                .registrationToken("token")
                .username("new-user")
                .build();
        Claims claims = claims("OAUTH2_REGISTRATION", "GOOGLE", "   ",
                "mail@example.com", "A", "B", true);
        when(jwtService.extractAllClaims("token")).thenReturn(claims);
        when(jwtService.isTokenExpired("token")).thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> service.completeRegistration(request));

        assertThat(ex.getMessage()).isEqualTo("Invalid registration token provider id.");
    }

    private OAuth2User oauth2User(String name, Map<String, Object> attributes) {
        OAuth2User oauth2User = mock(OAuth2User.class);
        when(oauth2User.getName()).thenReturn(name);
        when(oauth2User.getAttributes()).thenReturn(attributes);
        return oauth2User;
    }

    private Claims claims(
            String type,
            String provider,
            String providerId,
            String email,
            String firstname,
            String lastname,
            Boolean emailVerified
    ) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", type);
        claims.put("provider", provider);
        claims.put("provider_id", providerId);
        claims.put("email", email);
        claims.put("firstname", firstname);
        claims.put("lastname", lastname);
        claims.put("email_verified", emailVerified);
        return Jwts.claims(claims);
    }
}
