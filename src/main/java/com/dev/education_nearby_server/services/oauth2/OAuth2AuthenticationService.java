package com.dev.education_nearby_server.services.oauth2;

import com.dev.education_nearby_server.config.JwtService;
import com.dev.education_nearby_server.enums.AuthProvider;
import com.dev.education_nearby_server.enums.Role;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles OAuth2 login and registration completion flows.
 */
@Service
@RequiredArgsConstructor
public class OAuth2AuthenticationService {

    private static final String REGISTRATION_TOKEN_TYPE = "OAUTH2_REGISTRATION";
    private static final String STATUS_COMPLETE = "COMPLETE";
    private static final String STATUS_PENDING = "PENDING";
    private static final int USERNAME_MIN = 3;
    private static final int USERNAME_MAX = 50;

    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;
    private final JwtService jwtService;
    private final LyceumService lyceumService;

    public OAuth2LoginResponse handleOAuth2Login(AuthProvider provider, OAuth2User oauth2User) {
        if (oauth2User == null) {
            throw new UnauthorizedException("Missing OAuth2 user information.");
        }
        String providerId = trimToNull(oauth2User.getName());
        if (!StringUtils.hasText(providerId)) {
            throw new UnauthorizedException("Missing OAuth2 provider user id.");
        }

        OAuth2UserInfo userInfo = extractUserInfo(oauth2User);

        User byProvider = userRepository.findByAuthProviderAndAuthProviderId(provider, providerId).orElse(null);
        if (byProvider != null) {
            ensureEnabled(byProvider);
            if (byProvider.isRegistrationComplete()) {
                return completeResponse(authenticationService.issueTokens(byProvider));
            }
            String regToken = generateRegistrationToken(provider, providerId, userInfo);
            return pendingResponse(regToken, determineMissingFields(userInfo));
        }

        String normalizedEmail = trimToNull(userInfo.email());
        if (StringUtils.hasText(normalizedEmail)) {
            User byEmail = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
            if (byEmail != null) {
                ensureEnabled(byEmail);
                linkProvider(byEmail, provider, providerId, userInfo.emailVerified());
                return completeResponse(authenticationService.issueTokens(byEmail));
            }
        }

        String registrationToken = generateRegistrationToken(provider, providerId, userInfo);
        return pendingResponse(registrationToken, determineMissingFields(userInfo));
    }

