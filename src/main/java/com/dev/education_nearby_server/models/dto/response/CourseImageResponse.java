package com.dev.education_nearby_server.models.dto.response;

import com.dev.education_nearby_server.enums.ImageRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * View model for course images exposed via APIs.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseImageResponse {

    private Long id;
    private Long courseId;
    private String s3Key;
    private String url;
    private ImageRole role;
    private String altText;
    private Integer width;
    private Integer height;
    private String mimeType;
    private Integer orderIndex;
}
