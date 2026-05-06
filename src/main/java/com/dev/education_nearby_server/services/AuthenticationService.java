package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.config.JwtService;
import com.dev.education_nearby_server.config.PasswordResetProperties;
import com.dev.education_nearby_server.enums.AuthProvider;
import com.dev.education_nearby_server.enums.Role;
import com.dev.education_nearby_server.enums.TokenType;
import com.dev.education_nearby_server.exceptions.common.AccessDeniedException;
import com.dev.education_nearby_server.exceptions.common.BadRequestException;
import com.dev.education_nearby_server.exceptions.common.ConflictException;
import com.dev.education_nearby_server.exceptions.common.UnauthorizedException;
import com.dev.education_nearby_server.models.dto.auth.AuthenticationRequest;
import com.dev.education_nearby_server.models.dto.auth.AuthenticationResponse;
import com.dev.education_nearby_server.models.dto.auth.ForgotPasswordRequest;
import com.dev.education_nearby_server.models.dto.auth.PasswordResetCodeVerificationRequest;
import com.dev.education_nearby_server.models.dto.auth.RegisterRequest;
import com.dev.education_nearby_server.models.dto.auth.ResetForgottenPasswordRequest;
import com.dev.education_nearby_server.models.entity.Token;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.repositories.TokenRepository;
import com.dev.education_nearby_server.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.function.Predicate;

