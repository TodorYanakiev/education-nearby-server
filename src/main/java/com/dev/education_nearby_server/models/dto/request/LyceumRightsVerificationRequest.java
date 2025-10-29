package com.dev.education_nearby_server.models.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LyceumRightsVerificationRequest {

    @NotBlank(message = "Verification code must not be blank!")
    private String verificationCode;
}
