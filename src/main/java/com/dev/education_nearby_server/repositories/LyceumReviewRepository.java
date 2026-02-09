package com.dev.education_nearby_server.repositories;

import com.dev.education_nearby_server.models.entity.LyceumReview;
import com.dev.education_nearby_server.models.entity.LyceumReviewId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LyceumReviewRepository extends JpaRepository<LyceumReview, LyceumReviewId> {

    @EntityGraph(attributePaths = {"review", "review.user"})
    List<LyceumReview> findAllByLyceum_IdAndReview_DeletedAtIsNull(Long lyceumId);

    @EntityGraph(attributePaths = {"review", "review.user"})
    Optional<LyceumReview> findByLyceum_IdAndReviewer_IdAndReview_DeletedAtIsNull(Long lyceumId, Long reviewerId);

    boolean existsByLyceum_IdAndReviewer_Id(Long lyceumId, Long reviewerId);

    @Query("""
            SELECT AVG(r.rating)
            FROM LyceumReview lr
            JOIN lr.review r
            WHERE lr.lyceum.id = :lyceumId
              AND r.deletedAt IS NULL
            """)
    Double findAverageRatingByLyceumId(@Param("lyceumId") Long lyceumId);
}
