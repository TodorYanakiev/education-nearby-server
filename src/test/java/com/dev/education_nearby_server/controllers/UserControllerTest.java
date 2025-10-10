package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.models.dto.auth.ChangePasswordRequest;
import com.dev.education_nearby_server.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

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
