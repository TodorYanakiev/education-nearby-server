package com.dev.education_nearby_server.models.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload for inviting a lecturer to a lyceum by email.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LyceumLecturerInviteRequest {

    @NotBlank(message = "Email must not be blank.")
    @Email(message = "Invalid email format.")
    private String email;

    private Long lyceumId;
}