    public AuthenticationResponse completeRegistration(OAuth2CompleteRegistrationRequest request) {
        OAuth2RegistrationDetails details = parseRegistrationToken(request.getRegistrationToken());

        String username = trimToNull(request.getUsername());
        if (!StringUtils.hasText(username)) {
            throw new BadRequestException("Username must not be blank.");
        }
        if (username.length() < USERNAME_MIN || username.length() > USERNAME_MAX) {
            throw new BadRequestException("Username must be between " + USERNAME_MIN + " and " + USERNAME_MAX + " characters.");
        }

        String email = trimToNull(request.getEmail());
        if (!StringUtils.hasText(email)) {
            email = trimToNull(details.email());
        }
        if (!StringUtils.hasText(email)) {
            throw new BadRequestException("Email is required to complete registration.");
        }

        String firstname = trimToNull(request.getFirstname());
        if (!StringUtils.hasText(firstname)) {
            firstname = trimToNull(details.firstname());
        }
        if (!StringUtils.hasText(firstname)) {
            throw new BadRequestException("First name is required to complete registration.");
        }

        String lastname = trimToNull(request.getLastname());
        if (!StringUtils.hasText(lastname)) {
            lastname = trimToNull(details.lastname());
        }
        if (!StringUtils.hasText(lastname)) {
            throw new BadRequestException("Last name is required to complete registration.");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            throw new ConflictException("User with such username already exists!");
        }
        userRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            throw new ConflictException("User with such email already exists!");
        });

        User existingByProvider = userRepository
                .findByAuthProviderAndAuthProviderId(details.provider(), details.providerId())
                .orElse(null);
        if (existingByProvider != null) {
            ensureEnabled(existingByProvider);
            return authenticationService.issueTokens(existingByProvider);
        }

        User user = User.builder()
                .firstname(firstname)
                .lastname(lastname)
                .email(email)
                .username(username)
                .description(trimToNull(request.getDescription()))
                .role(Role.USER)
                .authProvider(details.provider())
                .authProviderId(details.providerId())
                .emailVerified(details.emailVerified())
                .registrationComplete(true)
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        lyceumService.acceptLecturerInvitationsFor(saved);
        return authenticationService.issueTokens(saved);
    }

    private OAuth2UserInfo extractUserInfo(OAuth2User oauth2User) {
        String email = attribute(oauth2User, "email");
        String firstname = attribute(oauth2User, "given_name");
        String lastname = attribute(oauth2User, "family_name");

        if (!StringUtils.hasText(firstname)) {
            firstname = attribute(oauth2User, "first_name");
        }
        if (!StringUtils.hasText(lastname)) {
            lastname = attribute(oauth2User, "last_name");
        }

        String fullName = attribute(oauth2User, "name");
        if ((!StringUtils.hasText(firstname) || !StringUtils.hasText(lastname)) && StringUtils.hasText(fullName)) {
            String[] parts = fullName.trim().split("\\s+", 2);
            if (!StringUtils.hasText(firstname) && parts.length > 0) {
                firstname = parts[0];
            }
            if (!StringUtils.hasText(lastname) && parts.length > 1) {
                lastname = parts[1];
            }
        }

        Boolean emailVerified = booleanAttribute(oauth2User, "email_verified");
        return new OAuth2UserInfo(trimToNull(email), trimToNull(firstname), trimToNull(lastname), emailVerified != null && emailVerified);
    }

    private String attribute(OAuth2User oauth2User, String key) {
        Object value = oauth2User.getAttributes().get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Boolean booleanAttribute(OAuth2User oauth2User, String key) {
        Object value = oauth2User.getAttributes().get(key);
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        if (value instanceof String text) {
            return Boolean.parseBoolean(text);
        }
        return null;
    }

    private void ensureEnabled(User user) {
        if (!user.isEnabled()) {
            throw new UnauthorizedException("The user is disabled");
        }
    }

    private void linkProvider(User user, AuthProvider provider, String providerId, boolean emailVerified) {
        if (user.getAuthProvider() != AuthProvider.LOCAL && user.getAuthProvider() != provider) {
            throw new ConflictException("Account is already linked to another provider.");
        }
        if (StringUtils.hasText(user.getAuthProviderId()) && !user.getAuthProviderId().equals(providerId)) {
            throw new ConflictException("Account is already linked to another " + provider.name().toLowerCase() + " identity.");
        }
        user.setAuthProvider(provider);
        user.setAuthProviderId(providerId);
        if (emailVerified) {
            user.setEmailVerified(true);
        }
        user.setRegistrationComplete(true);
        userRepository.save(user);
    }

    private String generateRegistrationToken(AuthProvider provider, String providerId, OAuth2UserInfo userInfo) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", REGISTRATION_TOKEN_TYPE);
        claims.put("provider", provider.name());
        claims.put("provider_id", providerId);
        claims.put("email", userInfo.email());
        claims.put("firstname", userInfo.firstname());
        claims.put("lastname", userInfo.lastname());
        claims.put("email_verified", userInfo.emailVerified());
        return jwtService.generateOauth2RegistrationToken(claims, "oauth2:" + provider.name() + ":" + providerId);
    }

    private OAuth2RegistrationDetails parseRegistrationToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new UnauthorizedException("Missing registration token.");
        }
        Claims claims;
        try {
            claims = jwtService.extractAllClaims(token);
        } catch (Exception ex) {
            throw new UnauthorizedException("Invalid registration token.");
        }
        if (!REGISTRATION_TOKEN_TYPE.equals(claims.get("type", String.class))) {
            throw new UnauthorizedException("Invalid registration token.");
        }
        if (jwtService.isTokenExpired(token)) {
            throw new UnauthorizedException("Registration token expired.");
        }
        String providerValue = claims.get("provider", String.class);
        AuthProvider provider;
        try {
            provider = AuthProvider.valueOf(providerValue);
        } catch (Exception ex) {
            throw new UnauthorizedException("Invalid registration token provider.");
        }
        String providerId = claims.get("provider_id", String.class);
        if (!StringUtils.hasText(providerId)) {
            throw new UnauthorizedException("Invalid registration token provider id.");
        }
        String email = claims.get("email", String.class);
        String firstname = claims.get("firstname", String.class);
        String lastname = claims.get("lastname", String.class);
        Boolean emailVerified = claims.get("email_verified", Boolean.class);
        return new OAuth2RegistrationDetails(provider, providerId, email, firstname, lastname, emailVerified != null && emailVerified);
    }

    private OAuth2LoginResponse completeResponse(AuthenticationResponse tokens) {
        return OAuth2LoginResponse.builder()
                .status(STATUS_COMPLETE)
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .build();
    }

    private OAuth2LoginResponse pendingResponse(String registrationToken, List<String> missingFields) {
        return OAuth2LoginResponse.builder()
                .status(STATUS_PENDING)
                .registrationToken(registrationToken)
                .missingFields(missingFields)
                .build();
    }

    private List<String> determineMissingFields(OAuth2UserInfo userInfo) {
        List<String> missing = new ArrayList<>();
        missing.add("username");
        if (!StringUtils.hasText(userInfo.email())) {
            missing.add("email");
        }
        if (!StringUtils.hasText(userInfo.firstname())) {
            missing.add("firstname");
        }
        if (!StringUtils.hasText(userInfo.lastname())) {
            missing.add("lastname");
        }
        return missing;
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? null : value.trim();
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }

    private record OAuth2UserInfo(String email, String firstname, String lastname, boolean emailVerified) {
    }

    private record OAuth2RegistrationDetails(AuthProvider provider, String providerId, String email,
                                             String firstname, String lastname, boolean emailVerified) {
    }
}
