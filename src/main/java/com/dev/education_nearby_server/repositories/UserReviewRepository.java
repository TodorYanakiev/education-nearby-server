package com.dev.education_nearby_server.repositories;

import com.dev.education_nearby_server.models.entity.UserReview;
import com.dev.education_nearby_server.models.entity.UserReviewId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserReviewRepository extends JpaRepository<UserReview, UserReviewId> {

    @EntityGraph(attributePaths = {"review", "review.user"})
    List<UserReview> findAllByReviewedUser_IdAndReview_DeletedAtIsNull(Long reviewedUserId);

    @EntityGraph(attributePaths = {"review", "review.user"})
    Optional<UserReview> findByReviewedUser_IdAndReviewer_IdAndReview_DeletedAtIsNull(Long reviewedUserId, Long reviewerId);

    boolean existsByReviewedUser_IdAndReviewer_Id(Long reviewedUserId, Long reviewerId);

    @Query("""
            SELECT AVG(r.rating)
            FROM UserReview ur
            JOIN ur.review r
            WHERE ur.reviewedUser.id = :reviewedUserId
              AND r.deletedAt IS NULL
            """)
    Double findAverageRatingByReviewedUserId(@Param("reviewedUserId") Long reviewedUserId);
}
