package com.dev.education_nearby_server.config.oauth2;

import com.dev.education_nearby_server.enums.AuthProvider;
import com.dev.education_nearby_server.models.dto.auth.OAuth2LoginResponse;
import com.dev.education_nearby_server.services.oauth2.OAuth2AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2AuthenticationService oAuth2AuthenticationService;
    private final ObjectMapper objectMapper;
    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Value("${app.oauth2.redirect-uri:}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        clearAuthorizationRequestCookies(request, response);

        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid OAuth2 authentication.");
            return;
        }

        AuthProvider provider;
        try {
            provider = resolveProvider(token.getAuthorizedClientRegistrationId());
        } catch (IllegalArgumentException ex) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unsupported OAuth2 provider.");
            return;
        }
        OAuth2User oauth2User = token.getPrincipal();
        OAuth2LoginResponse loginResponse = oAuth2AuthenticationService.handleOAuth2Login(provider, oauth2User);

        if (StringUtils.hasText(redirectUri)) {
            response.sendRedirect(buildRedirectUrl(redirectUri, loginResponse));
            return;
        }

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), loginResponse);
    }

    private void clearAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        authorizationRequestRepository.removeAuthorizationRequest(request, response);
    }

    private AuthProvider resolveProvider(String registrationId) {
        if (!StringUtils.hasText(registrationId)) {
            throw new IllegalArgumentException("Missing OAuth2 provider.");
        }
        if ("google".equalsIgnoreCase(registrationId.trim())) {
            return AuthProvider.GOOGLE;
        }
        throw new IllegalArgumentException("Unsupported OAuth2 provider.");
    }

    private String buildRedirectUrl(String baseUri, OAuth2LoginResponse loginResponse) {
        String fragment;
        if ("COMPLETE".equalsIgnoreCase(loginResponse.getStatus())) {
            fragment = buildFragment(
                    "status", "complete",
                    "access_token", loginResponse.getAccessToken(),
                    "refresh_token", loginResponse.getRefreshToken()
            );
        } else {
            String missingFields = joinMissingFields(loginResponse.getMissingFields());
            fragment = buildFragment(
                    "status", "pending",
                    "registration_token", loginResponse.getRegistrationToken(),
                    "missing_fields", missingFields
            );
        }
        return baseUri + "#" + fragment;
    }

    private String joinMissingFields(List<String> missingFields) {
        if (missingFields == null || missingFields.isEmpty()) {
            return "";
        }
        return String.join(",", missingFields);
    }

    private String buildFragment(String... pairs) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < pairs.length; i += 2) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(encode(pairs[i]))
                    .append("=")
                    .append(encode(pairs[i + 1] == null ? "" : pairs[i + 1]));
        }
        return builder.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
