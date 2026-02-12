package com.dev.education_nearby_server.models.dto.response;

import com.dev.education_nearby_server.enums.ImageRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Base view model for image metadata exposed via APIs.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ImageMetadataResponse {

    private Long id;
    private String s3Key;
    private String url;
    private ImageRole role;
    private String altText;
    private Integer width;
    private Integer height;
    private String mimeType;
    private Integer orderIndex;
}
