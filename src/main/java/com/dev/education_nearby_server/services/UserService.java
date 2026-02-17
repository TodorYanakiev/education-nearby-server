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
import com.dev.education_nearby_server.models.dto.response.UserImageResponse;
import com.dev.education_nearby_server.models.dto.response.UserResponse;
import com.dev.education_nearby_server.models.entity.UserImage;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.repositories.ReviewRepository;
import com.dev.education_nearby_server.repositories.TokenRepository;
import com.dev.education_nearby_server.repositories.UserImageRepository;
import com.dev.education_nearby_server.repositories.UserReviewRepository;
import com.dev.education_nearby_server.repositories.UserRepository;
import com.dev.education_nearby_server.utils.S3ImageLocationResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.List;

/**
 * User-facing operations for account maintenance.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository repository;
    private final UserReviewRepository userReviewRepository;
    private final ReviewRepository reviewRepository;
    private final TokenRepository tokenRepository;
    private final UserImageRepository userImageRepository;
    private final S3Properties s3Properties;
    private static final String NOT_FOUND = " not found.";
    private static final String PROFILE_IMAGE_MESSAGE = "You can only manage your own profile image.";
    private static final String PROFILE_IMAGE_USER = "Profile image for user ";

    /**
     * Returns users in the system as a paginated result. Intended for administrative dashboards.
     *
     * @param page zero-based page index
     * @param size page size
     * @return paged users with public fields only
     */
    public Page<UserResponse> getAllUsers(Integer page, Integer size) {
        validatePageRequest(page, size);
        Pageable pageable = PageRequest.of(page, size);
        return repository.findAll(pageable)
                .map(this::mapToResponse);
    }

    /**
     * Fetches a single user by id.
     *
     * @param userId user identifier
     * @return user with the given id
     */
    public UserResponse getUserById(Long userId) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User with id " + userId + NOT_FOUND));
        return mapToResponse(user);
    }

    /**
     * Fetches a single user by email.
     *
     * @param email user email
     * @return user with the given email
     */
    public UserResponse getUserByEmail(String email) {
        String normalizedEmail = trimToNull(email);
        if (!StringUtils.hasText(normalizedEmail)) {
            throw new BadRequestException("Email must not be blank.");
        }

        User user = repository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new NoSuchElementException("User with email " + normalizedEmail + NOT_FOUND));
        return mapToResponse(user);
    }

    /**
     * Returns the currently authenticated user.
     *
     * @param connectedUser authenticated principal
     * @return authenticated user representation
     */
    public UserResponse getAuthenticatedUser(Principal connectedUser) {
        User user = resolveUser(connectedUser);
        return mapToResponse(user);
    }

    /**
     * Updates mutable profile fields for a selected user.
     * Allowed for the user themself and global admins.
     *
     * @param userId target user identifier
     * @param request update payload
     * @param connectedUser authenticated principal performing the operation
     * @return updated user representation
     */
    public UserResponse updateUser(Long userId, UserUpdateRequest request, Principal connectedUser) {
        User targetUser = requireUser(userId);
        User actor = resolveUser(connectedUser);
        ensureCanManageUser(targetUser.getId(), actor, "You can only update your own profile.");

        String normalizedEmail = trimToNull(request.getEmail());
        String normalizedUsername = trimToNull(request.getUsername());
        if (!StringUtils.hasText(normalizedEmail)) {
            throw new ValidationException("The email should not be blank!");
        }
        if (!StringUtils.hasText(normalizedUsername)) {
            throw new ValidationException("The username should not be blank!");
        }

        ensureEmailNotTaken(normalizedEmail, targetUser.getId());
        ensureUsernameNotTaken(normalizedUsername, targetUser.getId());

        targetUser.setFirstname(trimToNull(request.getFirstname()));
        targetUser.setLastname(trimToNull(request.getLastname()));
        targetUser.setEmail(normalizedEmail);
        targetUser.setUsername(normalizedUsername);
        targetUser.setDescription(trimToNull(request.getDescription()));

        User saved = repository.save(targetUser);
        return mapToResponse(saved);
    }

    /**
     * Updates the role of a selected user.
     * Allowed for global admins only.
     *
     * @param userId target user identifier
     * @param request role update payload
     * @param connectedUser authenticated principal performing the operation
     * @return updated user representation
     */
    public UserResponse changeUserRole(Long userId, UserRoleUpdateRequest request, Principal connectedUser) {
        User actor = resolveUser(connectedUser);
        ensureGlobalAdmin(actor, "Only global admins can change user roles.");

        User targetUser = requireUser(userId);
        targetUser.setRole(request.getRole());

        User saved = repository.save(targetUser);
        return mapToResponse(saved);
    }

    /**
     * Deletes a user account and related auth/review records.
     * Allowed for the user themself and global admins.
     *
     * @param userId target user identifier
     * @param connectedUser authenticated principal performing the operation
     */
    @Transactional
    public void deleteUser(Long userId, Principal connectedUser) {
        User targetUser = requireUser(userId);
        User actor = resolveUser(connectedUser);
        ensureCanManageUser(targetUser.getId(), actor, "You can only delete your own profile.");

        tokenRepository.deleteAllByUser_Id(targetUser.getId());
        reviewRepository.deleteAllByUser_Id(targetUser.getId());
        repository.delete(targetUser);
    }

    /**
     * Changes the authenticated user's password after validating the current password
     * and new/confirmation match.
     *
     * @param request password change payload
     * @param connectedUser authenticated principal requesting the change
     */
    public void changePassword(ChangePasswordRequest request, Principal connectedUser) {
        User user = resolveUser(connectedUser);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ValidationException("Wrong password");
        }

        if (!request.getNewPassword().equals(request.getConfirmationPassword())) {
            throw new ValidationException("Password are not the same");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        repository.save(user);
    }

    /**
     * Returns the profile image metadata for the selected user.
     *
     * @param userId user identifier
     * @return profile image metadata
     */
    public UserImageResponse getUserProfileImage(Long userId) {
        UserImage image = userImageRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException(PROFILE_IMAGE_USER + userId + NOT_FOUND));
        return mapToImageResponse(image);
    }

    /**
     * Creates a profile image for a user. Allowed for the user themself and global admins.
     *
     * @param userId user identifier
     * @param request image payload
     * @param connectedUser authenticated principal performing the operation
     * @return persisted profile image metadata
     */
    public UserImageResponse addUserProfileImage(Long userId, UserImageRequest request, Principal connectedUser) {
        User targetUser = requireUser(userId);
        User actor = resolveUser(connectedUser);
        ensureCanManageUser(targetUser.getId(), actor, PROFILE_IMAGE_MESSAGE);

        if (userImageRepository.findByUserId(userId).isPresent()) {
            throw new ConflictException("User already has a profile image. Use update instead.");
        }

        S3ImageLocationResolver.ResolvedImageLocation resolvedLocation =
                S3ImageLocationResolver.resolveAndValidate(
                        request.getS3Key(),
                        request.getUrl(),
                        s3Properties.getUserAllowedPrefix(),
                        s3Properties
                );

        ensureUniqueS3Key(resolvedLocation.getS3Key(), null);

        UserImage image = new UserImage();
        image.setUser(targetUser);
        image.setS3Key(resolvedLocation.getS3Key());
        image.setUrl(resolvedLocation.getUrl());
        image.setRole(ImageRole.MAIN);
        image.setAltText(trimToNull(request.getAltText()));
        image.setWidth(request.getWidth());
        image.setHeight(request.getHeight());
        image.setMimeType(trimToNull(request.getMimeType()));
        image.setOrderIndex(0);

        UserImage saved = userImageRepository.save(image);
        targetUser.setProfileImage(saved);
        return mapToImageResponse(saved);
    }

    /**
     * Updates an existing profile image for a user. Allowed for the user themself and global admins.
     *
     * @param userId user identifier
     * @param request image payload
     * @param connectedUser authenticated principal performing the operation
     * @return updated profile image metadata
     */
    public UserImageResponse updateUserProfileImage(Long userId, UserImageRequest request, Principal connectedUser) {
        User targetUser = requireUser(userId);
        User actor = resolveUser(connectedUser);
        ensureCanManageUser(targetUser.getId(), actor, PROFILE_IMAGE_MESSAGE);

        UserImage image = userImageRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException(PROFILE_IMAGE_USER + userId + NOT_FOUND));

        S3ImageLocationResolver.ResolvedImageLocation resolvedLocation =
                S3ImageLocationResolver.resolveAndValidate(
                        request.getS3Key(),
                        request.getUrl(),
                        s3Properties.getUserAllowedPrefix(),
                        s3Properties
                );

        ensureUniqueS3Key(resolvedLocation.getS3Key(), image.getId());

        image.setS3Key(resolvedLocation.getS3Key());
        image.setUrl(resolvedLocation.getUrl());
        image.setRole(ImageRole.MAIN);
        image.setAltText(trimToNull(request.getAltText()));
        image.setWidth(request.getWidth());
        image.setHeight(request.getHeight());
        image.setMimeType(trimToNull(request.getMimeType()));
        image.setOrderIndex(0);

        UserImage saved = userImageRepository.save(image);
        return mapToImageResponse(saved);
    }

    /**
     * Deletes a user's profile image. Allowed for the user themself and global admins.
     *
     * @param userId user identifier
     * @param connectedUser authenticated principal performing the operation
     */
    public void deleteUserProfileImage(Long userId, Principal connectedUser) {
        User targetUser = requireUser(userId);
        User actor = resolveUser(connectedUser);
        ensureCanManageUser(targetUser.getId(), actor, PROFILE_IMAGE_MESSAGE);

        UserImage image = userImageRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException(PROFILE_IMAGE_USER + userId + NOT_FOUND));
        targetUser.setProfileImage(null);
        userImageRepository.delete(image);
    }

    private User resolveUser(Principal connectedUser) {
        if (!(connectedUser instanceof UsernamePasswordAuthenticationToken authToken)) {
            throw new UnauthorizedException("You must be authenticated to perform this action.");
        }
        Object principal = authToken.getPrincipal();
        if (!(principal instanceof User user)) {
            throw new UnauthorizedException("You must be authenticated to perform this action.");
        }
        return repository.findById(user.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found."));
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .email(user.getEmail())
                .username(user.getUsername())
                .description(user.getDescription())
                .role(user.getRole())
                .profileImage(user.getProfileImage() != null ? mapToImageResponse(user.getProfileImage()) : null)
                .administratedLyceumId(user.getAdministratedLyceum() != null ? user.getAdministratedLyceum().getId() : null)
                .lecturedCourseIds(extractLecturedCourseIds(user))
                .lecturedLyceumIds(extractLecturedLyceumIds(user))
                .enabled(user.isEnabled())
                .averageRating(userReviewRepository.findAverageRatingByReviewedUserId(user.getId()))
                .build();
    }

    private void validatePageRequest(Integer page, Integer size) {
        if (page == null || page < 0) {
            throw new BadRequestException("Page index must be zero or positive.");
        }
        if (size == null || size <= 0) {
            throw new BadRequestException("Page size must be greater than zero.");
        }
    }

    private User requireUser(Long userId) {
        return repository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User with id " + userId + NOT_FOUND));
    }

    private void ensureCanManageUser(Long targetUserId, User actor, String message) {
        boolean isAdmin = actor.getRole() == Role.ADMIN;
        boolean isSelf = actor.getId() != null && actor.getId().equals(targetUserId);
        if (!isAdmin && !isSelf) {
            throw new AccessDeniedException(message);
        }
    }

    private void ensureGlobalAdmin(User actor, String message) {
        if (actor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException(message);
        }
    }

    private void ensureEmailNotTaken(String email, Long currentUserId) {
        repository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            if (!existing.getId().equals(currentUserId)) {
                throw new ConflictException("User with such email already exists!");
            }
        });
    }

    private void ensureUsernameNotTaken(String username, Long currentUserId) {
        repository.findByUsername(username).ifPresent(existing -> {
            if (!existing.getId().equals(currentUserId)) {
                throw new ConflictException("User with such username already exists!");
            }
        });
    }

    private void ensureUniqueS3Key(String s3Key, Long currentImageId) {
        userImageRepository.findByS3Key(s3Key).ifPresent(existing -> {
            if (currentImageId == null || !existing.getId().equals(currentImageId)) {
                throw new ConflictException("An image with the same S3 key is already registered.");
            }
        });
    }

    private UserImageResponse mapToImageResponse(UserImage image) {
        return UserImageResponse.builder()
                .id(image.getId())
                .userId(image.getUser() != null ? image.getUser().getId() : null)
                .s3Key(image.getS3Key())
                .url(image.getUrl())
                .role(image.getRole())
                .altText(image.getAltText())
                .width(image.getWidth())
                .height(image.getHeight())
                .mimeType(image.getMimeType())
                .orderIndex(image.getOrderIndex())
                .build();
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? null : value.trim();
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }

    private List<Long> extractLecturedCourseIds(User user) {
        if (user.getCoursesLectured() == null || user.getCoursesLectured().isEmpty()) {
            return List.of();
        }
        return user.getCoursesLectured().stream()
                .map(course -> course.getId())
                .toList();
    }

    private List<Long> extractLecturedLyceumIds(User user) {
        if (user.getLecturedLyceums() == null || user.getLecturedLyceums().isEmpty()) {
            return List.of();
        }
        return user.getLecturedLyceums().stream()
                .map(lyceum -> lyceum.getId())
                .toList();
    }
}
