package com.dev.education_nearby_server.models.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Representation of a submitted feedback message.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {
    private Long id;
    private String fullName;
    private String email;
    private String title;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;
}
