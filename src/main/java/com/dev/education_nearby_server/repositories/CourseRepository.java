package com.dev.education_nearby_server.repositories;

import com.dev.education_nearby_server.enums.AgeGroup;
import com.dev.education_nearby_server.enums.CourseType;
import com.dev.education_nearby_server.enums.ScheduleRecurrence;
import com.dev.education_nearby_server.models.entity.Course;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @EntityGraph(attributePaths = {"images", "lyceum"})
    Optional<Course> findDetailedById(Long id);

    @EntityGraph(attributePaths = {"lecturers", "lyceum"})
    List<Course> findAllByLyceum_Id(Long lyceumId);

    @Query("""
            SELECT DISTINCT c
            FROM Course c
            LEFT JOIN c.ageGroupList ageGroup
            LEFT JOIN c.schedule.slots slot
            WHERE (:applyCourseTypeFilter = false OR c.type IN :courseTypes)
              AND (:applyAgeGroupFilter = false OR ageGroup IN :ageGroups)
              AND (:minPrice IS NULL OR c.price >= :minPrice)
              AND (:maxPrice IS NULL OR c.price <= :maxPrice)
              AND (:recurrence IS NULL OR slot.recurrence = :recurrence)
              AND (:dayOfWeek IS NULL OR slot.dayOfWeek = :dayOfWeek)
              AND (:startTimeFrom IS NULL OR slot.startTime >= :startTimeFrom)
              AND (:startTimeTo IS NULL OR slot.startTime <= :startTimeTo)
            ORDER BY c.id
            """)
    List<Course> filterCourses(
            @Param("courseTypes") List<CourseType> courseTypes,
            @Param("applyCourseTypeFilter") boolean applyCourseTypeFilter,
            @Param("ageGroups") List<AgeGroup> ageGroups,
            @Param("applyAgeGroupFilter") boolean applyAgeGroupFilter,
            @Param("minPrice") Float minPrice,
            @Param("maxPrice") Float maxPrice,
            @Param("recurrence") ScheduleRecurrence recurrence,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTimeFrom") LocalTime startTimeFrom,
            @Param("startTimeTo") LocalTime startTimeTo
    );
}
