package com.dev.education_nearby_server.exceptions.handlers;

import com.dev.education_nearby_server.exceptions.common.AccessDeniedException;
import com.dev.education_nearby_server.exceptions.common.ApiException;
import com.dev.education_nearby_server.exceptions.common.InternalServerErrorException;
import com.dev.education_nearby_server.exceptions.common.ValidationException;
import com.dev.education_nearby_server.exceptions.common.UnauthorizedException;
import com.dev.education_nearby_server.models.dto.response.ExceptionResponse;

import com.dev.education_nearby_server.utils.ApiExceptionParser;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.time.LocalDateTime;

@ControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String VALIDATION_FAILED = "Validation failed";

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ExceptionResponse> handleResponseStatusExceptions(ResponseStatusException exception) {
        int code = exception.getStatusCode().value();
        HttpStatus status = HttpStatus.resolve(code);

        ExceptionResponse body = ExceptionResponse.builder()
                .dateTime(LocalDateTime.now())
                .message(exception.getReason() != null ? exception.getReason() : exception.getMessage())
                .status(status)
                .statusCode(code)
                .build();

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(body);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ExceptionResponse> handleRuntimeExceptions(RuntimeException exception) {
        return handleApiExceptions(new InternalServerErrorException());
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ResponseEntity<ExceptionResponse> handleInternalAuthServiceExceptions(InternalAuthenticationServiceException exception) {
        Throwable cause = exception.getCause();

        if (cause instanceof ApiException) {
            return handleApiExceptions((ApiException) cause);
        }

        return handleRuntimeExceptions(exception);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ExceptionResponse> handleBadCredentialsExceptions() {
        return handleApiExceptions(new UnauthorizedException("Invalid credentials!"));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ExceptionResponse> handleAccessDeniedExceptions() {
        return handleApiExceptions(new AccessDeniedException());
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ExceptionResponse> handleApiExceptions(ApiException exception) {
        ExceptionResponse apiException = ApiExceptionParser.parseException(exception);

        return ResponseEntity
                .status(apiException.getStatus())
                .body(apiException);
    }

    @ExceptionHandler(TransactionException.class)
    public ResponseEntity<ExceptionResponse> handleTransactionExceptions(org.springframework.transaction.TransactionException exception) {
        if (exception.getRootCause() instanceof ConstraintViolationException) {
            return handleConstraintValidationExceptions((ConstraintViolationException) exception.getRootCause());
        }

        return handleRuntimeExceptions(exception);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ExceptionResponse> handleConstraintValidationExceptions(ConstraintViolationException exception) {
        return handleApiExceptions(new ValidationException(exception.getConstraintViolations()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ExceptionResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        String message = prettyDataIntegrityMessage(exception);
        return handleApiExceptions(new com.dev.education_nearby_server.exceptions.common.ConflictException(message));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String message = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .reduce((a, b) -> a + "\n" + b)
                .orElse(VALIDATION_FAILED);

        ResponseEntity<ExceptionResponse> resp = handleApiExceptions(new ValidationException(message));
        return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
    }

    protected ResponseEntity<Object> handleBindException(
            BindException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String message = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .reduce((a, b) -> a + "\n" + b)
                .orElse(VALIDATION_FAILED);

        ResponseEntity<ExceptionResponse> resp = handleApiExceptions(new ValidationException(message));
        return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
    }

    // Compatibility overload for environments expecting HttpStatus
    protected ResponseEntity<Object> handleBindException(
            BindException ex,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request) {
        String message = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .reduce((a, b) -> a + "\n" + b)
                .orElse(VALIDATION_FAILED);

        ResponseEntity<ExceptionResponse> resp = handleApiExceptions(new ValidationException(message));
        return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
    }

    private String prettyDataIntegrityMessage(DataIntegrityViolationException ex) {
        Throwable cause = ex.getRootCause() != null ? ex.getRootCause() : ex;
        String raw = cause.getMessage();
        if (!StringUtils.hasText(raw)) {
            raw = ex.getMessage();
        }
        if (!StringUtils.hasText(raw)) {
            return "Data integrity violation";
        }

        String lower = raw.toLowerCase();

        // PostgreSQL
        if (lower.contains("duplicate key value violates unique constraint")) {
            String key = null;
            int p = lower.indexOf("unique constraint ");
            if (p >= 0) {
                int start = raw.indexOf('"', p);
                if (start >= 0) {
                    int end = raw.indexOf('"', start + 1);
                    if (end > start) {
                        key = raw.substring(start + 1, end);
                    }
                }
            }
            String mapped = mapUniqueKeyToFieldMessage(key);
            return mapped != null ? mapped : "Duplicate key value violates unique constraint";
        }

        // MySQL/MariaDB
        if (lower.contains("duplicate entry")) {
            String key = extractBetween(raw, "for key '", "'");
            String mapped = mapUniqueKeyToFieldMessage(key);
            return mapped != null ? mapped : "Duplicate entry for unique key";
        }

        // H2
        if (lower.contains("unique index") || lower.contains("primary key violation")) {
            String cols = extractBetween(raw, "(", ")");
            String mapped = mapUniqueKeyToFieldMessage(cols);
            return mapped != null ? mapped : "Unique index or primary key violation";
        }

        return "Data integrity violation";
    }

    private String extractBetween(String text, String start, String end) {
        if (text == null) return null;
        int i = text.indexOf(start);
        if (i < 0) return null;
        i += start.length();
        int j = text.indexOf(end, i);
        if (j < 0) return null;
        return text.substring(i, j);
    }

    private String mapUniqueKeyToFieldMessage(String keyOrCols) {
        if (!StringUtils.hasText(keyOrCols)) return null;
        String k = keyOrCols.toLowerCase();
        if (k.contains("email")) {
            return "User with such email already exists!";
        }
        if (k.contains("username") || k.contains("user_name")) {
            return "User with such username already exists!";
        }
        return null;
    }
}
