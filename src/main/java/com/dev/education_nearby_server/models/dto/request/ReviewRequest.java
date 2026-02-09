package com.dev.education_nearby_server.models.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

/**
 * Payload for creating a review with a rating and optional comment.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {

    @NotNull(message = "Rating should not be null!")
    @Min(1)
    @Max(5)
    private Integer rating;

    @Length(max = 500, message = "Comment should be at most 500 characters!")
    private String comment;
}
