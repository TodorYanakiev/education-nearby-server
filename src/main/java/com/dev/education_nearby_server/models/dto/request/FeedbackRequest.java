package com.dev.education_nearby_server.models.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload for unauthenticated feedback submissions.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {

    @NotBlank(message = "Full name must not be blank.")
    @Size(max = 255, message = "Full name should be at most 255 characters.")
    private String fullName;

    @NotBlank(message = "Email must not be blank.")
    @Email(message = "Invalid email format.")
    @Size(max = 255, message = "Email should be at most 255 characters.")
    private String email;

    @NotBlank(message = "Title must not be blank.")
    @Size(max = 255, message = "Title should be at most 255 characters.")
    private String title;

    @NotBlank(message = "Message must not be blank.")
    @Size(max = 5000, message = "Message should be at most 5000 characters.")
    private String message;
}
