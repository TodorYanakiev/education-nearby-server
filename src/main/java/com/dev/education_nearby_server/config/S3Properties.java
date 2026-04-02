package com.dev.education_nearby_server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * S3 configuration used for image metadata validation and URL construction.
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
     * AWS region where the bucket is located.
     */
    private String region = "eu-central-1";

    /**
     * Optional required prefix for all course images (e.g. courses/{courseId}/images/).
     */
    private String allowedPrefix = "courses/";

    /**
     * Optional required prefix for all lyceum images (e.g. lyceums/{lyceumId}/images/).
     */
    private String lyceumAllowedPrefix = "lyceums/";

    /**
     * Optional required prefix for all user profile images (e.g. users/{userId}/profile/).
     */
    private String userAllowedPrefix = "users/";
}
