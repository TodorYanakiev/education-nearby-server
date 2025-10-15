package com.dev.education_nearby_server.exceptions.handlers;

import com.dev.education_nearby_server.exceptions.common.AccessDeniedException;
import com.dev.education_nearby_server.exceptions.common.ApiException;
import com.dev.education_nearby_server.exceptions.common.ConflictException;
import com.dev.education_nearby_server.exceptions.common.InternalServerErrorException;
import com.dev.education_nearby_server.exceptions.common.UnauthorizedException;
import com.dev.education_nearby_server.exceptions.common.ValidationException;
import com.dev.education_nearby_server.models.dto.response.ExceptionResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.BindException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CustomExceptionHandlerTest {

    private final CustomExceptionHandler handler = new CustomExceptionHandler();

    @Test
    void handleResponseStatusExceptions_returnsProvidedStatusAndMessage() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Not here");
        var resp = handler.handleResponseStatusExceptions(ex);

        assertEquals(404, resp.getStatusCode().value());
        ExceptionResponse body = resp.getBody();
        assertNotNull(body);
        assertEquals("Not here", body.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, body.getStatus());
        assertEquals(404, body.getStatusCode());
    }

    @Test
    void handleRuntimeExceptions_mapsToInternalServerError() {
        var resp = handler.handleRuntimeExceptions(new RuntimeException("boom"));
        assertEquals(500, resp.getStatusCode().value());
        ExceptionResponse body = resp.getBody();
        assertNotNull(body);
        assertEquals(new InternalServerErrorException().getMessage(), body.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, body.getStatus());
    }

    @Test
    void handleInternalAuthServiceExceptions_unwrapsApiException() {
        ApiException cause = new UnauthorizedException("Nope");
        var ex = new org.springframework.security.authentication.InternalAuthenticationServiceException("auth", cause);
        var resp = handler.handleInternalAuthServiceExceptions(ex);
        assertEquals(401, resp.getStatusCode().value());
        ExceptionResponse body = resp.getBody();
        assertNotNull(body);
        assertEquals("Nope", body.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, body.getStatus());
    }

    @Test
    void handleBadCredentialsExceptions_returns401() {
        var resp = handler.handleBadCredentialsExceptions();
        assertEquals(401, resp.getStatusCode().value());
        ExceptionResponse body = resp.getBody();
        assertNotNull(body);
        assertEquals("Invalid credentials!", body.getMessage());
        assertEquals(401, body.getStatusCode());
    }

    @Test
    void handleAccessDeniedExceptions_returns403() {
        var resp = handler.handleAccessDeniedExceptions();
        assertEquals(403, resp.getStatusCode().value());
        ExceptionResponse body = resp.getBody();
        assertNotNull(body);
        assertEquals(new AccessDeniedException().getMessage(), body.getMessage());
        assertEquals(HttpStatus.FORBIDDEN, body.getStatus());
    }

    @Test
    void handleApiExceptions_passthroughStatusAndMessage() {
        var resp = handler.handleApiExceptions(new ConflictException("dup"));
        assertEquals(409, resp.getStatusCode().value());
        ExceptionResponse body = resp.getBody();
        assertNotNull(body);
        assertEquals("dup", body.getMessage());
        assertEquals(HttpStatus.CONFLICT, body.getStatus());
    }

    @Test
    void handleTransactionExceptions_extractsConstraintViolations() {
        // Build a real violation via Validator
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        var invalid = new DummyBean("");
        Set<ConstraintViolation<DummyBean>> violations = validator.validate(invalid);
        var cve = new jakarta.validation.ConstraintViolationException(violations);
        var ex = new TransactionSystemException("tx", cve);

        var resp = handler.handleTransactionExceptions(ex);
        assertEquals(400, resp.getStatusCode().value());
        ExceptionResponse body = resp.getBody();
        assertNotNull(body);
        assertTrue(body.getMessage() != null && !body.getMessage().isBlank());
        assertEquals(HttpStatus.BAD_REQUEST, body.getStatus());
    }

    @Test
    void handleConstraintValidationExceptions_joinsMessages() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        var invalid = new DummyBean("");
        Set<ConstraintViolation<DummyBean>> violations = validator.validate(invalid);
        var resp = handler.handleConstraintValidationExceptions(new jakarta.validation.ConstraintViolationException(violations));
        assertEquals(400, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals(HttpStatus.BAD_REQUEST, resp.getBody().getStatus());
    }

    @Test
    void handleDataIntegrityViolation_mysqlDuplicateEmail_isMapped() {
        String msg = "Duplicate entry 'abc' for key 'email'";
        var cause = new RuntimeException(msg);
        var resp = handler.handleDataIntegrityViolation(new DataIntegrityViolationException("wrap", cause));
        assertEquals(409, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals("User with such email already exists!", resp.getBody().getMessage());
    }

    @Test
    void handleDataIntegrityViolation_postgresDuplicateUsername_isMapped() {
        String msg = "duplicate key value violates unique constraint \"user_username_key\"";
        var cause = new RuntimeException(msg);
        var resp = handler.handleDataIntegrityViolation(new DataIntegrityViolationException("wrap", cause));
        assertEquals(409, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals("User with such username already exists!", resp.getBody().getMessage());
    }

    @Test
    void handleDataIntegrityViolation_h2UniqueIndex_username_isMapped() {
        String msg = "Unique index or primary key violation (USERNAME)";
        var cause = new RuntimeException(msg);
        var resp = handler.handleDataIntegrityViolation(new DataIntegrityViolationException("wrap", cause));
        assertEquals(409, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals("User with such username already exists!", resp.getBody().getMessage());
    }

    @Test
    void handleBindException_collectsDefaultMessages_HttpStatusCodeOverload() {
        BindException ex = new BindException(new Object(), "obj");
        ex.addError(new ObjectError("obj", "first"));
        ex.addError(new ObjectError("obj", "second"));
        var resp = handler.handleBindException(ex, new HttpHeaders(), HttpStatusCode.valueOf(400), null);
        assertEquals(400, resp.getStatusCode().value());
        ExceptionResponse body = (ExceptionResponse) resp.getBody();
        assertNotNull(body);
        assertEquals("first\nsecond", body.getMessage());
    }

    @Test
    void handleBindException_collectsDefaultMessages_HttpStatusOverload() {
        BindException ex = new BindException(new Object(), "obj");
        ex.addError(new ObjectError("obj", "only"));
        var resp = handler.handleBindException(ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, null);
        assertEquals(400, resp.getStatusCode().value());
        ExceptionResponse body = (ExceptionResponse) resp.getBody();
        assertNotNull(body);
        assertEquals("only", body.getMessage());
    }

    @Test
    void handleMethodArgumentNotValid_aggregatesMessages() throws Exception {
        class Dummy { void foo(String name) {} }
        var mp = new MethodParameter(Dummy.class.getDeclaredMethod("foo", String.class), 0);
        var target = new Object();
        var binding = new BeanPropertyBindingResult(target, "target");
        binding.addError(new ObjectError("target", "m1"));
        binding.addError(new ObjectError("target", "m2"));
        var manve = new MethodArgumentNotValidException(mp, binding);

        var resp = handler.handleMethodArgumentNotValid(manve, new HttpHeaders(), HttpStatusCode.valueOf(400), null);
        assertEquals(400, resp.getStatusCode().value());
        ExceptionResponse body = (ExceptionResponse) resp.getBody();
        assertNotNull(body);
        assertEquals("m1\nm2", body.getMessage());
    }

    // Dummy bean to generate a validation error
    static class DummyBean {
        @NotBlank
        String name;

        DummyBean(String name) {
            this.name = name;
        }
    }
}
