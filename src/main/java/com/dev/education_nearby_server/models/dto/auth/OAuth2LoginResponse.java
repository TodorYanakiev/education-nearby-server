package com.dev.education_nearby_server.models.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OAuth2 login outcome returned after successful provider authentication.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OAuth2LoginResponse {

    private String status;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("registration_token")
    private String registrationToken;

    @JsonProperty("missing_fields")
    private List<String> missingFields;
}
