package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.enums.Role;
import com.dev.education_nearby_server.exceptions.common.UnauthorizedException;
import com.dev.education_nearby_server.exceptions.common.ValidationException;
import com.dev.education_nearby_server.models.dto.auth.ChangePasswordRequest;
import com.dev.education_nearby_server.models.entity.Course;
import com.dev.education_nearby_server.models.entity.Lyceum;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
    void changePasswordThrowsWhenPrincipalNotAuthenticationToken() {
        Principal principal = () -> "anonymous";
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("old")
                .newPassword("newPassword123")
                .confirmationPassword("newPassword123")
                .build();

        assertThrows(UnauthorizedException.class, () -> userService.changePassword(request, principal));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePasswordThrowsWhenPrincipalIsNotUser() {
        Principal principal = new UsernamePasswordAuthenticationToken("rawPrincipal", null, List.of());
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("old")
                .newPassword("newPassword123")
                .confirmationPassword("newPassword123")
                .build();

        assertThrows(UnauthorizedException.class, () -> userService.changePassword(request, principal));
        verify(userRepository, never()).save(any(User.class));
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

    @Test
    void getAuthenticatedUserReturnsMappedUser() {
        Lyceum lyceum = new Lyceum();
        lyceum.setId(99L);
        User user = User.builder()
                .id(5L)
                .firstname("Dale")
                .lastname("Cooper")
                .email("dale@example.com")
                .username("dale@example.com")
                .role(Role.ADMIN)
                .administratedLyceum(lyceum)
                .enabled(true)
                .build();
        Principal principal = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        var response = userService.getAuthenticatedUser(principal);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getFirstname()).isEqualTo("Dale");
        assertThat(response.getLastname()).isEqualTo("Cooper");
        assertThat(response.getEmail()).isEqualTo("dale@example.com");
        assertThat(response.getUsername()).isEqualTo("dale@example.com");
        assertThat(response.getRole()).isEqualTo(Role.ADMIN);
        assertThat(response.getAdministratedLyceumId()).isEqualTo(99L);
        assertThat(response.getLecturedCourseIds()).isEmpty();
        assertThat(response.getLecturedLyceumIds()).isEmpty();
        assertThat(response.isEnabled()).isTrue();
    }

    @Test
    void getAuthenticatedUserMapsLecturedIds() {
        Course course = new Course();
        course.setId(33L);
        Lyceum lyceum = new Lyceum();
        lyceum.setId(44L);
        User user = User.builder()
                .id(15L)
                .firstname("Audrey")
                .lastname("Horne")
                .email("audrey@example.com")
                .username("audrey@example.com")
                .role(Role.USER)
                .enabled(true)
                .build();
        user.setCoursesLectured(List.of(course));
        user.setLecturedLyceums(List.of(lyceum));
        Principal principal = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        when(userRepository.findById(15L)).thenReturn(Optional.of(user));

        var response = userService.getAuthenticatedUser(principal);

        assertThat(response.getLecturedCourseIds()).containsExactly(33L);
        assertThat(response.getLecturedLyceumIds()).containsExactly(44L);
    }

    @Test
    void getAuthenticatedUserMapsEmptyLecturedIdsWhenNullLists() {
        User user = User.builder()
                .id(18L)
                .firstname("Laura")
                .lastname("Palmer")
                .email("laura@example.com")
                .username("laura@example.com")
                .role(Role.USER)
                .enabled(true)
                .build();
        user.setCoursesLectured(null);
        user.setLecturedLyceums(null);
        Principal principal = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        when(userRepository.findById(18L)).thenReturn(Optional.of(user));

        var response = userService.getAuthenticatedUser(principal);

        assertThat(response.getLecturedCourseIds()).isEmpty();
        assertThat(response.getLecturedLyceumIds()).isEmpty();
    }

    @Test
    void getAuthenticatedUserThrowsWhenUserNotFoundInRepository() {
        User user = User.builder()
                .id(7L)
                .role(Role.USER)
                .build();
        Principal principal = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> userService.getAuthenticatedUser(principal));
    }
}
