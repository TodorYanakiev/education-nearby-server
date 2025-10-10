package com.dev.education_nearby_server.integration.controllers;

import com.dev.education_nearby_server.models.dto.auth.AuthenticationResponse;
import com.dev.education_nearby_server.models.dto.auth.ChangePasswordRequest;
import com.dev.education_nearby_server.models.dto.auth.RegisterRequest;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.repositories.TokenRepository;
import com.dev.education_nearby_server.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanUp() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void changePasswordEndpointUpdatesStoredPassword() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstname("Donna")
                .lastname("Hayward")
                .email("donna@example.com")
                .username("donna@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();
        AuthenticationResponse registration = registerViaHttp(registerRequest);

        ChangePasswordRequest payload = ChangePasswordRequest.builder()
                .currentPassword("Password123")
                .newPassword("NewPassword456")
                .confirmationPassword("NewPassword456")
                .build();

        mockMvc.perform(patch("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + registration.getAccessToken())
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        User reloaded = userRepository.findByEmail(registerRequest.getEmail()).orElseThrow();
        assertThat(passwordEncoder.matches("NewPassword456", reloaded.getPassword())).isTrue();
    }

    private AuthenticationResponse registerViaHttp(RegisterRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthenticationResponse.class);
    }
}
