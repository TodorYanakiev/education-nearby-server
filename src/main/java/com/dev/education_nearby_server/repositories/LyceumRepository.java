package com.dev.education_nearby_server.repositories;

import com.dev.education_nearby_server.enums.VerificationStatus;
import com.dev.education_nearby_server.models.entity.Lyceum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LyceumRepository extends JpaRepository<Lyceum, Long> {
    Optional<Lyceum> findFirstByNameIgnoreCaseAndTownIgnoreCase(String name, String town);
    List<Lyceum> findAllByTownIgnoreCase(String town);
    List<Lyceum> findAllByVerificationStatus(VerificationStatus status);
    @EntityGraph(attributePaths = "lecturers")
    Optional<Lyceum> findWithLecturersById(Long id);

    @Modifying
    @Query("UPDATE Lyceum l SET l.seenInResultsCount = l.seenInResultsCount + 1 WHERE l.id IN :lyceumIds")
    void incrementSeenInResultsCount(@Param("lyceumIds") List<Long> lyceumIds);

    @Query(value = """
            SELECT *
            FROM lyceums l
            WHERE l.verification_status = :status
              AND (:town IS NULL OR LOWER(l.town) = LOWER(:town))
              AND (:latitude IS NULL OR :longitude IS NULL OR (l.latitude IS NOT NULL AND l.longitude IS NOT NULL))
            ORDER BY CASE
                        WHEN :latitude IS NULL OR :longitude IS NULL THEN 0
                        ELSE (6371 * ACOS(
                                COS(RADIANS(:latitude)) * COS(RADIANS(l.latitude)) *
                                COS(RADIANS(l.longitude) - RADIANS(:longitude)) +
                                SIN(RADIANS(:latitude)) * SIN(RADIANS(l.latitude))
                             ))
                     END,
                     l.id
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM lyceums l
            WHERE l.verification_status = :status
              AND (:town IS NULL OR LOWER(l.town) = LOWER(:town))
              AND (:latitude IS NULL OR :longitude IS NULL OR (l.latitude IS NOT NULL AND l.longitude IS NOT NULL))
            """,
            nativeQuery = true)
    Page<Lyceum> filterLyceums(
            @Param("town") String town,
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("status") String status,
            Pageable pageable
    );
}
