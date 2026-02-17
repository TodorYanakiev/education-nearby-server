package com.dev.education_nearby_server.integration.controllers;

import com.dev.education_nearby_server.enums.Role;
import com.dev.education_nearby_server.models.dto.auth.AuthenticationResponse;
import com.dev.education_nearby_server.models.dto.auth.ChangePasswordRequest;
import com.dev.education_nearby_server.models.dto.auth.RegisterRequest;
import com.dev.education_nearby_server.models.dto.request.UserImageRequest;
import com.dev.education_nearby_server.models.dto.request.UserRoleUpdateRequest;
import com.dev.education_nearby_server.models.dto.request.UserUpdateRequest;
import com.dev.education_nearby_server.models.dto.response.UserResponse;
import com.dev.education_nearby_server.models.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode content = payload.path("content");
        UserResponse[] responses = objectMapper.treeToValue(content, UserResponse[].class);
        List<UserResponse> users = Arrays.asList(responses);

        assertThat(users).hasSize(2);
        assertThat(users).extracting(UserResponse::getEmail)
                .containsExactlyInAnyOrder(firstUser.getEmail(), secondUser.getEmail());
        assertThat(users).allSatisfy(user -> {
            assertThat(user.getId()).isNotNull();
            assertThat(user.getRole()).isEqualTo(Role.USER);
            assertThat(user.getUsername()).isNotBlank();
            assertThat(user.getLecturedCourseIds()).isEmpty();
            assertThat(user.getLecturedLyceumIds()).isEmpty();
        });
        assertThat(payload.path("number").asInt()).isEqualTo(0);
        assertThat(payload.path("size").asInt()).isEqualTo(9);
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
        assertThat(response.getLecturedCourseIds()).isEmpty();
        assertThat(response.getLecturedLyceumIds()).isEmpty();
    }

    @Test
    void getUserByEmailReturnsSingleUser() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstname("Norma")
                .lastname("Jennings")
                .email("norma@example.com")
                .username("norma@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();
        registerViaHttp(registerRequest);

        MvcResult result = mockMvc.perform(get("/api/v1/users/by-email")
                        .param("email", "  norma@example.com  "))
                .andExpect(status().isOk())
                .andReturn();

        UserResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), UserResponse.class);
        assertThat(response.getEmail()).isEqualTo(registerRequest.getEmail());
        assertThat(response.getRole()).isEqualTo(Role.USER);
        assertThat(response.getLecturedCourseIds()).isEmpty();
        assertThat(response.getLecturedLyceumIds()).isEmpty();
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
        assertThat(response.getLecturedCourseIds()).isEmpty();
        assertThat(response.getLecturedLyceumIds()).isEmpty();
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

    @Test
    void userCanUpdateAndDeleteOwnProfile() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstname("Annie")
                .lastname("Blackburn")
                .email("annie@example.com")
                .username("annie@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();
        AuthenticationResponse auth = registerViaHttp(registerRequest);
        Long userId = userRepository.findByEmail(registerRequest.getEmail()).orElseThrow().getId();

        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .firstname("Annie Updated")
                .lastname("Blackburn Updated")
                .email("annie.updated@example.com")
                .username("annie@example.com")
                .description("Updated bio")
                .build();

        mockMvc.perform(put("/api/v1/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + auth.getAccessToken())
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value("annie.updated@example.com"))
                .andExpect(jsonPath("$.username").value("annie@example.com"))
                .andExpect(jsonPath("$.description").value("Updated bio"));

        mockMvc.perform(delete("/api/v1/users/{userId}", userId)
                        .header("Authorization", "Bearer " + auth.getAccessToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/{userId}", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotUpdateOrDeleteAnotherUser() throws Exception {
        RegisterRequest firstUser = RegisterRequest.builder()
                .firstname("Lucy")
                .lastname("Moran")
                .email("lucy@example.com")
                .username("lucy@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();
        RegisterRequest secondUser = RegisterRequest.builder()
                .firstname("Hawk")
                .lastname("Hill")
                .email("hawk@example.com")
                .username("hawk@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();
        AuthenticationResponse firstAuth = registerViaHttp(firstUser);
        registerViaHttp(secondUser);
        Long secondUserId = userRepository.findByEmail(secondUser.getEmail()).orElseThrow().getId();

        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .firstname("Other")
                .lastname("User")
                .email("other@example.com")
                .username("other-user")
                .build();

        mockMvc.perform(put("/api/v1/users/{userId}", secondUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + firstAuth.getAccessToken())
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/users/{userId}", secondUserId)
                        .header("Authorization", "Bearer " + firstAuth.getAccessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUpdateAndDeleteAnotherUser() throws Exception {
        RegisterRequest adminRegister = RegisterRequest.builder()
                .firstname("Albert")
                .lastname("Rosenfield")
                .email("albert.admin@example.com")
                .username("albert.admin@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();
        RegisterRequest targetRegister = RegisterRequest.builder()
                .firstname("Shelly")
                .lastname("Johnson")
                .email("shelly@example.com")
                .username("shelly@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();

        AuthenticationResponse adminAuth = registerViaHttp(adminRegister);
        registerViaHttp(targetRegister);

        User admin = userRepository.findByEmail(adminRegister.getEmail()).orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        Long targetUserId = userRepository.findByEmail(targetRegister.getEmail()).orElseThrow().getId();
        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .firstname("Shelly Updated")
                .lastname("Johnson Updated")
                .email("shelly.updated@example.com")
                .username("shelly-updated")
                .description("Updated by admin")
                .build();

        mockMvc.perform(put("/api/v1/users/{userId}", targetUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminAuth.getAccessToken())
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("shelly.updated@example.com"))
                .andExpect(jsonPath("$.username").value("shelly-updated"));

        mockMvc.perform(delete("/api/v1/users/{userId}", targetUserId)
                        .header("Authorization", "Bearer " + adminAuth.getAccessToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/{userId}", targetUserId))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCanChangeAnotherUsersRole() throws Exception {
        RegisterRequest adminRegister = RegisterRequest.builder()
                .firstname("Major")
                .lastname("Briggs")
                .email("major.admin@example.com")
                .username("major.admin@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();
        RegisterRequest targetRegister = RegisterRequest.builder()
                .firstname("Bobby")
                .lastname("Briggs")
                .email("bobby@example.com")
                .username("bobby@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();

        AuthenticationResponse adminAuth = registerViaHttp(adminRegister);
        registerViaHttp(targetRegister);

        User admin = userRepository.findByEmail(adminRegister.getEmail()).orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        Long targetUserId = userRepository.findByEmail(targetRegister.getEmail()).orElseThrow().getId();
        UserRoleUpdateRequest roleRequest = UserRoleUpdateRequest.builder()
                .role(Role.ADMIN)
                .build();

        mockMvc.perform(patch("/api/v1/users/{userId}/role", targetUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminAuth.getAccessToken())
                        .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetUserId))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        User targetAfterUpdate = userRepository.findById(targetUserId).orElseThrow();
        assertThat(targetAfterUpdate.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void nonAdminCannotChangeUsersRole() throws Exception {
        RegisterRequest firstUser = RegisterRequest.builder()
                .firstname("Andy")
                .lastname("Brennan")
                .email("andy@example.com")
                .username("andy@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();
        RegisterRequest secondUser = RegisterRequest.builder()
                .firstname("James")
                .lastname("Hurley")
                .email("james@example.com")
                .username("james@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();

        AuthenticationResponse firstAuth = registerViaHttp(firstUser);
        registerViaHttp(secondUser);

        Long secondUserId = userRepository.findByEmail(secondUser.getEmail()).orElseThrow().getId();
        UserRoleUpdateRequest roleRequest = UserRoleUpdateRequest.builder()
                .role(Role.ADMIN)
                .build();

        mockMvc.perform(patch("/api/v1/users/{userId}/role", secondUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + firstAuth.getAccessToken())
                        .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isForbidden());

        User untouched = userRepository.findById(secondUserId).orElseThrow();
        assertThat(untouched.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void userCanAddReadUpdateAndDeleteOwnProfileImage() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstname("Philip")
                .lastname("Jeffries")
                .email("philip@example.com")
                .username("philip@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();
        AuthenticationResponse auth = registerViaHttp(registerRequest);
        Long userId = userRepository.findByEmail(registerRequest.getEmail()).orElseThrow().getId();

        UserImageRequest createRequest = UserImageRequest.builder()
                .s3Key("users/" + userId + "/profile.png")
                .url("https://cdn.example.com/users/" + userId + "/profile.png")
                .altText("Profile")
                .mimeType("image/png")
                .build();

        mockMvc.perform(post("/api/v1/users/{userId}/profile-image", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + auth.getAccessToken())
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.s3Key").value("users/" + userId + "/profile.png"))
                .andExpect(jsonPath("$.role").value("MAIN"));

        mockMvc.perform(get("/api/v1/users/{userId}/profile-image", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId));

        UserImageRequest updateRequest = UserImageRequest.builder()
                .url("https://cdn.example.com/users/" + userId + "/profile-updated.png")
                .altText("Updated profile")
                .build();

        mockMvc.perform(put("/api/v1/users/{userId}/profile-image", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + auth.getAccessToken())
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.s3Key").value("users/" + userId + "/profile-updated.png"));

        mockMvc.perform(delete("/api/v1/users/{userId}/profile-image", userId)
                        .header("Authorization", "Bearer " + auth.getAccessToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/{userId}/profile-image", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotManageAnotherUsersProfileImage() throws Exception {
        RegisterRequest firstUser = RegisterRequest.builder()
                .firstname("Harry")
                .lastname("Truman")
                .email("harry@example.com")
                .username("harry@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();
        RegisterRequest secondUser = RegisterRequest.builder()
                .firstname("Albert")
                .lastname("Rosenfield")
                .email("albert@example.com")
                .username("albert@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();
        AuthenticationResponse firstAuth = registerViaHttp(firstUser);
        registerViaHttp(secondUser);
        Long secondUserId = userRepository.findByEmail(secondUser.getEmail()).orElseThrow().getId();

        UserImageRequest request = UserImageRequest.builder()
                .s3Key("users/" + secondUserId + "/profile.png")
                .url("https://cdn.example.com/users/" + secondUserId + "/profile.png")
                .build();

        mockMvc.perform(post("/api/v1/users/{userId}/profile-image", secondUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + firstAuth.getAccessToken())
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanManageAnotherUsersProfileImage() throws Exception {
        RegisterRequest adminRegister = RegisterRequest.builder()
                .firstname("Gordon")
                .lastname("Cole")
                .email("gordon@example.com")
                .username("gordon@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();
        RegisterRequest targetRegister = RegisterRequest.builder()
                .firstname("Diane")
                .lastname("Evans")
                .email("diane@example.com")
                .username("diane@example.com")
                .password("Password123")
                .repeatedPassword("Password123")
                .build();

        AuthenticationResponse adminAuth = registerViaHttp(adminRegister);
        registerViaHttp(targetRegister);

        User admin = userRepository.findByEmail(adminRegister.getEmail()).orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        Long targetUserId = userRepository.findByEmail(targetRegister.getEmail()).orElseThrow().getId();
        UserImageRequest request = UserImageRequest.builder()
                .s3Key("users/" + targetUserId + "/profile.png")
                .url("https://cdn.example.com/users/" + targetUserId + "/profile.png")
                .build();

        mockMvc.perform(post("/api/v1/users/{userId}/profile-image", targetUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminAuth.getAccessToken())
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(targetUserId));
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
