package com.dev.education_nearby_server.models.dto.request;

import com.dev.education_nearby_server.enums.ImageRole;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload for registering or updating a course image reference (S3 key or URL) with metadata.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseImageRequest {

    @Size(max = 512, message = "S3 key must be at most 512 characters.")
    private String s3Key;

    @Size(max = 1024, message = "URL must be at most 1024 characters.")
    private String url;

    @NotNull(message = "Image role is required.")
    private ImageRole role;

    @Size(max = 255, message = "Alt text must be at most 255 characters.")
    private String altText;

    @Positive(message = "Width must be positive.")
    private Integer width;

    @Positive(message = "Height must be positive.")
    private Integer height;

    @Size(max = 100, message = "Mime type must be at most 100 characters.")
    private String mimeType;

    @Min(value = 0, message = "Order index cannot be negative.")
    private Integer orderIndex;
}
