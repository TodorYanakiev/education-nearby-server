package com.dev.education_nearby_server.config;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 60_000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 120_000L);
        ReflectionTestUtils.setField(jwtService, "oauth2RegistrationExpiration", 180_000L);
    }

    @Test
    void generateTokenAndValidateToken() {
        UserDetails user = User.withUsername("john@example.com")
                .password("pass")
                .authorities("ROLE_USER")
                .build();

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("john@example.com");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    @Test
    void generateTokenWithExtraClaimsAndExtractClaim() {
        UserDetails user = User.withUsername("alice@example.com")
                .password("pass")
                .authorities("ROLE_USER")
                .build();
        Map<String, Object> claims = new HashMap<>();
        claims.put("scope", "api");

        String token = jwtService.generateToken(claims, user);
        String scope = jwtService.extractClaim(token, c -> c.get("scope", String.class));

        assertThat(scope).isEqualTo("api");
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice@example.com");
    }

    @Test
    void generateRefreshTokenUsesRefreshExpiration() {
        UserDetails user = User.withUsername("refresh@example.com")
                .password("pass")
                .authorities("ROLE_USER")
                .build();

        String refreshToken = jwtService.generateRefreshToken(user);

        assertThat(jwtService.extractUsername(refreshToken)).isEqualTo("refresh@example.com");
        assertThat(jwtService.isTokenExpired(refreshToken)).isFalse();
    }

    @Test
    void generateOauth2RegistrationTokenPersistsClaimsAndSubject() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "OAUTH2_REGISTRATION");
        claims.put("provider", "GOOGLE");

        String token = jwtService.generateOauth2RegistrationToken(claims, "oauth2:GOOGLE:provider-id");
        Claims extracted = jwtService.extractAllClaims(token);

        assertThat(extracted.getSubject()).isEqualTo("oauth2:GOOGLE:provider-id");
        assertThat(extracted.get("type", String.class)).isEqualTo("OAUTH2_REGISTRATION");
        assertThat(extracted.get("provider", String.class)).isEqualTo("GOOGLE");
    }

    @Test
    void generateTokenWithSubjectCanProduceExpiredToken() {
        String token = jwtService.generateTokenWithSubject(Map.of("kind", "test"), "subject", -1_000L);

        assertThrows(io.jsonwebtoken.ExpiredJwtException.class, () -> jwtService.isTokenExpired(token));
        assertThrows(io.jsonwebtoken.ExpiredJwtException.class, () -> jwtService.isTokenValid(token, User.withUsername("subject")
                .password("pass")
                .authorities("ROLE_USER")
                .build()));
    }
}
