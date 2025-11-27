package com.dev.education_nearby_server.utils;

import com.dev.education_nearby_server.exceptions.common.ApiException;
import com.dev.education_nearby_server.models.dto.response.ExceptionResponse;

import java.time.LocalDateTime;

/**
 * Translates {@link ApiException} instances into serializable {@link ExceptionResponse} objects.
 */
public class ApiExceptionParser {
    /**
     * Copies key details from an ApiException while attaching the current timestamp.
     */
    public static ExceptionResponse parseException(ApiException exception) {
        return ExceptionResponse
                .builder()
                .dateTime(LocalDateTime.now())
                .message(exception.getMessage())
                .status(exception.getStatus())
                .statusCode(exception.getStatusCode())
                .build();
    }
}
