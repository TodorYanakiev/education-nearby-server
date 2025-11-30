package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.enums.Role;
import com.dev.education_nearby_server.exceptions.common.ValidationException;
import com.dev.education_nearby_server.models.dto.auth.ChangePasswordRequest;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.Principal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void changePasswordThrowsWhenCurrentPasswordInvalid() {
        User user = User.builder()
                .id(1L)
                .password("encoded")
                .role(Role.USER)
                .build();
        Principal principal = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("old")
                .newPassword("newPassword123")
                .confirmationPassword("newPassword123")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "encoded")).thenReturn(false);

        assertThrows(ValidationException.class, () -> userService.changePassword(request, principal));
    }

    @Test
    void changePasswordThrowsWhenConfirmationDiffers() {
        User user = User.builder()
                .id(1L)
                .password("encoded")
                .role(Role.USER)
                .build();
        Principal principal = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("old")
                .newPassword("newPassword123")
                .confirmationPassword("different")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "encoded")).thenReturn(true);

        assertThrows(ValidationException.class, () -> userService.changePassword(request, principal));
    }

    @Test
    void changePasswordEncodesAndPersists() {
        User user = User.builder()
                .id(1L)
                .password("encoded")
                .role(Role.USER)
                .build();
        Principal principal = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("old")
                .newPassword("newPassword123")
                .confirmationPassword("newPassword123")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "encoded")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-encoded");

        userService.changePassword(request, principal);

        assertThat(user.getPassword()).isEqualTo("new-encoded");
        verify(userRepository).save(any(User.class));
    }
}
