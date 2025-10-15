package com.dev.education_nearby_server.exceptions.handlers;

import com.dev.education_nearby_server.models.dto.response.ExceptionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationEntryPointTest {

    @Test
    void commence_writesUnauthorizedResponseBody() throws Exception {
        ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
        JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(mapper);

        MockHttpServletResponse response = new MockHttpServletResponse();
        entryPoint.commence(null, response, new BadCredentialsException("Bad credentials"));

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertEquals("application/json", response.getContentType());

        ExceptionResponse body = mapper.readValue(response.getContentAsByteArray(), ExceptionResponse.class);
        assertEquals("Bad credentials", body.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, body.getStatus());
        assertEquals(401, body.getStatusCode());
        assertNotNull(body.getDateTime());
    }
}

