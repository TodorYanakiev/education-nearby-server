package com.dev.education_nearby_server.exceptions.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationEntryPointTest {

    @Test
    void commenceWritesJsonErrorBody() throws IOException, ServletException {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(mapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Auth required"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");

        JsonNode node = mapper.readTree(response.getContentAsString());

        assertThat(node.get("statusCode").asInt()).isEqualTo(401);
        assertThat(node.get("status").asText()).isEqualTo("UNAUTHORIZED");
        assertThat(node.get("message").asText()).isEqualTo("Auth required");
        assertThat(node.hasNonNull("dateTime")).isTrue();
    }
}
