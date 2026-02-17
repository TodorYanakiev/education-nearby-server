package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.config.S3Properties;
import com.dev.education_nearby_server.enums.ImageRole;
import com.dev.education_nearby_server.enums.Role;
import com.dev.education_nearby_server.exceptions.common.AccessDeniedException;
import com.dev.education_nearby_server.exceptions.common.BadRequestException;
import com.dev.education_nearby_server.exceptions.common.ConflictException;
import com.dev.education_nearby_server.exceptions.common.NoSuchElementException;
import com.dev.education_nearby_server.exceptions.common.UnauthorizedException;
import com.dev.education_nearby_server.exceptions.common.ValidationException;
import com.dev.education_nearby_server.models.dto.auth.ChangePasswordRequest;
import com.dev.education_nearby_server.models.dto.request.UserImageRequest;
import com.dev.education_nearby_server.models.dto.request.UserRoleUpdateRequest;
import com.dev.education_nearby_server.models.dto.request.UserUpdateRequest;
import com.dev.education_nearby_server.models.dto.response.UserResponse;
import com.dev.education_nearby_server.models.entity.Course;
import com.dev.education_nearby_server.models.entity.Lyceum;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.models.entity.UserImage;
import com.dev.education_nearby_server.repositories.ReviewRepository;
import com.dev.education_nearby_server.repositories.TokenRepository;
import com.dev.education_nearby_server.repositories.UserImageRepository;
import com.dev.education_nearby_server.repositories.UserReviewRepository;
import com.dev.education_nearby_server.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
    private ReviewRepository reviewRepository;
    @Mock
    private TokenRepository tokenRepository;
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
    void getAllUsersReturnsMappedPage() {
        User first = User.builder()
                .id(1L)
                .firstname("First")
                .lastname("User")
                .email("first@example.com")
                .username("first")
                .role(Role.USER)
                .enabled(true)
                .build();
        User second = User.builder()
                .id(2L)
                .firstname("Second")
                .lastname("User")
                .email("second@example.com")
                .username("second")
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        Page<User> users = new PageImpl<>(List.of(first, second), PageRequest.of(1, 2), 5);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(users);
        when(userReviewRepository.findAverageRatingByReviewedUserId(1L)).thenReturn(4.8);
        when(userReviewRepository.findAverageRatingByReviewedUserId(2L)).thenReturn(3.9);

        Page<UserResponse> result = userService.getAllUsers(1, 2);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(2);

        assertThat(result.getContent()).extracting(UserResponse::getEmail)
                .containsExactly("first@example.com", "second@example.com");
        assertThat(result.getContent()).extracting(UserResponse::getAverageRating)
                .containsExactly(4.8, 3.9);
        assertThat(result.getTotalElements()).isEqualTo(5);
    }

    @Test
    void getAllUsersThrowsWhenPageIsNegative() {
        assertThrows(BadRequestException.class, () -> userService.getAllUsers(-1, 10));

        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getAllUsersThrowsWhenSizeIsNotPositive() {
        assertThrows(BadRequestException.class, () -> userService.getAllUsers(0, 0));

        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getUserByEmailReturnsMappedUser() {
        User user = User.builder()
                .id(6L)
                .firstname("Annie")
                .lastname("Blackburn")
                .email("annie@example.com")
                .username("annie")
                .role(Role.USER)
                .enabled(true)
                .build();
        when(userRepository.findByEmailIgnoreCase("annie@example.com")).thenReturn(Optional.of(user));
        when(userReviewRepository.findAverageRatingByReviewedUserId(6L)).thenReturn(4.3);

        UserResponse response = userService.getUserByEmail("  annie@example.com  ");

        assertThat(response.getId()).isEqualTo(6L);
        assertThat(response.getEmail()).isEqualTo("annie@example.com");
        assertThat(response.getAverageRating()).isEqualTo(4.3);
    }

    @Test
    void getUserByEmailThrowsWhenEmailIsBlank() {
        assertThrows(BadRequestException.class, () -> userService.getUserByEmail("   "));

        verify(userRepository, never()).findByEmailIgnoreCase(any());
    }

    @Test
    void getUserByEmailThrowsWhenUserIsMissing() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> userService.getUserByEmail("missing@example.com"));
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
    void updateUserUpdatesSelfProfile() {
        User user = User.builder()
                .id(11L)
                .firstname("Old")
                .lastname("Name")
                .email("old@example.com")
                .username("old-user")
                .description("Old description")
                .role(Role.USER)
                .build();
        Principal principal = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        UserUpdateRequest request = UserUpdateRequest.builder()
                .firstname(" New ")
                .lastname(" Person ")
                .email("new@example.com")
                .username("new-user")
                .description(" Updated ")
                .build();

        when(userRepository.findById(11L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("new-user")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = userService.updateUser(11L, request, principal);

        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getFirstname()).isEqualTo("New");
        assertThat(response.getLastname()).isEqualTo("Person");
        assertThat(response.getEmail()).isEqualTo("new@example.com");
        assertThat(response.getUsername()).isEqualTo("new-user");
        assertThat(response.getDescription()).isEqualTo("Updated");
        verify(userRepository).save(user);
    }

    @Test
    void updateUserAllowsAdminToUpdateAnotherUser() {
        User target = User.builder()
                .id(12L)
                .firstname("Target")
                .lastname("User")
                .email("target@example.com")
                .username("target-user")
                .role(Role.USER)
                .build();
        User admin = User.builder().id(1L).role(Role.ADMIN).build();
        Principal principal = new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities());
        UserUpdateRequest request = UserUpdateRequest.builder()
                .firstname("Updated")
                .lastname("Target")
                .email("updated@example.com")
                .username("updated-user")
                .description("Admin updated")
                .build();

        when(userRepository.findById(12L)).thenReturn(Optional.of(target));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findByEmailIgnoreCase("updated@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("updated-user")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = userService.updateUser(12L, request, principal);

        assertThat(response.getId()).isEqualTo(12L);
        assertThat(response.getEmail()).isEqualTo("updated@example.com");
        assertThat(response.getUsername()).isEqualTo("updated-user");
        assertThat(response.getDescription()).isEqualTo("Admin updated");
    }

    @Test
    void updateUserThrowsWhenNonAdminUpdatesAnotherUser() {
        User target = User.builder().id(13L).role(Role.USER).build();
        User actor = User.builder().id(99L).role(Role.USER).build();
        Principal principal = new UsernamePasswordAuthenticationToken(actor, null, actor.getAuthorities());
        UserUpdateRequest request = UserUpdateRequest.builder()
                .firstname("Any")
                .lastname("Name")
                .email("any@example.com")
                .username("any-user")
                .build();

        when(userRepository.findById(13L)).thenReturn(Optional.of(target));
        when(userRepository.findById(99L)).thenReturn(Optional.of(actor));

        assertThrows(AccessDeniedException.class, () -> userService.updateUser(13L, request, principal));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserThrowsWhenEmailAlreadyTaken() {
        User target = User.builder().id(14L).role(Role.USER).build();
        User duplicate = User.builder().id(77L).role(Role.USER).build();
        Principal principal = new UsernamePasswordAuthenticationToken(target, null, target.getAuthorities());
        UserUpdateRequest request = UserUpdateRequest.builder()
                .firstname("Any")
                .lastname("Name")
                .email("taken@example.com")
                .username("new-user")
                .build();

        when(userRepository.findById(14L)).thenReturn(Optional.of(target));
        when(userRepository.findByEmailIgnoreCase("taken@example.com")).thenReturn(Optional.of(duplicate));

        assertThrows(ConflictException.class, () -> userService.updateUser(14L, request, principal));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changeUserRoleAllowsGlobalAdmin() {
        User admin = User.builder().id(1L).role(Role.ADMIN).build();
        User target = User.builder()
                .id(22L)
                .email("target@example.com")
                .username("target")
                .role(Role.USER)
                .enabled(true)
                .build();
        Principal principal = new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities());
        UserRoleUpdateRequest request = UserRoleUpdateRequest.builder()
                .role(Role.ADMIN)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(22L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.changeUserRole(22L, request, principal);

        assertThat(target.getRole()).isEqualTo(Role.ADMIN);
        assertThat(response.getRole()).isEqualTo(Role.ADMIN);
        verify(userRepository).save(target);
    }

    @Test
    void changeUserRoleThrowsWhenActorIsNotGlobalAdmin() {
        User actor = User.builder().id(2L).role(Role.USER).build();
        Principal principal = new UsernamePasswordAuthenticationToken(actor, null, actor.getAuthorities());
        UserRoleUpdateRequest request = UserRoleUpdateRequest.builder()
                .role(Role.ADMIN)
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(actor));

        assertThrows(AccessDeniedException.class, () -> userService.changeUserRole(22L, request, principal));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteUserDeletesSelfAndRelatedRecords() {
        User user = User.builder().id(30L).role(Role.USER).build();
        Principal principal = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        when(userRepository.findById(30L)).thenReturn(Optional.of(user));

        userService.deleteUser(30L, principal);

        verify(tokenRepository).deleteAllByUser_Id(30L);
        verify(reviewRepository).deleteAllByUser_Id(30L);
        verify(userRepository).delete(user);
    }

    @Test
    void deleteUserAllowsAdminToDeleteAnotherUser() {
        User target = User.builder().id(31L).role(Role.USER).build();
        User admin = User.builder().id(2L).role(Role.ADMIN).build();
        Principal principal = new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities());

        when(userRepository.findById(31L)).thenReturn(Optional.of(target));
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));

        userService.deleteUser(31L, principal);

        verify(tokenRepository).deleteAllByUser_Id(31L);
        verify(reviewRepository).deleteAllByUser_Id(31L);
        verify(userRepository).delete(target);
    }

    @Test
    void deleteUserThrowsWhenNonAdminDeletesAnotherUser() {
        User target = User.builder().id(32L).role(Role.USER).build();
        User actor = User.builder().id(3L).role(Role.USER).build();
        Principal principal = new UsernamePasswordAuthenticationToken(actor, null, actor.getAuthorities());

        when(userRepository.findById(32L)).thenReturn(Optional.of(target));
        when(userRepository.findById(3L)).thenReturn(Optional.of(actor));

        assertThrows(AccessDeniedException.class, () -> userService.deleteUser(32L, principal));
        verify(tokenRepository, never()).deleteAllByUser_Id(anyLong());
        verify(reviewRepository, never()).deleteAllByUser_Id(anyLong());
        verify(userRepository, never()).delete(any(User.class));
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
