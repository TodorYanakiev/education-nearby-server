package com.dev.education_nearby_server.repositories;

import com.dev.education_nearby_server.models.entity.CourseReview;
import com.dev.education_nearby_server.models.entity.CourseReviewId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseReviewRepository extends JpaRepository<CourseReview, CourseReviewId> {

    @EntityGraph(attributePaths = {"review", "review.user"})
    List<CourseReview> findAllByCourse_IdAndReview_DeletedAtIsNull(Long courseId);

    @EntityGraph(attributePaths = {"review", "review.user"})
    Optional<CourseReview> findByCourse_IdAndReviewer_IdAndReview_DeletedAtIsNull(Long courseId, Long reviewerId);

    boolean existsByCourse_IdAndReviewer_Id(Long courseId, Long reviewerId);

    @Query("""
            SELECT AVG(r.rating)
            FROM CourseReview cr
            JOIN cr.review r
            WHERE cr.course.id = :courseId
              AND r.deletedAt IS NULL
            """)
    Double findAverageRatingByCourseId(@Param("courseId") Long courseId);
}
