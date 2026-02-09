package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.enums.Role;
import com.dev.education_nearby_server.exceptions.common.AccessDeniedException;
import com.dev.education_nearby_server.exceptions.common.BadRequestException;
import com.dev.education_nearby_server.exceptions.common.ConflictException;
import com.dev.education_nearby_server.exceptions.common.NoSuchElementException;
import com.dev.education_nearby_server.exceptions.common.UnauthorizedException;
import com.dev.education_nearby_server.models.dto.request.ReviewRequest;
import com.dev.education_nearby_server.models.dto.request.ReviewUpdateRequest;
import com.dev.education_nearby_server.models.dto.response.ReviewResponse;
import com.dev.education_nearby_server.models.entity.Course;
import com.dev.education_nearby_server.models.entity.CourseReview;
import com.dev.education_nearby_server.models.entity.Lyceum;
import com.dev.education_nearby_server.models.entity.LyceumReview;
import com.dev.education_nearby_server.models.entity.Review;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.models.entity.UserReview;
import com.dev.education_nearby_server.repositories.CourseRepository;
import com.dev.education_nearby_server.repositories.CourseReviewRepository;
import com.dev.education_nearby_server.repositories.LyceumRepository;
import com.dev.education_nearby_server.repositories.LyceumReviewRepository;
import com.dev.education_nearby_server.repositories.ReviewRepository;
import com.dev.education_nearby_server.repositories.UserRepository;
import com.dev.education_nearby_server.repositories.UserReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles review CRUD operations for courses, lyceums, and users.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private static final String NOT_FOUND = " not found.";

    private final ReviewRepository reviewRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final LyceumReviewRepository lyceumReviewRepository;
    private final UserReviewRepository userReviewRepository;
    private final CourseRepository courseRepository;
    private final LyceumRepository lyceumRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ReviewResponse> getCourseReviews(Long courseId) {
        Course course = requireCourse(courseId);
        return courseReviewRepository.findAllByCourse_IdAndReview_DeletedAtIsNull(course.getId())
                .stream()
                .map(link -> mapToResponse(link.getReview()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReviewResponse getCourseReview(Long courseId, Long userId) {
        CourseReview link = requireCourseReviewLink(courseId, userId);
        return mapToResponse(link.getReview());
    }

    @Transactional
    public ReviewResponse createCourseReview(Long courseId, ReviewRequest request) {
        ReviewRequest payload = requireReviewRequest(request);
        User currentUser = getManagedCurrentUser();
        Course course = requireCourse(courseId);
        ensureCourseReviewUnique(course.getId(), currentUser.getId());

        Review saved = saveReview(payload, currentUser);
        CourseReview link = new CourseReview();
        link.setCourse(course);
        link.setReviewer(currentUser);
        link.setReview(saved);
        courseReviewRepository.save(link);
        log.info("Created course review. courseId={} userId={} reviewId={}", courseId, currentUser.getId(), saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    public ReviewResponse updateCourseReview(Long courseId, Long userId, ReviewUpdateRequest request) {
        ReviewUpdateRequest payload = requireReviewUpdateRequest(request);
        CourseReview link = requireCourseReviewLink(courseId, userId);
        Review review = link.getReview();
        User currentUser = getManagedCurrentUser();
        ensureAuthorCanEdit(currentUser, review);
        applyReviewUpdates(review, payload);
        Review saved = reviewRepository.save(review);
        log.info("Updated course review. courseId={} userId={} reviewId={}", courseId, userId, saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteCourseReview(Long courseId, Long userId) {
        CourseReview link = requireCourseReviewLink(courseId, userId);
        Review review = link.getReview();
        User currentUser = getManagedCurrentUser();
        ensureUserCanDelete(currentUser, review);
        softDeleteReview(review);
        courseReviewRepository.delete(link);
        log.info("Deleted course review. courseId={} userId={} reviewId={}", courseId, userId, review.getId());
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getLyceumReviews(Long lyceumId) {
        Lyceum lyceum = requireLyceum(lyceumId);
        return lyceumReviewRepository.findAllByLyceum_IdAndReview_DeletedAtIsNull(lyceum.getId())
                .stream()
                .map(link -> mapToResponse(link.getReview()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReviewResponse getLyceumReview(Long lyceumId, Long userId) {
        LyceumReview link = requireLyceumReviewLink(lyceumId, userId);
        return mapToResponse(link.getReview());
    }

    @Transactional
    public ReviewResponse createLyceumReview(Long lyceumId, ReviewRequest request) {
        ReviewRequest payload = requireReviewRequest(request);
        User currentUser = getManagedCurrentUser();
        Lyceum lyceum = requireLyceum(lyceumId);
        ensureLyceumReviewUnique(lyceum.getId(), currentUser.getId());

        Review saved = saveReview(payload, currentUser);
        LyceumReview link = new LyceumReview();
        link.setLyceum(lyceum);
        link.setReviewer(currentUser);
        link.setReview(saved);
        lyceumReviewRepository.save(link);
        log.info("Created lyceum review. lyceumId={} userId={} reviewId={}", lyceumId, currentUser.getId(), saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    public ReviewResponse updateLyceumReview(Long lyceumId, Long userId, ReviewUpdateRequest request) {
        ReviewUpdateRequest payload = requireReviewUpdateRequest(request);
        LyceumReview link = requireLyceumReviewLink(lyceumId, userId);
        Review review = link.getReview();
        User currentUser = getManagedCurrentUser();
        ensureAuthorCanEdit(currentUser, review);
        applyReviewUpdates(review, payload);
        Review saved = reviewRepository.save(review);
        log.info("Updated lyceum review. lyceumId={} userId={} reviewId={}", lyceumId, userId, saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteLyceumReview(Long lyceumId, Long userId) {
        LyceumReview link = requireLyceumReviewLink(lyceumId, userId);
        Review review = link.getReview();
        User currentUser = getManagedCurrentUser();
        ensureUserCanDelete(currentUser, review);
        softDeleteReview(review);
        lyceumReviewRepository.delete(link);
        log.info("Deleted lyceum review. lyceumId={} userId={} reviewId={}", lyceumId, userId, review.getId());
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getUserReviews(Long reviewedUserId) {
        User reviewedUser = requireUser(reviewedUserId);
        return userReviewRepository.findAllByReviewedUser_IdAndReview_DeletedAtIsNull(reviewedUser.getId())
                .stream()
                .map(link -> mapToResponse(link.getReview()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReviewResponse getUserReview(Long reviewedUserId, Long userId) {
        UserReview link = requireUserReviewLink(reviewedUserId, userId);
        return mapToResponse(link.getReview());
    }

    @Transactional
    public ReviewResponse createUserReview(Long reviewedUserId, ReviewRequest request) {
        ReviewRequest payload = requireReviewRequest(request);
        User currentUser = getManagedCurrentUser();
        User reviewedUser = requireUser(reviewedUserId);
        ensureUserReviewUnique(reviewedUser.getId(), currentUser.getId());

        Review saved = saveReview(payload, currentUser);
        UserReview link = new UserReview();
        link.setReviewedUser(reviewedUser);
        link.setReviewer(currentUser);
        link.setReview(saved);
        userReviewRepository.save(link);
        log.info("Created user review. reviewedUserId={} userId={} reviewId={}", reviewedUserId, currentUser.getId(), saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    public ReviewResponse updateUserReview(Long reviewedUserId, Long userId, ReviewUpdateRequest request) {
        ReviewUpdateRequest payload = requireReviewUpdateRequest(request);
        UserReview link = requireUserReviewLink(reviewedUserId, userId);
        Review review = link.getReview();
        User currentUser = getManagedCurrentUser();
        ensureAuthorCanEdit(currentUser, review);
        applyReviewUpdates(review, payload);
        Review saved = reviewRepository.save(review);
        log.info("Updated user review. reviewedUserId={} userId={} reviewId={}", reviewedUserId, userId, saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteUserReview(Long reviewedUserId, Long userId) {
        UserReview link = requireUserReviewLink(reviewedUserId, userId);
        Review review = link.getReview();
        User currentUser = getManagedCurrentUser();
        ensureUserCanDelete(currentUser, review);
        softDeleteReview(review);
        userReviewRepository.delete(link);
        log.info("Deleted user review. reviewedUserId={} userId={} reviewId={}", reviewedUserId, userId, review.getId());
    }

    private ReviewRequest requireReviewRequest(ReviewRequest request) {
        if (request == null) {
            throw new BadRequestException("Review payload must not be null.");
        }
        return request;
    }

    private ReviewUpdateRequest requireReviewUpdateRequest(ReviewUpdateRequest request) {
        if (request == null) {
            throw new BadRequestException("Review payload must not be null.");
        }
        if (request.getRating() == null && request.getComment() == null) {
            throw new BadRequestException("At least one field must be provided for update.");
        }
        return request;
    }

    private Review saveReview(ReviewRequest request, User author) {
        Review review = new Review();
        review.setUser(author);
        review.setRating(request.getRating());
        review.setComment(trimToNull(request.getComment()));
        return reviewRepository.save(review);
    }

    private void applyReviewUpdates(Review review, ReviewUpdateRequest request) {
        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }
        if (request.getComment() != null) {
            review.setComment(trimToNull(request.getComment()));
        }
    }

    private void ensureCourseReviewUnique(Long courseId, Long userId) {
        if (courseReviewRepository.existsByCourse_IdAndReviewer_Id(courseId, userId)) {
            throw new ConflictException("You have already reviewed this course.");
        }
    }

    private void ensureLyceumReviewUnique(Long lyceumId, Long userId) {
        if (lyceumReviewRepository.existsByLyceum_IdAndReviewer_Id(lyceumId, userId)) {
            throw new ConflictException("You have already reviewed this lyceum.");
        }
    }

    private void ensureUserReviewUnique(Long reviewedUserId, Long userId) {
        if (userReviewRepository.existsByReviewedUser_IdAndReviewer_Id(reviewedUserId, userId)) {
            throw new ConflictException("You have already reviewed this user.");
        }
    }

    private Course requireCourse(Long courseId) {
        if (courseId == null) {
            throw new BadRequestException("Course id must be provided.");
        }
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Course with id " + courseId + NOT_FOUND));
    }

    private Lyceum requireLyceum(Long lyceumId) {
        if (lyceumId == null) {
            throw new BadRequestException("Lyceum id must be provided.");
        }
        return lyceumRepository.findById(lyceumId)
                .orElseThrow(() -> new NoSuchElementException("Lyceum with id " + lyceumId + NOT_FOUND));
    }

    private User requireUser(Long userId) {
        if (userId == null) {
            throw new BadRequestException("User id must be provided.");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User with id " + userId + NOT_FOUND));
    }

    private CourseReview requireCourseReviewLink(Long courseId, Long userId) {
        if (courseId == null || userId == null) {
            throw new BadRequestException("Course id and user id must be provided.");
        }
        return courseReviewRepository.findByCourse_IdAndReviewer_IdAndReview_DeletedAtIsNull(courseId, userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Review for course with id " + courseId + " and user id " + userId + NOT_FOUND
                ));
    }

    private LyceumReview requireLyceumReviewLink(Long lyceumId, Long userId) {
        if (lyceumId == null || userId == null) {
            throw new BadRequestException("Lyceum id and user id must be provided.");
        }
        return lyceumReviewRepository.findByLyceum_IdAndReviewer_IdAndReview_DeletedAtIsNull(lyceumId, userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Review for lyceum with id " + lyceumId + " and user id " + userId + NOT_FOUND
                ));
    }

    private UserReview requireUserReviewLink(Long reviewedUserId, Long userId) {
        if (reviewedUserId == null || userId == null) {
            throw new BadRequestException("Reviewed user id and user id must be provided.");
        }
        return userReviewRepository.findByReviewedUser_IdAndReviewer_IdAndReview_DeletedAtIsNull(reviewedUserId, userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Review for user with id " + reviewedUserId + " and user id " + userId + NOT_FOUND
                ));
    }

    private void softDeleteReview(Review review) {
        if (review.getDeletedAt() != null) {
            throw new ConflictException("Review is already deleted.");
        }
        review.setDeletedAt(LocalDateTime.now());
        reviewRepository.save(review);
    }

    private void ensureAuthorCanEdit(User currentUser, Review review) {
        if (review == null || review.getUser() == null || review.getUser().getId() == null) {
            throw new AccessDeniedException("You do not have permission to modify this review.");
        }
        if (!Objects.equals(currentUser.getId(), review.getUser().getId())) {
            throw new AccessDeniedException("You do not have permission to modify this review.");
        }
    }

    private void ensureUserCanDelete(User currentUser, Review review) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (review == null || review.getUser() == null || review.getUser().getId() == null) {
            throw new AccessDeniedException("You do not have permission to delete this review.");
        }
        if (!Objects.equals(currentUser.getId(), review.getUser().getId())) {
            throw new AccessDeniedException("You do not have permission to delete this review.");
        }
    }

    private User getManagedCurrentUser() {
        User currentUser = getCurrentUser()
                .orElseThrow(() -> new UnauthorizedException("You must be authenticated to perform this action."));
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UnauthorizedException("User" + NOT_FOUND));
    }

    private Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof User user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .userId(review.getUser() != null ? review.getUser().getId() : null)
                .createdAt(review.getCreatedAt())
                .deletedAt(review.getDeletedAt())
                .build();
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? null : value.trim();
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }
}
