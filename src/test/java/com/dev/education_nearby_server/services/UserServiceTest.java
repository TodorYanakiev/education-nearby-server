package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.config.S3Properties;
import com.dev.education_nearby_server.enums.ImageRole;
import com.dev.education_nearby_server.enums.Role;
import com.dev.education_nearby_server.exceptions.common.AccessDeniedException;
import com.dev.education_nearby_server.exceptions.common.ConflictException;
import com.dev.education_nearby_server.exceptions.common.NoSuchElementException;
import com.dev.education_nearby_server.exceptions.common.UnauthorizedException;
import com.dev.education_nearby_server.exceptions.common.ValidationException;
import com.dev.education_nearby_server.models.dto.auth.ChangePasswordRequest;
import com.dev.education_nearby_server.models.dto.request.UserImageRequest;
import com.dev.education_nearby_server.models.entity.Course;
import com.dev.education_nearby_server.models.entity.Lyceum;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.models.entity.UserImage;
import com.dev.education_nearby_server.repositories.UserImageRepository;
import com.dev.education_nearby_server.repositories.UserReviewRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserReviewRepository userReviewRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserImageRepository userImageRepository;
    @Mock
    private S3Properties s3Properties;

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
                .description("Administrator")
                .role(Role.ADMIN)
                .administratedLyceum(lyceum)
                .enabled(true)
                .build();
        Principal principal = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userReviewRepository.findAverageRatingByReviewedUserId(5L)).thenReturn(4.1);

        var response = userService.getAuthenticatedUser(principal);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getFirstname()).isEqualTo("Dale");
        assertThat(response.getLastname()).isEqualTo("Cooper");
        assertThat(response.getEmail()).isEqualTo("dale@example.com");
        assertThat(response.getUsername()).isEqualTo("dale@example.com");
        assertThat(response.getDescription()).isEqualTo("Administrator");
        assertThat(response.getRole()).isEqualTo(Role.ADMIN);
        assertThat(response.getAdministratedLyceumId()).isEqualTo(99L);
        assertThat(response.getLecturedCourseIds()).isEmpty();
        assertThat(response.getLecturedLyceumIds()).isEmpty();
        assertThat(response.getProfileImage()).isNull();
        assertThat(response.isEnabled()).isTrue();
        assertThat(response.getAverageRating()).isEqualTo(4.1);
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

    @Test
    void getUserProfileImageReturnsMappedImage() {
        User user = User.builder().id(11L).build();
        UserImage image = buildUserImage(50L, user, "users/11/profile.png", "https://cdn.example.com/users/11/profile.png");
        when(userImageRepository.findByUserId(11L)).thenReturn(Optional.of(image));

        var response = userService.getUserProfileImage(11L);

        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getUserId()).isEqualTo(11L);
        assertThat(response.getS3Key()).isEqualTo("users/11/profile.png");
        assertThat(response.getRole()).isEqualTo(ImageRole.MAIN);
    }

    @Test
    void addUserProfileImageCreatesImageForSelf() {
        User target = User.builder().id(20L).role(Role.USER).build();
        Principal principal = new UsernamePasswordAuthenticationToken(target, null, target.getAuthorities());
        UserImageRequest request = UserImageRequest.builder()
                .s3Key("users/20/profile.png")
                .altText(" Profile ")
                .mimeType(" image/png ")
                .build();

        when(userRepository.findById(20L)).thenReturn(Optional.of(target));
        when(s3Properties.getUserAllowedPrefix()).thenReturn("users/");
        when(s3Properties.getPublicBaseUrl()).thenReturn("https://cdn.example.com/");
        when(userImageRepository.findByUserId(20L)).thenReturn(Optional.empty());
        when(userImageRepository.findByS3Key("users/20/profile.png")).thenReturn(Optional.empty());
        when(userImageRepository.save(any(UserImage.class))).thenAnswer(invocation -> {
            UserImage image = invocation.getArgument(0);
            image.setId(201L);
            return image;
        });

        var response = userService.addUserProfileImage(20L, request, principal);

        assertThat(response.getId()).isEqualTo(201L);
        assertThat(response.getUserId()).isEqualTo(20L);
        assertThat(response.getRole()).isEqualTo(ImageRole.MAIN);
        assertThat(response.getOrderIndex()).isEqualTo(0);
        assertThat(response.getAltText()).isEqualTo("Profile");
        assertThat(response.getMimeType()).isEqualTo("image/png");
        verify(userImageRepository).save(any(UserImage.class));
    }

    @Test
    void addUserProfileImageAllowsAdminForOtherUser() {
        User target = User.builder().id(25L).role(Role.USER).build();
        User admin = User.builder().id(2L).role(Role.ADMIN).build();
        Principal principal = new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities());
        UserImageRequest request = UserImageRequest.builder()
                .s3Key("users/25/profile.png")
                .build();

        when(userRepository.findById(25L)).thenReturn(Optional.of(target));
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(s3Properties.getUserAllowedPrefix()).thenReturn("users/");
        when(s3Properties.getPublicBaseUrl()).thenReturn("https://cdn.example.com");
        when(userImageRepository.findByUserId(25L)).thenReturn(Optional.empty());
        when(userImageRepository.findByS3Key("users/25/profile.png")).thenReturn(Optional.empty());
        when(userImageRepository.save(any(UserImage.class))).thenAnswer(invocation -> {
            UserImage image = invocation.getArgument(0);
            image.setId(301L);
            return image;
        });

        var response = userService.addUserProfileImage(25L, request, principal);

        assertThat(response.getId()).isEqualTo(301L);
        assertThat(response.getUserId()).isEqualTo(25L);
    }

    @Test
    void addUserProfileImageThrowsWhenManagingAnotherUserWithoutAdminRole() {
        User target = User.builder().id(22L).role(Role.USER).build();
        User actor = User.builder().id(99L).role(Role.USER).build();
        Principal principal = new UsernamePasswordAuthenticationToken(actor, null, actor.getAuthorities());
        UserImageRequest request = UserImageRequest.builder()
                .s3Key("users/22/profile.png")
                .build();

        when(userRepository.findById(22L)).thenReturn(Optional.of(target));
        when(userRepository.findById(99L)).thenReturn(Optional.of(actor));

        assertThrows(AccessDeniedException.class, () -> userService.addUserProfileImage(22L, request, principal));
        verifyNoInteractions(userImageRepository);
    }

    @Test
    void addUserProfileImageThrowsWhenImageAlreadyExists() {
        User target = User.builder().id(24L).role(Role.USER).build();
        Principal principal = new UsernamePasswordAuthenticationToken(target, null, target.getAuthorities());
        UserImageRequest request = UserImageRequest.builder()
                .s3Key("users/24/profile.png")
                .build();

        when(userRepository.findById(24L)).thenReturn(Optional.of(target));
        when(userImageRepository.findByUserId(24L)).thenReturn(Optional.of(new UserImage()));

        assertThrows(ConflictException.class, () -> userService.addUserProfileImage(24L, request, principal));
        verify(userImageRepository, never()).save(any(UserImage.class));
    }

    @Test
    void updateUserProfileImageUpdatesCurrentImage() {
        User target = User.builder().id(31L).role(Role.USER).build();
        Principal principal = new UsernamePasswordAuthenticationToken(target, null, target.getAuthorities());
        UserImage image = buildUserImage(99L, target, "users/31/old.png", "https://cdn.example.com/users/31/old.png");
        UserImageRequest request = UserImageRequest.builder()
                .url("https://cdn.example.com/users/31/new.png")
                .altText(" New alt ")
                .build();

        when(userRepository.findById(31L)).thenReturn(Optional.of(target));
        when(s3Properties.getUserAllowedPrefix()).thenReturn("users/");
        when(userImageRepository.findByUserId(31L)).thenReturn(Optional.of(image));
        when(userImageRepository.findByS3Key("users/31/new.png")).thenReturn(Optional.empty());
        when(userImageRepository.save(any(UserImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = userService.updateUserProfileImage(31L, request, principal);

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getS3Key()).isEqualTo("users/31/new.png");
        assertThat(response.getAltText()).isEqualTo("New alt");
    }

    @Test
    void updateUserProfileImageThrowsWhenNotFound() {
        User target = User.builder().id(36L).role(Role.USER).build();
        Principal principal = new UsernamePasswordAuthenticationToken(target, null, target.getAuthorities());
        UserImageRequest request = UserImageRequest.builder().s3Key("users/36/profile.png").build();

        when(userRepository.findById(36L)).thenReturn(Optional.of(target));
        when(userImageRepository.findByUserId(36L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> userService.updateUserProfileImage(36L, request, principal));
    }

    @Test
    void updateUserProfileImageThrowsWhenS3KeyAlreadyTakenByAnotherImage() {
        User target = User.builder().id(37L).role(Role.USER).build();
        Principal principal = new UsernamePasswordAuthenticationToken(target, null, target.getAuthorities());
        UserImage current = buildUserImage(7L, target, "users/37/current.png", "https://cdn.example.com/users/37/current.png");
        User anotherUser = User.builder().id(38L).role(Role.USER).build();
        UserImage another = buildUserImage(8L, anotherUser, "users/shared.png", "https://cdn.example.com/users/shared.png");
        UserImageRequest request = UserImageRequest.builder().s3Key("users/shared.png").build();

        when(userRepository.findById(37L)).thenReturn(Optional.of(target));
        when(s3Properties.getUserAllowedPrefix()).thenReturn("users/");
        when(s3Properties.getPublicBaseUrl()).thenReturn("https://cdn.example.com");
        when(userImageRepository.findByUserId(37L)).thenReturn(Optional.of(current));
        when(userImageRepository.findByS3Key("users/shared.png")).thenReturn(Optional.of(another));

        assertThrows(ConflictException.class, () -> userService.updateUserProfileImage(37L, request, principal));
        verify(userImageRepository, never()).save(any(UserImage.class));
    }

    @Test
    void deleteUserProfileImageRemovesImage() {
        User target = User.builder().id(41L).role(Role.USER).build();
        Principal principal = new UsernamePasswordAuthenticationToken(target, null, target.getAuthorities());
        UserImage image = buildUserImage(410L, target, "users/41/profile.png", "https://cdn.example.com/users/41/profile.png");

        when(userRepository.findById(41L)).thenReturn(Optional.of(target));
        when(userImageRepository.findByUserId(41L)).thenReturn(Optional.of(image));

        userService.deleteUserProfileImage(41L, principal);

        verify(userImageRepository).delete(eq(image));
    }

    private UserImage buildUserImage(Long id, User user, String s3Key, String url) {
        UserImage image = new UserImage();
        image.setId(id);
        image.setUser(user);
        image.setS3Key(s3Key);
        image.setUrl(url);
        image.setRole(ImageRole.MAIN);
        image.setOrderIndex(0);
        return image;
    }
}
