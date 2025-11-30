package com.dev.education_nearby_server.integration.controllers;

import com.dev.education_nearby_server.enums.Role;
import com.dev.education_nearby_server.models.dto.auth.AuthenticationResponse;
import com.dev.education_nearby_server.models.dto.auth.ChangePasswordRequest;
import com.dev.education_nearby_server.models.dto.auth.RegisterRequest;
import com.dev.education_nearby_server.models.dto.response.UserResponse;
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

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void getAllUsersReturnsRegisteredUsers() throws Exception {
        RegisterRequest firstUser = RegisterRequest.builder()
                .firstname("Agent")
                .lastname("Cooper")
                .email("cooper@example.com")
                .username("cooper@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();
        RegisterRequest secondUser = RegisterRequest.builder()
                .firstname("Audrey")
                .lastname("Horne")
                .email("audrey@example.com")
                .username("audrey@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();
        registerViaHttp(firstUser);
        registerViaHttp(secondUser);

        MvcResult result = mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andReturn();

        UserResponse[] responses = objectMapper.readValue(result.getResponse().getContentAsString(), UserResponse[].class);
        List<UserResponse> users = Arrays.asList(responses);

        assertThat(users).extracting(UserResponse::getEmail)
                .containsExactlyInAnyOrder(firstUser.getEmail(), secondUser.getEmail());
        assertThat(users).allSatisfy(user -> {
            assertThat(user.getId()).isNotNull();
            assertThat(user.getRole()).isEqualTo(Role.USER);
            assertThat(user.getUsername()).isNotBlank();
        });
    }

    @Test
    void getUserByIdReturnsSingleUser() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstname("Donna")
                .lastname("Hayward")
                .email("donna@example.com")
                .username("donna@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();
        registerViaHttp(registerRequest);
        Long userId = userRepository.findByEmail(registerRequest.getEmail()).orElseThrow().getId();

        MvcResult result = mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isOk())
                .andReturn();

        UserResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), UserResponse.class);
        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getEmail()).isEqualTo(registerRequest.getEmail());
        assertThat(response.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void getAuthenticatedUserReturnsCurrentPrincipal() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstname("Gordon")
                .lastname("Cole")
                .email("cole@example.com")
                .username("cole@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();
        AuthenticationResponse registration = registerViaHttp(registerRequest);

        Long userId = userRepository.findByEmail(registerRequest.getEmail()).orElseThrow().getId();

        MvcResult result = mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + registration.getAccessToken()))
                .andExpect(status().isOk())
                .andReturn();

        UserResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), UserResponse.class);
        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getEmail()).isEqualTo(registerRequest.getEmail());
        assertThat(response.getRole()).isEqualTo(Role.USER);
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
