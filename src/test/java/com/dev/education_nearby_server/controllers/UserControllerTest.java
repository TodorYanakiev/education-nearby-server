package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.models.dto.auth.ChangePasswordRequest;
import com.dev.education_nearby_server.models.dto.request.UserImageRequest;
import com.dev.education_nearby_server.models.dto.request.UserUpdateRequest;
import com.dev.education_nearby_server.models.dto.response.UserImageResponse;
import com.dev.education_nearby_server.models.dto.response.UserResponse;
import com.dev.education_nearby_server.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    void getAllUsersReturnsServicePayload() {
        List<UserResponse> users = List.of(
                UserResponse.builder().id(1L).email("one@example.com").build(),
                UserResponse.builder().id(2L).email("two@example.com").build()
        );
        when(userService.getAllUsers()).thenReturn(users);

        ResponseEntity<List<UserResponse>> response = userController.getAllUsers();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactlyElementsOf(users);
        verify(userService).getAllUsers();
    }

    @Test
    void getUserByIdReturnsServicePayload() {
        UserResponse user = UserResponse.builder().id(10L).email("user@example.com").build();
        when(userService.getUserById(10L)).thenReturn(user);

        ResponseEntity<UserResponse> response = userController.getUserById(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(user);
        verify(userService).getUserById(10L);
    }

    @Test
    void updateUserReturnsServicePayload() {
        UserUpdateRequest request = UserUpdateRequest.builder()
                .firstname("New")
                .lastname("Name")
                .email("new@example.com")
                .username("new-user")
                .description("Updated")
                .build();
        UserResponse responseBody = UserResponse.builder()
                .id(10L)
                .firstname("New")
                .lastname("Name")
                .email("new@example.com")
                .username("new-user")
                .description("Updated")
                .build();
        Principal principal = mock(Principal.class);
        when(userService.updateUser(10L, request, principal)).thenReturn(responseBody);

        ResponseEntity<UserResponse> response = userController.updateUser(10L, request, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(responseBody);
        verify(userService).updateUser(10L, request, principal);
    }

    @Test
    void deleteUserReturnsNoContent() {
        Principal principal = mock(Principal.class);

        ResponseEntity<Void> response = userController.deleteUser(10L, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userService).deleteUser(10L, principal);
    }

    @Test
    void getUserProfileImageReturnsServicePayload() {
        UserImageResponse image = UserImageResponse.builder()
                .id(44L)
                .userId(10L)
                .url("https://cdn.example.com/users/10/profile.png")
                .build();
        when(userService.getUserProfileImage(10L)).thenReturn(image);

        ResponseEntity<UserImageResponse> response = userController.getUserProfileImage(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(image);
        verify(userService).getUserProfileImage(10L);
    }

    @Test
    void getAuthenticatedUserReturnsServicePayload() {
        UserResponse user = UserResponse.builder().id(5L).email("me@example.com").build();
        Principal principal = mock(Principal.class);
        when(userService.getAuthenticatedUser(principal)).thenReturn(user);

        ResponseEntity<UserResponse> response = userController.getAuthenticatedUser(principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(user);
        verify(userService).getAuthenticatedUser(principal);
    }

    @Test
    void changePasswordReturnsOk() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("old")
                .newPassword("newPassword123")
                .confirmationPassword("newPassword123")
                .build();
        Principal principal = mock(Principal.class);

        ResponseEntity<?> response = userController.changePassword(request, principal);

        verify(userService).changePassword(request, principal);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void addUserProfileImageReturnsCreatedResponse() {
        UserImageRequest request = UserImageRequest.builder()
                .s3Key("users/10/profile.png")
                .build();
        UserImageResponse image = UserImageResponse.builder()
                .id(11L)
                .userId(10L)
                .s3Key("users/10/profile.png")
                .url("https://cdn.example.com/users/10/profile.png")
                .build();
        Principal principal = mock(Principal.class);
        when(userService.addUserProfileImage(10L, request, principal)).thenReturn(image);

        ResponseEntity<UserImageResponse> response = userController.addUserProfileImage(10L, request, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(image);
        verify(userService).addUserProfileImage(10L, request, principal);
    }

    @Test
    void updateUserProfileImageReturnsUpdatedResponse() {
        UserImageRequest request = UserImageRequest.builder()
                .url("https://cdn.example.com/users/10/profile-new.png")
                .build();
        UserImageResponse image = UserImageResponse.builder()
                .id(11L)
                .userId(10L)
                .url("https://cdn.example.com/users/10/profile-new.png")
                .build();
        Principal principal = mock(Principal.class);
        when(userService.updateUserProfileImage(10L, request, principal)).thenReturn(image);

        ResponseEntity<UserImageResponse> response = userController.updateUserProfileImage(10L, request, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(image);
        verify(userService).updateUserProfileImage(10L, request, principal);
    }

    @Test
    void deleteUserProfileImageReturnsNoContent() {
        Principal principal = mock(Principal.class);

        ResponseEntity<Void> response = userController.deleteUserProfileImage(10L, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userService).deleteUserProfileImage(10L, principal);
    }
}
