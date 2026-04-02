package com.dev.education_nearby_server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Storage options for asynchronous subscriber exports.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.exports")
public class ExportProperties {

    /**
     * Temporary local directory used while generating export files before uploading to S3.
     */
    private String directory = "exports";

    /**
     * S3 object key prefix where generated export files are uploaded.
     */
    private String s3Prefix = "exports/subscribers/";

    /**
     * Presigned download URL validity in minutes.
     */
    private int presignedUrlMinutes = 10;
}
