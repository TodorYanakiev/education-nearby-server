package com.dev.education_nearby_server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * S3 configuration used for course images: expected bucket, optional public base URL,
 * and allowed key prefix for validation.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.s3")
public class S3Properties {

    /**
     * Expected bucket name. Used to validate incoming URLs and derive URLs when only a key is provided.
     */
    private String bucketName;

    /**
     * Optional public/base URL prefix (e.g. https://education-nearby-course-images.s3.eu-central-1.amazonaws.com/).
     * If set, it is used to build a URL when only an S3 key is provided.
     */
    private String publicBaseUrl;

    /**
     * Optional required prefix for all course images (e.g. courses/{courseId}/images/).
     */
    private String allowedPrefix = "courses/";
}
