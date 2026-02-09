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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private CourseReviewRepository courseReviewRepository;
    @Mock
    private LyceumReviewRepository lyceumReviewRepository;
    @Mock
    private UserReviewRepository userReviewRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private LyceumRepository lyceumRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewService reviewService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCourseReviewsReturnsMappedResponses() {
        Course course = buildCourse(3L);
        User reviewer = buildUser(5L, Role.USER);
        Review review = buildReview(11L, 4, "Great", reviewer);
        CourseReview link = buildCourseReview(course, reviewer, review);
        when(courseRepository.findById(3L)).thenReturn(Optional.of(course));
        when(courseReviewRepository.findAllByCourse_IdAndReview_DeletedAtIsNull(3L)).thenReturn(List.of(link));

        List<ReviewResponse> responses = reviewService.getCourseReviews(3L);

        assertThat(responses).hasSize(1);
        ReviewResponse response = responses.getFirst();
        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getUserId()).isEqualTo(5L);
        assertThat(response.getRating()).isEqualTo(4);
        assertThat(response.getComment()).isEqualTo("Great");
        verify(courseReviewRepository).findAllByCourse_IdAndReview_DeletedAtIsNull(3L);
    }

    @Test
    void getCourseReviewReturnsMappedResponse() {
        Course course = buildCourse(9L);
        User reviewer = buildUser(4L, Role.USER);
        Review review = buildReview(22L, 2, "Ok", reviewer);
        CourseReview link = buildCourseReview(course, reviewer, review);
        when(courseReviewRepository.findByCourse_IdAndReviewer_IdAndReview_DeletedAtIsNull(9L, 4L))
                .thenReturn(Optional.of(link));

        ReviewResponse response = reviewService.getCourseReview(9L, 4L);

        assertThat(response.getId()).isEqualTo(22L);
        assertThat(response.getUserId()).isEqualTo(4L);
        assertThat(response.getRating()).isEqualTo(2);
        assertThat(response.getComment()).isEqualTo("Ok");
    }

    @Test
    void createCourseReviewTrimsCommentAndSavesLink() {
        User reviewer = buildUser(7L, Role.USER);
        Course course = buildCourse(8L);
        authenticate(reviewer);
        when(userRepository.findById(7L)).thenReturn(Optional.of(reviewer));
        when(courseRepository.findById(8L)).thenReturn(Optional.of(course));
        when(courseReviewRepository.existsByCourse_IdAndReviewer_Id(8L, 7L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review saved = invocation.getArgument(0);
            saved.setId(33L);
            return saved;
        });
        ReviewRequest request = ReviewRequest.builder()
                .rating(5)
                .comment("  nice work  ")
                .build();

        ReviewResponse response = reviewService.createCourseReview(8L, request);

        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(reviewCaptor.capture());
        assertThat(reviewCaptor.getValue().getComment()).isEqualTo("nice work");
        assertThat(response.getId()).isEqualTo(33L);
        assertThat(response.getComment()).isEqualTo("nice work");
        ArgumentCaptor<CourseReview> linkCaptor = ArgumentCaptor.forClass(CourseReview.class);
        verify(courseReviewRepository).save(linkCaptor.capture());
        assertThat(linkCaptor.getValue().getCourse()).isEqualTo(course);
        assertThat(linkCaptor.getValue().getReviewer()).isEqualTo(reviewer);
    }

    @Test
    void createCourseReviewThrowsWhenDuplicate() {
        User reviewer = buildUser(7L, Role.USER);
        Course course = buildCourse(8L);
        authenticate(reviewer);
        when(userRepository.findById(7L)).thenReturn(Optional.of(reviewer));
        when(courseRepository.findById(8L)).thenReturn(Optional.of(course));
        when(courseReviewRepository.existsByCourse_IdAndReviewer_Id(8L, 7L)).thenReturn(true);
        ReviewRequest request = ReviewRequest.builder().rating(4).comment("Ok").build();

        assertThrows(ConflictException.class, () -> reviewService.createCourseReview(8L, request));
        verify(reviewRepository, never()).save(any(Review.class));
        verify(courseReviewRepository, never()).save(any(CourseReview.class));
    }

    @Test
    void createCourseReviewThrowsWhenUnauthenticated() {
        ReviewRequest request = ReviewRequest.builder().rating(3).comment("Ok").build();

        assertThrows(UnauthorizedException.class, () -> reviewService.createCourseReview(1L, request));
    }

    @Test
    void updateCourseReviewUpdatesFields() {
        User reviewer = buildUser(3L, Role.USER);
        Course course = buildCourse(9L);
        Review review = buildReview(44L, 2, "old", reviewer);
        CourseReview link = buildCourseReview(course, reviewer, review);
        authenticate(reviewer);
        when(userRepository.findById(3L)).thenReturn(Optional.of(reviewer));
        when(courseReviewRepository.findByCourse_IdAndReviewer_IdAndReview_DeletedAtIsNull(9L, 3L))
                .thenReturn(Optional.of(link));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReviewUpdateRequest request = ReviewUpdateRequest.builder()
                .rating(4)
                .comment("  updated  ")
                .build();

        ReviewResponse response = reviewService.updateCourseReview(9L, 3L, request);

        assertThat(response.getRating()).isEqualTo(4);
        assertThat(response.getComment()).isEqualTo("updated");
        assertThat(review.getRating()).isEqualTo(4);
        assertThat(review.getComment()).isEqualTo("updated");
    }

    @Test
    void updateCourseReviewThrowsWhenEmptyUpdate() {
        ReviewUpdateRequest request = ReviewUpdateRequest.builder().build();

        assertThrows(BadRequestException.class, () -> reviewService.updateCourseReview(1L, 2L, request));
    }

    @Test
    void updateCourseReviewRejectsNonAuthor() {
        User reviewer = buildUser(1L, Role.USER);
        User otherUser = buildUser(2L, Role.USER);
        Course course = buildCourse(9L);
        Review review = buildReview(10L, 3, "old", reviewer);
        CourseReview link = buildCourseReview(course, reviewer, review);
        authenticate(otherUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(courseReviewRepository.findByCourse_IdAndReviewer_IdAndReview_DeletedAtIsNull(9L, 1L))
                .thenReturn(Optional.of(link));
        ReviewUpdateRequest request = ReviewUpdateRequest.builder().rating(5).build();

        assertThrows(AccessDeniedException.class, () -> reviewService.updateCourseReview(9L, 1L, request));
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void deleteCourseReviewAsAdminSoftDeletesAndUnlinks() {
        User admin = buildUser(9L, Role.ADMIN);
        User reviewer = buildUser(1L, Role.USER);
        Course course = buildCourse(5L);
        Review review = buildReview(10L, 3, "ok", reviewer);
        CourseReview link = buildCourseReview(course, reviewer, review);
        authenticate(admin);
        when(userRepository.findById(9L)).thenReturn(Optional.of(admin));
        when(courseReviewRepository.findByCourse_IdAndReviewer_IdAndReview_DeletedAtIsNull(5L, 1L))
                .thenReturn(Optional.of(link));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reviewService.deleteCourseReview(5L, 1L);

        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(reviewCaptor.capture());
        assertThat(reviewCaptor.getValue().getDeletedAt()).isNotNull();
        verify(courseReviewRepository).delete(link);
    }

    @Test
    void deleteCourseReviewRejectsNonAuthor() {
        User reviewer = buildUser(1L, Role.USER);
        User otherUser = buildUser(2L, Role.USER);
        Course course = buildCourse(5L);
        Review review = buildReview(10L, 3, "ok", reviewer);
        CourseReview link = buildCourseReview(course, reviewer, review);
        authenticate(otherUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(courseReviewRepository.findByCourse_IdAndReviewer_IdAndReview_DeletedAtIsNull(5L, 1L))
                .thenReturn(Optional.of(link));

        assertThrows(AccessDeniedException.class, () -> reviewService.deleteCourseReview(5L, 1L));
        verify(reviewRepository, never()).save(any(Review.class));
        verify(courseReviewRepository, never()).delete(any(CourseReview.class));
    }

    @Test
    void deleteCourseReviewThrowsWhenAlreadyDeleted() {
        User admin = buildUser(9L, Role.ADMIN);
        User reviewer = buildUser(1L, Role.USER);
        Course course = buildCourse(5L);
        Review review = buildReview(10L, 3, "ok", reviewer);
        review.setDeletedAt(LocalDateTime.now());
        CourseReview link = buildCourseReview(course, reviewer, review);
        authenticate(admin);
        when(userRepository.findById(9L)).thenReturn(Optional.of(admin));
        when(courseReviewRepository.findByCourse_IdAndReviewer_IdAndReview_DeletedAtIsNull(5L, 1L))
                .thenReturn(Optional.of(link));

        assertThrows(ConflictException.class, () -> reviewService.deleteCourseReview(5L, 1L));
        verify(reviewRepository, never()).save(any(Review.class));
        verify(courseReviewRepository, never()).delete(any(CourseReview.class));
    }

    @Test
    void getLyceumReviewsReturnsMappedResponses() {
        Lyceum lyceum = buildLyceum(12L);
        User reviewer = buildUser(6L, Role.USER);
        Review review = buildReview(13L, 5, "Great", reviewer);
        LyceumReview link = buildLyceumReview(lyceum, reviewer, review);
        when(lyceumRepository.findById(12L)).thenReturn(Optional.of(lyceum));
        when(lyceumReviewRepository.findAllByLyceum_IdAndReview_DeletedAtIsNull(12L)).thenReturn(List.of(link));

        List<ReviewResponse> responses = reviewService.getLyceumReviews(12L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getId()).isEqualTo(13L);
        assertThat(responses.getFirst().getUserId()).isEqualTo(6L);
    }

    @Test
    void createLyceumReviewSavesLink() {
        User reviewer = buildUser(4L, Role.USER);
        Lyceum lyceum = buildLyceum(7L);
        authenticate(reviewer);
        when(userRepository.findById(4L)).thenReturn(Optional.of(reviewer));
        when(lyceumRepository.findById(7L)).thenReturn(Optional.of(lyceum));
        when(lyceumReviewRepository.existsByLyceum_IdAndReviewer_Id(7L, 4L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review saved = invocation.getArgument(0);
            saved.setId(41L);
            return saved;
        });
        ReviewRequest request = ReviewRequest.builder().rating(4).comment("Nice").build();

        ReviewResponse response = reviewService.createLyceumReview(7L, request);

        assertThat(response.getId()).isEqualTo(41L);
        ArgumentCaptor<LyceumReview> linkCaptor = ArgumentCaptor.forClass(LyceumReview.class);
        verify(lyceumReviewRepository).save(linkCaptor.capture());
        assertThat(linkCaptor.getValue().getLyceum()).isEqualTo(lyceum);
    }

    @Test
    void updateLyceumReviewUpdatesRatingOnly() {
        User reviewer = buildUser(4L, Role.USER);
        Lyceum lyceum = buildLyceum(7L);
        Review review = buildReview(41L, 2, "old", reviewer);
        LyceumReview link = buildLyceumReview(lyceum, reviewer, review);
        authenticate(reviewer);
        when(userRepository.findById(4L)).thenReturn(Optional.of(reviewer));
        when(lyceumReviewRepository.findByLyceum_IdAndReviewer_IdAndReview_DeletedAtIsNull(7L, 4L))
                .thenReturn(Optional.of(link));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReviewUpdateRequest request = ReviewUpdateRequest.builder().rating(5).build();

        ReviewResponse response = reviewService.updateLyceumReview(7L, 4L, request);

        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getComment()).isEqualTo("old");
    }

    @Test
    void deleteLyceumReviewAsAuthorSoftDeletesAndUnlinks() {
        User reviewer = buildUser(4L, Role.USER);
        Lyceum lyceum = buildLyceum(7L);
        Review review = buildReview(41L, 4, "ok", reviewer);
        LyceumReview link = buildLyceumReview(lyceum, reviewer, review);
        authenticate(reviewer);
        when(userRepository.findById(4L)).thenReturn(Optional.of(reviewer));
        when(lyceumReviewRepository.findByLyceum_IdAndReviewer_IdAndReview_DeletedAtIsNull(7L, 4L))
                .thenReturn(Optional.of(link));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reviewService.deleteLyceumReview(7L, 4L);

        verify(reviewRepository).save(any(Review.class));
        verify(lyceumReviewRepository).delete(link);
    }

    @Test
    void getUserReviewsReturnsMappedResponses() {
        User reviewedUser = buildUser(12L, Role.USER);
        User reviewer = buildUser(6L, Role.USER);
        Review review = buildReview(13L, 5, "Great", reviewer);
        UserReview link = buildUserReview(reviewedUser, reviewer, review);
        when(userRepository.findById(12L)).thenReturn(Optional.of(reviewedUser));
        when(userReviewRepository.findAllByReviewedUser_IdAndReview_DeletedAtIsNull(12L)).thenReturn(List.of(link));

        List<ReviewResponse> responses = reviewService.getUserReviews(12L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getId()).isEqualTo(13L);
        assertThat(responses.getFirst().getUserId()).isEqualTo(6L);
    }

    @Test
    void getUserReviewReturnsMappedResponse() {
        User reviewedUser = buildUser(8L, Role.USER);
        User reviewer = buildUser(3L, Role.USER);
        Review review = buildReview(18L, 1, "Bad", reviewer);
        UserReview link = buildUserReview(reviewedUser, reviewer, review);
        when(userReviewRepository.findByReviewedUser_IdAndReviewer_IdAndReview_DeletedAtIsNull(8L, 3L))
                .thenReturn(Optional.of(link));

        ReviewResponse response = reviewService.getUserReview(8L, 3L);

        assertThat(response.getId()).isEqualTo(18L);
        assertThat(response.getUserId()).isEqualTo(3L);
        assertThat(response.getRating()).isEqualTo(1);
    }

    @Test
    void createUserReviewSavesLink() {
        User reviewer = buildUser(10L, Role.USER);
        User reviewedUser = buildUser(20L, Role.USER);
        authenticate(reviewer);
        when(userRepository.findById(10L)).thenReturn(Optional.of(reviewer));
        when(userRepository.findById(20L)).thenReturn(Optional.of(reviewedUser));
        when(userReviewRepository.existsByReviewedUser_IdAndReviewer_Id(20L, 10L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review saved = invocation.getArgument(0);
            saved.setId(51L);
            return saved;
        });
        ReviewRequest request = ReviewRequest.builder().rating(5).comment("Great").build();

        ReviewResponse response = reviewService.createUserReview(20L, request);

        assertThat(response.getId()).isEqualTo(51L);
        ArgumentCaptor<UserReview> linkCaptor = ArgumentCaptor.forClass(UserReview.class);
        verify(userReviewRepository).save(linkCaptor.capture());
        assertThat(linkCaptor.getValue().getReviewedUser()).isEqualTo(reviewedUser);
    }

    @Test
    void updateUserReviewTrimsBlankCommentToNull() {
        User reviewer = buildUser(10L, Role.USER);
        User reviewedUser = buildUser(20L, Role.USER);
        Review review = buildReview(51L, 2, "old", reviewer);
        UserReview link = buildUserReview(reviewedUser, reviewer, review);
        authenticate(reviewer);
        when(userRepository.findById(10L)).thenReturn(Optional.of(reviewer));
        when(userReviewRepository.findByReviewedUser_IdAndReviewer_IdAndReview_DeletedAtIsNull(20L, 10L))
                .thenReturn(Optional.of(link));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReviewUpdateRequest request = ReviewUpdateRequest.builder()
                .comment("   ")
                .build();

        ReviewResponse response = reviewService.updateUserReview(20L, 10L, request);

        assertThat(response.getComment()).isNull();
        assertThat(review.getComment()).isNull();
    }

    @Test
    void deleteUserReviewRejectsNonAuthor() {
        User reviewer = buildUser(1L, Role.USER);
        User otherUser = buildUser(2L, Role.USER);
        User reviewedUser = buildUser(9L, Role.USER);
        Review review = buildReview(10L, 3, "ok", reviewer);
        UserReview link = buildUserReview(reviewedUser, reviewer, review);
        authenticate(otherUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(userReviewRepository.findByReviewedUser_IdAndReviewer_IdAndReview_DeletedAtIsNull(9L, 1L))
                .thenReturn(Optional.of(link));

        assertThrows(AccessDeniedException.class, () -> reviewService.deleteUserReview(9L, 1L));
        verify(reviewRepository, never()).save(any(Review.class));
        verify(userReviewRepository, never()).delete(any(UserReview.class));
    }

    @Test
    void getCourseReviewThrowsWhenMissing() {
        when(courseReviewRepository.findByCourse_IdAndReviewer_IdAndReview_DeletedAtIsNull(4L, 9L))
                .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> reviewService.getCourseReview(4L, 9L));
    }

    private void authenticate(User user) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Course buildCourse(Long id) {
        Course course = new Course();
        course.setId(id);
        return course;
    }

    private Lyceum buildLyceum(Long id) {
        Lyceum lyceum = new Lyceum();
        lyceum.setId(id);
        return lyceum;
    }

    private User buildUser(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }

    private Review buildReview(Long id, int rating, String comment, User user) {
        Review review = new Review();
        review.setId(id);
        review.setRating(rating);
        review.setComment(comment);
        review.setUser(user);
        return review;
    }

    private CourseReview buildCourseReview(Course course, User reviewer, Review review) {
        CourseReview link = new CourseReview();
        link.setCourse(course);
        link.setReviewer(reviewer);
        link.setReview(review);
        return link;
    }

    private LyceumReview buildLyceumReview(Lyceum lyceum, User reviewer, Review review) {
        LyceumReview link = new LyceumReview();
        link.setLyceum(lyceum);
        link.setReviewer(reviewer);
        link.setReview(review);
        return link;
    }

    private UserReview buildUserReview(User reviewedUser, User reviewer, Review review) {
        UserReview link = new UserReview();
        link.setReviewedUser(reviewedUser);
        link.setReviewer(reviewer);
        link.setReview(review);
        return link;
    }
}
