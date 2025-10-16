package com.dev.education_nearby_server.utils;

import com.dev.education_nearby_server.exceptions.common.ApiException;
import com.dev.education_nearby_server.models.dto.response.ExceptionResponse;

import java.time.LocalDateTime;

public class ApiExceptionParser {
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
