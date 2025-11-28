package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.models.dto.auth.ChangePasswordRequest;
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
}
