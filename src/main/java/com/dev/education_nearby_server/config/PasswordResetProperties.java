package com.dev.education_nearby_server.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for forgot-password verification codes.
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app.auth.password-reset")
public class PasswordResetProperties {

    /**
     * Number of minutes a reset code remains valid after it is issued.
     */
    @Min(1)
    private long expirationMinutes = 15;

    /**
     * Human-entered numeric verification code length.
     */
    @Min(4)
    @Max(10)
    private int codeLength = 6;
}
