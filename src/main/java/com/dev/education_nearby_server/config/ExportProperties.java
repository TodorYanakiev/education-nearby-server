package com.dev.education_nearby_server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * File-system storage options for asynchronous subscriber exports.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.exports")
public class ExportProperties {

    /**
     * Base directory where generated export files are stored.
     */
    private String directory = "exports";
}

