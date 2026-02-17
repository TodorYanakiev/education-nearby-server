package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.models.dto.auth.ChangePasswordRequest;
import com.dev.education_nearby_server.models.dto.request.ReviewRequest;
import com.dev.education_nearby_server.models.dto.request.ReviewUpdateRequest;
import com.dev.education_nearby_server.models.dto.request.UserImageRequest;
import com.dev.education_nearby_server.models.dto.request.UserRoleUpdateRequest;
import com.dev.education_nearby_server.models.dto.request.UserUpdateRequest;
import com.dev.education_nearby_server.models.dto.response.ReviewResponse;
import com.dev.education_nearby_server.models.dto.response.UserImageResponse;
import com.dev.education_nearby_server.models.dto.response.UserResponse;
import com.dev.education_nearby_server.services.ReviewService;
import com.dev.education_nearby_server.services.UserService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * User-facing endpoints for account management.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;
    private final ReviewService reviewService;

    /**
     * Lists users with pagination.
     *
     * @param page zero-based page index
     * @param size page size
     * @return paginated public user representations
     */
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "9") Integer size
    ) {
        return ResponseEntity.ok(service.getAllUsers(page, size));
    }

    /**
     * Fetches a single user by email.
     *
     * @param email user email
     * @return user representation
     */
    @GetMapping("/by-email")
    public ResponseEntity<UserResponse> getUserByEmail(@RequestParam String email) {
        return ResponseEntity.ok(service.getUserByEmail(email));
    }

    /**
     * Fetches a single user by id.
     *
     * @param userId user identifier
     * @return user representation
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getUserById(userId));
    }

    /**
     * Updates a user profile. Allowed for the user and global admins.
     *
     * @param userId user identifier
     * @param request updated user profile payload
     * @param connectedUser authenticated principal performing the action
     * @return updated user representation
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request,
            Principal connectedUser
    ) {
        return ResponseEntity.ok(service.updateUser(userId, request, connectedUser));
    }

    /**
     * Changes a user's global role. Allowed for global admins only.
     *
     * @param userId user identifier
     * @param request role update payload
     * @param connectedUser authenticated principal performing the action
     * @return updated user representation
     */
    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserResponse> changeUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UserRoleUpdateRequest request,
            Principal connectedUser
    ) {
        return ResponseEntity.ok(service.changeUserRole(userId, request, connectedUser));
    }

    /**
     * Deletes a user profile. Allowed for the user and global admins.
     *
     * @param userId user identifier
     * @param connectedUser authenticated principal performing the action
     * @return empty 204 on success
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long userId,
            Principal connectedUser
    ) {
        service.deleteUser(userId, connectedUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * Fetches a user's profile image metadata.
     *
     * @param userId user identifier
     * @return profile image metadata
     */
    @GetMapping("/{userId}/profile-image")
    public ResponseEntity<UserImageResponse> getUserProfileImage(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getUserProfileImage(userId));
    }

    /**
     * Returns the currently authenticated user's details.
     *
     * @param connectedUser authenticated principal
     * @return authenticated user representation
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getAuthenticatedUser(Principal connectedUser) {
        return ResponseEntity.ok(service.getAuthenticatedUser(connectedUser));
    }

    /**
     * Lists reviews for a user.
     *
     * @param userId reviewed user identifier
     * @return reviews associated with the user
     */
    @GetMapping("/{userId}/reviews")
    public ResponseEntity<List<ReviewResponse>> getUserReviews(@PathVariable Long userId) {
        return ResponseEntity.ok(reviewService.getUserReviews(userId));
    }

    /**
     * Fetches a specific review for a user by reviewer id.
     *
     * @param userId reviewed user identifier
     * @param reviewerId reviewer identifier
     * @return review details
     */
    @GetMapping("/{userId}/reviews/{reviewerId}")
    public ResponseEntity<ReviewResponse> getUserReview(
            @PathVariable Long userId,
            @PathVariable Long reviewerId
    ) {
        return ResponseEntity.ok(reviewService.getUserReview(userId, reviewerId));
    }

    /**
     * Creates a review for a user.
     *
     * @param userId reviewed user identifier
     * @param request review payload
     * @return created review
     */
    @PostMapping("/{userId}/reviews")
    public ResponseEntity<ReviewResponse> createUserReview(
            @PathVariable Long userId,
            @Valid @RequestBody ReviewRequest request
    ) {
        ReviewResponse response = reviewService.createUserReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates a review for a user by reviewer id.
     *
     * @param userId reviewed user identifier
     * @param reviewerId reviewer identifier
     * @param request review update payload
     * @return updated review
     */
    @PutMapping("/{userId}/reviews/{reviewerId}")
    public ResponseEntity<ReviewResponse> updateUserReview(
            @PathVariable Long userId,
            @PathVariable Long reviewerId,
            @Valid @RequestBody ReviewUpdateRequest request
    ) {
        return ResponseEntity.ok(reviewService.updateUserReview(userId, reviewerId, request));
    }

    /**
     * Deletes a review for a user by reviewer id.
     *
     * @param userId reviewed user identifier
     * @param reviewerId reviewer identifier
     * @return empty 204 on success
     */
    @DeleteMapping("/{userId}/reviews/{reviewerId}")
    public ResponseEntity<Void> deleteUserReview(
            @PathVariable Long userId,
            @PathVariable Long reviewerId
    ) {
        reviewService.deleteUserReview(userId, reviewerId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Creates a profile image for a user. Allowed for the user and global admins.
     *
     * @param userId user identifier
     * @param request image payload
     * @param connectedUser authenticated principal performing the action
     * @return created profile image metadata
     */
    @PostMapping("/{userId}/profile-image")
    public ResponseEntity<UserImageResponse> addUserProfileImage(
            @PathVariable Long userId,
            @Valid @RequestBody UserImageRequest request,
            Principal connectedUser
    ) {
        UserImageResponse response = service.addUserProfileImage(userId, request, connectedUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates a profile image for a user. Allowed for the user and global admins.
     *
     * @param userId user identifier
     * @param request image payload
     * @param connectedUser authenticated principal performing the action
     * @return updated profile image metadata
     */
    @PutMapping("/{userId}/profile-image")
    public ResponseEntity<UserImageResponse> updateUserProfileImage(
            @PathVariable Long userId,
            @Valid @RequestBody UserImageRequest request,
            Principal connectedUser
    ) {
        return ResponseEntity.ok(service.updateUserProfileImage(userId, request, connectedUser));
    }

    /**
     * Deletes a profile image for a user. Allowed for the user and global admins.
     *
     * @param userId user identifier
     * @param connectedUser authenticated principal performing the action
     * @return empty 204 on success
     */
    @DeleteMapping("/{userId}/profile-image")
    public ResponseEntity<Void> deleteUserProfileImage(
            @PathVariable Long userId,
            Principal connectedUser
    ) {
        service.deleteUserProfileImage(userId, connectedUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * Allows an authenticated user to update their password.
     *
     * @param request validated payload containing current and new passwords
     * @param connectedUser authenticated principal performing the change
     * @return empty 200 OK when the password was updated
     */
    @PatchMapping
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Principal connectedUser
    ) {
        service.changePassword(request, connectedUser);
        return ResponseEntity.ok().build();
    }
}
