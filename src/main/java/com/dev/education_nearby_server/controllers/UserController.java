package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.models.dto.auth.ChangePasswordRequest;
import com.dev.education_nearby_server.models.dto.request.ReviewRequest;
import com.dev.education_nearby_server.models.dto.request.ReviewUpdateRequest;
import com.dev.education_nearby_server.models.dto.response.ReviewResponse;
import com.dev.education_nearby_server.models.dto.response.UserResponse;
import com.dev.education_nearby_server.services.ReviewService;
import com.dev.education_nearby_server.services.UserService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
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
     * Lists all users.
     *
     * @return public user representations
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(service.getAllUsers());
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