/**
 * Handles authentication workflows: registering users, authenticating credentials,
 * issuing JWT access/refresh tokens, refreshing access tokens, and password reset flows.
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    @SuppressWarnings("java:S2068") // False positive: constants are password-reset messages, not credentials.
    private static final String PASSWORD_RESET_REQUEST_MESSAGE =
            "If an account with that email exists, we have sent a verification code.";
    @SuppressWarnings("java:S2068") // False positive: constants are password-reset messages, not credentials.
    private static final String PASSWORD_RESET_CODE_VALID_MESSAGE = "Verification code confirmed.";
    @SuppressWarnings("java:S2068") // False positive: constants are password-reset messages, not credentials.
    private static final String PASSWORD_RESET_SUCCESS_MESSAGE = "Password has been reset successfully.";
    @SuppressWarnings("java:S2068") // False positive: constants are password-reset messages, not credentials.
    private static final String INVALID_PASSWORD_RESET_CODE_MESSAGE = "Invalid verification code.";
    @SuppressWarnings("java:S2068") // False positive: constants are password-reset messages, not credentials.
    private static final String EXPIRED_PASSWORD_RESET_CODE_MESSAGE = "Verification code has expired.";

    private final UserRepository repository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationProvider authenticationProvider;
    private final ObjectMapper objectMapper;
    private final LyceumService lyceumService;
    private final EmailService emailService;
    private final PasswordResetProperties passwordResetProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Creates a new user if the email/username are unique and password constraints are met,
     * then issues JWT access and refresh tokens.
     *
     * @param request registration payload
     * @return tokens for the newly registered account
     */
    public AuthenticationResponse register(RegisterRequest request) {
        if (repository.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictException("User with such email already exists!");
        }
        if (repository.findByUsername(request.getUsername()).isPresent()) {
            throw new ConflictException("User with such username already exists!");
        }
        if (!request.getPassword().equals(request.getRepeatedPassword())) {
            throw new BadRequestException("Passwords do not match!");
        }
        if (request.getPassword().length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters long!");
        }

        User user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .username(request.getUsername())
                .description(trimToNull(request.getDescription()))
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .emailVerified(true)
                .registrationComplete(true)
                .enabled(true)
                .build();
        User savedUser = repository.save(user);
        lyceumService.acceptLecturerInvitationsFor(savedUser);
        return issueTokens(savedUser);
    }

    /**
     * Authenticates user credentials, revokes existing access tokens, and returns a fresh token pair.
     *
     * @param request login payload
     * @return new access/refresh tokens
     */
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        User user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials!"));
        ensureUserEnabled(user);

        authenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        return issueTokens(user);
    }

    /**
     * Issues a fresh access/refresh token pair for the provided user and revokes previous access tokens.
     *
     * @param user authenticated user
     * @return token pair for API usage
     */
    public AuthenticationResponse issueTokens(User user) {
        String jwtToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        revokeActiveBearerTokens(user);
        saveUserToken(user, jwtToken);
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * Validates a refresh token from the Authorization header and streams a new access token.
     * Refresh tokens remain valid while all previous access tokens are revoked.
     *
     * @param request HTTP request containing the bearer refresh token
     * @param response HTTP response to write the refreshed token payload
     * @throws IOException if writing to the response fails
     */
    public void refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid refresh token");
        }

        final String refreshToken = authHeader.substring(7);
        try {
            final String username = jwtService.extractUsername(refreshToken);
            if (username == null) {
                throw new UnauthorizedException("Invalid refresh token");
            }

            User user = repository.findByUsername(username)
                    .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
            if (!jwtService.isTokenValid(refreshToken, user)) {
                throw new UnauthorizedException("Invalid refresh token");
            }

            String accessToken = jwtService.generateToken(user);
            revokeActiveBearerTokens(user);
            saveUserToken(user, accessToken);

            AuthenticationResponse authResponse = AuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
            ObjectMapper mapper = objectMapper != null ? objectMapper : new ObjectMapper();
            mapper.writeValue(response.getOutputStream(), authResponse);
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnauthorizedException("Invalid refresh token");
        }
    }

    /**
     * Starts the forgot-password flow without revealing whether the email belongs to an account.
     *
     * @param request email used to look up the account
     * @return generic user-facing response
     */
    @Transactional
    public String requestPasswordReset(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());
        User user = repository.findByEmailIgnoreCase(email).orElse(null);

        if (!(user == null || !user.isEnabled())) {
            invalidateActiveTokens(user, token -> token.getTokenType() == TokenType.PASSWORD_RESET);
            String code = createPasswordResetToken(user);
            emailService.sendPasswordResetEmail(
                    user.getEmail(),
                    code,
                    passwordResetProperties.getExpirationMinutes()
            );
        }

        return PASSWORD_RESET_REQUEST_MESSAGE;
    }

    /**
     * Confirms that a forgot-password code is still valid for the supplied email.
     *
     * @param request email and verification code
     * @return confirmation message when the code can still be used
     */
    public String verifyPasswordResetCode(PasswordResetCodeVerificationRequest request) {
        requireValidPasswordResetToken(request.getEmail(), request.getVerificationCode());
        return PASSWORD_RESET_CODE_VALID_MESSAGE;
    }

    /**
     * Resets a forgotten password after validating the verification code and password confirmation.
     *
     * @param request email, verification code, and replacement password
     * @return confirmation message after the password is updated
     */
    @Transactional
    public String resetForgottenPassword(ResetForgottenPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmationPassword())) {
            throw new BadRequestException("Passwords do not match!");
        }

        Token token = requireValidPasswordResetToken(request.getEmail(), request.getVerificationCode());
        User user = token.getUser();
        ensureUserEnabled(user);

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        repository.save(user);
        invalidateActiveTokens(user, activeToken ->
                activeToken.getTokenType() == TokenType.BEARER || activeToken.getTokenType() == TokenType.PASSWORD_RESET
        );
        return PASSWORD_RESET_SUCCESS_MESSAGE;
    }

    private void saveUserToken(User user, String jwtToken) {
        Token token = Token.builder()
                .user(user)
                .tokenValue(jwtToken)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    private void revokeActiveBearerTokens(User user) {
        invalidateActiveTokens(user, token -> token.getTokenType() == TokenType.BEARER);
    }

    private void invalidateActiveTokens(User user, Predicate<Token> predicate) {
        var activeUserTokens = tokenRepository.findAllValidTokenByUser(user.getId())
                .stream()
                .filter(predicate)
                .toList();
        if (activeUserTokens.isEmpty()) {
            return;
        }

        activeUserTokens.forEach(this::expireToken);
        tokenRepository.saveAll(activeUserTokens);
    }

    private String createPasswordResetToken(User user) {
        String tokenValue = generateUniquePasswordResetCode();
        Token token = Token.builder()
                .user(user)
                .tokenValue(tokenValue)
                .tokenType(TokenType.PASSWORD_RESET)
                .createdAt(LocalDateTime.now())
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
        return tokenValue;
    }

    private String generateUniquePasswordResetCode() {
        int codeLength = passwordResetProperties.getCodeLength();
        StringBuilder builder = new StringBuilder(codeLength);
        String code;
        do {
            builder.setLength(0);
            for (int i = 0; i < codeLength; i++) {
                builder.append(secureRandom.nextInt(10));
            }
            code = builder.toString();
        } while (tokenRepository.findByToken(code).isPresent());
        return code;
    }

    private Token requireValidPasswordResetToken(String rawEmail, String rawCode) {
        String email = normalizeEmail(rawEmail);
        String code = normalizeVerificationCode(rawCode);

        User user = repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadRequestException(INVALID_PASSWORD_RESET_CODE_MESSAGE));
        ensureUserEnabled(user);

        Token token = tokenRepository.findByToken(code)
                .orElseThrow(() -> new BadRequestException(INVALID_PASSWORD_RESET_CODE_MESSAGE));
        if (token.getTokenType() != TokenType.PASSWORD_RESET) {
            throw new BadRequestException(INVALID_PASSWORD_RESET_CODE_MESSAGE);
        }
        if (token.getUser() == null || !user.getId().equals(token.getUser().getId())) {
            throw new BadRequestException(INVALID_PASSWORD_RESET_CODE_MESSAGE);
        }
        if (token.isExpired() || token.isRevoked()) {
            throw new BadRequestException(EXPIRED_PASSWORD_RESET_CODE_MESSAGE);
        }
        if (isPasswordResetTokenExpired(token)) {
            expireToken(token);
            tokenRepository.save(token);
            throw new BadRequestException(EXPIRED_PASSWORD_RESET_CODE_MESSAGE);
        }
        return token;
    }

    private boolean isPasswordResetTokenExpired(Token token) {
        LocalDateTime createdAt = token.getCreatedAt();
        if (createdAt == null) {
            return true;
        }
        return createdAt.plusMinutes(passwordResetProperties.getExpirationMinutes()).isBefore(LocalDateTime.now());
    }

    private void ensureUserEnabled(User user) {
        if (!user.isEnabled()) {
            throw new AccessDeniedException("The user is disabled");
        }
    }

    private void expireToken(Token token) {
        token.setExpired(true);
        token.setRevoked(true);
    }

    private String normalizeEmail(String email) {
        String normalizedEmail = trimToNull(email);
        if (!StringUtils.hasText(normalizedEmail)) {
            throw new BadRequestException("Email must not be blank.");
        }
        return normalizedEmail;
    }

    private String normalizeVerificationCode(String code) {
        String normalizedCode = trimToNull(code);
        if (!StringUtils.hasText(normalizedCode)) {
            throw new BadRequestException("Verification code must be provided.");
        }
        return normalizedCode;
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? null : value.trim();
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }
}
