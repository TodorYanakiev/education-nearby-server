package com.dev.education_nearby_server.repositories;

import com.dev.education_nearby_server.enums.AgeGroup;
import com.dev.education_nearby_server.enums.CourseType;
import com.dev.education_nearby_server.enums.ScheduleRecurrence;
import com.dev.education_nearby_server.models.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @EntityGraph(attributePaths = {"lecturers", "lyceum"})
    List<Course> findDistinctByLecturers_Id(Long lecturerId);

    @Query(value = """
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
              AND (
                  :applyActivePeriodFilter = false OR (
                      c.activeStartMonth IS NOT NULL
                      AND c.activeEndMonth IS NOT NULL
                      AND (
                          (
                              (CASE c.activeStartMonth
                                  WHEN java.time.Month.JANUARY THEN 1
                                  WHEN java.time.Month.FEBRUARY THEN 2
                                  WHEN java.time.Month.MARCH THEN 3
                                  WHEN java.time.Month.APRIL THEN 4
                                  WHEN java.time.Month.MAY THEN 5
                                  WHEN java.time.Month.JUNE THEN 6
                                  WHEN java.time.Month.JULY THEN 7
                                  WHEN java.time.Month.AUGUST THEN 8
                                  WHEN java.time.Month.SEPTEMBER THEN 9
                                  WHEN java.time.Month.OCTOBER THEN 10
                                  WHEN java.time.Month.NOVEMBER THEN 11
                                  WHEN java.time.Month.DECEMBER THEN 12
                                  ELSE null
                              END) <= (CASE c.activeEndMonth
                                  WHEN java.time.Month.JANUARY THEN 1
                                  WHEN java.time.Month.FEBRUARY THEN 2
                                  WHEN java.time.Month.MARCH THEN 3
                                  WHEN java.time.Month.APRIL THEN 4
                                  WHEN java.time.Month.MAY THEN 5
                                  WHEN java.time.Month.JUNE THEN 6
                                  WHEN java.time.Month.JULY THEN 7
                                  WHEN java.time.Month.AUGUST THEN 8
                                  WHEN java.time.Month.SEPTEMBER THEN 9
                                  WHEN java.time.Month.OCTOBER THEN 10
                                  WHEN java.time.Month.NOVEMBER THEN 11
                                  WHEN java.time.Month.DECEMBER THEN 12
                                  ELSE null
                              END)
                              AND :activeStartMonthValue <= :activeEndMonthValue
                              AND (CASE c.activeStartMonth
                                  WHEN java.time.Month.JANUARY THEN 1
                                  WHEN java.time.Month.FEBRUARY THEN 2
                                  WHEN java.time.Month.MARCH THEN 3
                                  WHEN java.time.Month.APRIL THEN 4
                                  WHEN java.time.Month.MAY THEN 5
                                  WHEN java.time.Month.JUNE THEN 6
                                  WHEN java.time.Month.JULY THEN 7
                                  WHEN java.time.Month.AUGUST THEN 8
                                  WHEN java.time.Month.SEPTEMBER THEN 9
                                  WHEN java.time.Month.OCTOBER THEN 10
                                  WHEN java.time.Month.NOVEMBER THEN 11
                                  WHEN java.time.Month.DECEMBER THEN 12
                                  ELSE null
                              END) <= :activeEndMonthValue
                              AND :activeStartMonthValue <= (CASE c.activeEndMonth
                                  WHEN java.time.Month.JANUARY THEN 1
                                  WHEN java.time.Month.FEBRUARY THEN 2
                                  WHEN java.time.Month.MARCH THEN 3
                                  WHEN java.time.Month.APRIL THEN 4
                                  WHEN java.time.Month.MAY THEN 5
                                  WHEN java.time.Month.JUNE THEN 6
                                  WHEN java.time.Month.JULY THEN 7
                                  WHEN java.time.Month.AUGUST THEN 8
                                  WHEN java.time.Month.SEPTEMBER THEN 9
                                  WHEN java.time.Month.OCTOBER THEN 10
                                  WHEN java.time.Month.NOVEMBER THEN 11
                                  WHEN java.time.Month.DECEMBER THEN 12
                                  ELSE null
                              END)
                          )
                          OR
                          (
                              (CASE c.activeStartMonth
                                  WHEN java.time.Month.JANUARY THEN 1
                                  WHEN java.time.Month.FEBRUARY THEN 2
                                  WHEN java.time.Month.MARCH THEN 3
                                  WHEN java.time.Month.APRIL THEN 4
                                  WHEN java.time.Month.MAY THEN 5
                                  WHEN java.time.Month.JUNE THEN 6
                                  WHEN java.time.Month.JULY THEN 7
                                  WHEN java.time.Month.AUGUST THEN 8
                                  WHEN java.time.Month.SEPTEMBER THEN 9
                                  WHEN java.time.Month.OCTOBER THEN 10
                                  WHEN java.time.Month.NOVEMBER THEN 11
                                  WHEN java.time.Month.DECEMBER THEN 12
                                  ELSE null
                              END) > (CASE c.activeEndMonth
                                  WHEN java.time.Month.JANUARY THEN 1
                                  WHEN java.time.Month.FEBRUARY THEN 2
                                  WHEN java.time.Month.MARCH THEN 3
                                  WHEN java.time.Month.APRIL THEN 4
                                  WHEN java.time.Month.MAY THEN 5
                                  WHEN java.time.Month.JUNE THEN 6
                                  WHEN java.time.Month.JULY THEN 7
                                  WHEN java.time.Month.AUGUST THEN 8
                                  WHEN java.time.Month.SEPTEMBER THEN 9
                                  WHEN java.time.Month.OCTOBER THEN 10
                                  WHEN java.time.Month.NOVEMBER THEN 11
                                  WHEN java.time.Month.DECEMBER THEN 12
                                  ELSE null
                              END)
                              AND :activeStartMonthValue > :activeEndMonthValue
                          )
                          OR
                          (
                              (CASE c.activeStartMonth
                                  WHEN java.time.Month.JANUARY THEN 1
                                  WHEN java.time.Month.FEBRUARY THEN 2
                                  WHEN java.time.Month.MARCH THEN 3
                                  WHEN java.time.Month.APRIL THEN 4
                                  WHEN java.time.Month.MAY THEN 5
                                  WHEN java.time.Month.JUNE THEN 6
                                  WHEN java.time.Month.JULY THEN 7
                                  WHEN java.time.Month.AUGUST THEN 8
                                  WHEN java.time.Month.SEPTEMBER THEN 9
                                  WHEN java.time.Month.OCTOBER THEN 10
                                  WHEN java.time.Month.NOVEMBER THEN 11
                                  WHEN java.time.Month.DECEMBER THEN 12
                                  ELSE null
                              END) > (CASE c.activeEndMonth
                                  WHEN java.time.Month.JANUARY THEN 1
                                  WHEN java.time.Month.FEBRUARY THEN 2
                                  WHEN java.time.Month.MARCH THEN 3
                                  WHEN java.time.Month.APRIL THEN 4
                                  WHEN java.time.Month.MAY THEN 5
                                  WHEN java.time.Month.JUNE THEN 6
                                  WHEN java.time.Month.JULY THEN 7
                                  WHEN java.time.Month.AUGUST THEN 8
                                  WHEN java.time.Month.SEPTEMBER THEN 9
                                  WHEN java.time.Month.OCTOBER THEN 10
                                  WHEN java.time.Month.NOVEMBER THEN 11
                                  WHEN java.time.Month.DECEMBER THEN 12
                                  ELSE null
                              END)
                              AND :activeStartMonthValue <= :activeEndMonthValue
                              AND (
                                  :activeStartMonthValue <= (CASE c.activeEndMonth
                                      WHEN java.time.Month.JANUARY THEN 1
                                      WHEN java.time.Month.FEBRUARY THEN 2
                                      WHEN java.time.Month.MARCH THEN 3
                                      WHEN java.time.Month.APRIL THEN 4
                                      WHEN java.time.Month.MAY THEN 5
                                      WHEN java.time.Month.JUNE THEN 6
                                      WHEN java.time.Month.JULY THEN 7
                                      WHEN java.time.Month.AUGUST THEN 8
                                      WHEN java.time.Month.SEPTEMBER THEN 9
                                      WHEN java.time.Month.OCTOBER THEN 10
                                      WHEN java.time.Month.NOVEMBER THEN 11
                                      WHEN java.time.Month.DECEMBER THEN 12
                                      ELSE null
                                  END)
                                  OR :activeEndMonthValue >= (CASE c.activeStartMonth
                                      WHEN java.time.Month.JANUARY THEN 1
                                      WHEN java.time.Month.FEBRUARY THEN 2
                                      WHEN java.time.Month.MARCH THEN 3
                                      WHEN java.time.Month.APRIL THEN 4
                                      WHEN java.time.Month.MAY THEN 5
                                      WHEN java.time.Month.JUNE THEN 6
                                      WHEN java.time.Month.JULY THEN 7
                                      WHEN java.time.Month.AUGUST THEN 8
                                      WHEN java.time.Month.SEPTEMBER THEN 9
                                      WHEN java.time.Month.OCTOBER THEN 10
                                      WHEN java.time.Month.NOVEMBER THEN 11
                                      WHEN java.time.Month.DECEMBER THEN 12
                                      ELSE null
                                  END)
                              )
                          )
                          OR
                          (
                              (CASE c.activeStartMonth
                                  WHEN java.time.Month.JANUARY THEN 1
                                  WHEN java.time.Month.FEBRUARY THEN 2
                                  WHEN java.time.Month.MARCH THEN 3
                                  WHEN java.time.Month.APRIL THEN 4
                                  WHEN java.time.Month.MAY THEN 5
                                  WHEN java.time.Month.JUNE THEN 6
                                  WHEN java.time.Month.JULY THEN 7
                                  WHEN java.time.Month.AUGUST THEN 8
                                  WHEN java.time.Month.SEPTEMBER THEN 9
                                  WHEN java.time.Month.OCTOBER THEN 10
                                  WHEN java.time.Month.NOVEMBER THEN 11
                                  WHEN java.time.Month.DECEMBER THEN 12
                                  ELSE null
                              END) <= (CASE c.activeEndMonth
                                  WHEN java.time.Month.JANUARY THEN 1
                                  WHEN java.time.Month.FEBRUARY THEN 2
                                  WHEN java.time.Month.MARCH THEN 3
                                  WHEN java.time.Month.APRIL THEN 4
                                  WHEN java.time.Month.MAY THEN 5
                                  WHEN java.time.Month.JUNE THEN 6
                                  WHEN java.time.Month.JULY THEN 7
                                  WHEN java.time.Month.AUGUST THEN 8
                                  WHEN java.time.Month.SEPTEMBER THEN 9
                                  WHEN java.time.Month.OCTOBER THEN 10
                                  WHEN java.time.Month.NOVEMBER THEN 11
                                  WHEN java.time.Month.DECEMBER THEN 12
                                  ELSE null
                              END)
                              AND :activeStartMonthValue > :activeEndMonthValue
                              AND (
                                  (CASE c.activeStartMonth
                                      WHEN java.time.Month.JANUARY THEN 1
                                      WHEN java.time.Month.FEBRUARY THEN 2
                                      WHEN java.time.Month.MARCH THEN 3
                                      WHEN java.time.Month.APRIL THEN 4
                                      WHEN java.time.Month.MAY THEN 5
                                      WHEN java.time.Month.JUNE THEN 6
                                      WHEN java.time.Month.JULY THEN 7
                                      WHEN java.time.Month.AUGUST THEN 8
                                      WHEN java.time.Month.SEPTEMBER THEN 9
                                      WHEN java.time.Month.OCTOBER THEN 10
                                      WHEN java.time.Month.NOVEMBER THEN 11
                                      WHEN java.time.Month.DECEMBER THEN 12
                                      ELSE null
                                  END) <= :activeEndMonthValue
                                  OR (CASE c.activeEndMonth
                                      WHEN java.time.Month.JANUARY THEN 1
                                      WHEN java.time.Month.FEBRUARY THEN 2
                                      WHEN java.time.Month.MARCH THEN 3
                                      WHEN java.time.Month.APRIL THEN 4
                                      WHEN java.time.Month.MAY THEN 5
                                      WHEN java.time.Month.JUNE THEN 6
                                      WHEN java.time.Month.JULY THEN 7
                                      WHEN java.time.Month.AUGUST THEN 8
                                      WHEN java.time.Month.SEPTEMBER THEN 9
                                      WHEN java.time.Month.OCTOBER THEN 10
                                      WHEN java.time.Month.NOVEMBER THEN 11
                                      WHEN java.time.Month.DECEMBER THEN 12
                                      ELSE null
                                  END) >= :activeStartMonthValue
                              )
                          )
                      )
                  )
              )
            """,
            countQuery = """
            SELECT COUNT(DISTINCT c)
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
              AND (
                  :applyActivePeriodFilter = false OR (
                      c.activeStartMonth IS NOT NULL
                      AND c.activeEndMonth IS NOT NULL
                      AND (
                          (
                              (CASE c.activeStartMonth
                                  WHEN java.time.Month.JANUARY THEN 1
                                  WHEN java.time.Month.FEBRUARY THEN 2
                                  WHEN java.time.Month.MARCH THEN 3
                                  WHEN java.time.Month.APRIL THEN 4
                                  WHEN java.time.Month.MAY THEN 5
                                  WHEN java.time.Month.JUNE THEN 6
                                  WHEN java.time.Month.JULY THEN 7
                                  WHEN java.time.Month.AUGUST THEN 8
                                  WHEN java.time.Month.SEPTEMBER THEN 9
                                  WHEN java.time.Month.OCTOBER THEN 10
                                  WHEN java.time.Month.NOVEMBER THEN 11
                                  WHEN java.time.Month.DECEMBER THEN 12
                                  ELSE null
                              END) <= (CASE c.activeEndMonth
                                  WHEN java.time.Month.JANUARY THEN 1
                                  WHEN java.time.Month.FEBRUARY THEN 2
                                  WHEN java.time.Month.MARCH THEN 3
                                  WHEN java.time.Month.APRIL THEN 4
                                  WHEN java.time.Month.MAY THEN 5
                                  WHEN java.time.Month.JUNE THEN 6
                                  WHEN java.time.Month.JULY THEN 7
                                  WHEN java.time.Month.AUGUST THEN 8
                                  WHEN java.time.Month.SEPTEMBER THEN 9
                                  WHEN java.time.Month.OCTOBER THEN 10
                                  WHEN java.time.Month.NOVEMBER THEN 11
                                  WHEN java.time.Month.DECEMBER THEN 12
                                  ELSE null
                              END)
                              AND :activeStartMonthValue <= :activeEndMonthValue
                              AND (CASE c.activeStartMonth
                                  WHEN java.time.Month.JANUARY THEN 1
                                  WHEN java.time.Month.FEBRUARY THEN 2
                                  WHEN java.time.Month.MARCH THEN 3
                                  WHEN java.time.Month.APRIL THEN 4
                                  WHEN java.time.Month.MAY THEN 5
                                  WHEN java.time.Month.JUNE THEN 6
                                  WHEN java.time.Month.JULY THEN 7
                                  WHEN java.time.Month.AUGUST THEN 8
                                  WHEN java.time.Month.SEPTEMBER THEN 9
                                  WHEN java.time.Month.OCTOBER THEN 10
                                  WHEN java.time.Month.NOVEMBER THEN 11
                                  WHEN java.time.Month.DECEMBER THEN 12
                                  ELSE null
                              END) <= :activeEndMonthValue
                              AND :activeStartMonthValue <= (CASE c.activeEndMonth
                                  WHEN java.time.Month.JANUARY THEN 1
                                  WHEN java.time.Month.FEBRUARY THEN 2
                                  WHEN java.time.Month.MARCH THEN 3
                                  WHEN java.time.Month.APRIL THEN 4
                                  WHEN java.time.Month.MAY THEN 5
                                  WHEN java.time.Month.JUNE THEN 6
                                  WHEN java.time.Month.JULY THEN 7
                                  WHEN java.time.Month.AUGUST THEN 8
                                  WHEN java.time.Month.SEPTEMBER THEN 9
                                  WHEN java.time.Month.OCTOBER THEN 10
                                  WHEN java.time.Month.NOVEMBER THEN 11
                                  WHEN java.time.Month.DECEMBER THEN 12
                                  ELSE null
                              END)
                          )
                          OR
                          (
                              (CASE c.activeStartMonth
                                  WHEN java.time.Month.JANUARY THEN 1
                                  WHEN java.time.Month.FEBRUARY THEN 2
                                  WHEN java.time.Month.MARCH THEN 3
                                  WHEN java.time.Month.APRIL THEN 4
                                  WHEN java.time.Month.MAY THEN 5
                                  WHEN java.time.Month.JUNE THEN 6
                                  WHEN java.time.Month.JULY THEN 7
                                  WHEN java.time.Month.AUGUST THEN 8
                                  WHEN java.time.Month.SEPTEMBER THEN 9
                                  WHEN java.time.Month.OCTOBER THEN 10
                                  WHEN java.time.Month.NOVEMBER THEN 11
                                  WHEN java.time.Month.DECEMBER THEN 12
                                  ELSE null
                              END) > (CASE c.activeEndMonth
                                  WHEN java.time.Month.JANUARY THEN 1
                                  WHEN java.time.Month.FEBRUARY THEN 2
                                  WHEN java.time.Month.MARCH THEN 3
                                  WHEN java.time.Month.APRIL THEN 4
                                  WHEN java.time.Month.MAY THEN 5
                                  WHEN java.time.Month.JUNE THEN 6
                                  WHEN java.time.Month.JULY THEN 7
                                  WHEN java.time.Month.AUGUST THEN 8
                                  WHEN java.time.Month.SEPTEMBER THEN 9
                                  WHEN java.time.Month.OCTOBER THEN 10
                                  WHEN java.time.Month.NOVEMBER THEN 11
                                  WHEN java.time.Month.DECEMBER THEN 12
                                  ELSE null
                              END)
                              AND :activeStartMonthValue > :activeEndMonthValue
                          )
                          OR
                          (
                              (CASE c.activeStartMonth
                                  WHEN java.time.Month.JANUARY THEN 1
                                  WHEN java.time.Month.FEBRUARY THEN 2
                                  WHEN java.time.Month.MARCH THEN 3
                                  WHEN java.time.Month.APRIL THEN 4
                                  WHEN java.time.Month.MAY THEN 5
                                  WHEN java.time.Month.JUNE THEN 6
                                  WHEN java.time.Month.JULY THEN 7
                                  WHEN java.time.Month.AUGUST THEN 8
                                  WHEN java.time.Month.SEPTEMBER THEN 9
                                  WHEN java.time.Month.OCTOBER THEN 10
                                  WHEN java.time.Month.NOVEMBER THEN 11
                                  WHEN java.time.Month.DECEMBER THEN 12
                                  ELSE null
                              END) > (CASE c.activeEndMonth
                                  WHEN java.time.Month.JANUARY THEN 1
                                  WHEN java.time.Month.FEBRUARY THEN 2
                                  WHEN java.time.Month.MARCH THEN 3
                                  WHEN java.time.Month.APRIL THEN 4
                                  WHEN java.time.Month.MAY THEN 5
                                  WHEN java.time.Month.JUNE THEN 6
                                  WHEN java.time.Month.JULY THEN 7
                                  WHEN java.time.Month.AUGUST THEN 8
                                  WHEN java.time.Month.SEPTEMBER THEN 9
                                  WHEN java.time.Month.OCTOBER THEN 10
                                  WHEN java.time.Month.NOVEMBER THEN 11
                                  WHEN java.time.Month.DECEMBER THEN 12
                                  ELSE null
                              END)
                              AND :activeStartMonthValue <= :activeEndMonthValue
                              AND (
                                  :activeStartMonthValue <= (CASE c.activeEndMonth
                                      WHEN java.time.Month.JANUARY THEN 1
                                      WHEN java.time.Month.FEBRUARY THEN 2
                                      WHEN java.time.Month.MARCH THEN 3
                                      WHEN java.time.Month.APRIL THEN 4
                                      WHEN java.time.Month.MAY THEN 5
                                      WHEN java.time.Month.JUNE THEN 6
                                      WHEN java.time.Month.JULY THEN 7
                                      WHEN java.time.Month.AUGUST THEN 8
                                      WHEN java.time.Month.SEPTEMBER THEN 9
                                      WHEN java.time.Month.OCTOBER THEN 10
                                      WHEN java.time.Month.NOVEMBER THEN 11
                                      WHEN java.time.Month.DECEMBER THEN 12
                                      ELSE null
                                  END)
                                  OR :activeEndMonthValue >= (CASE c.activeStartMonth
                                      WHEN java.time.Month.JANUARY THEN 1
                                      WHEN java.time.Month.FEBRUARY THEN 2
                                      WHEN java.time.Month.MARCH THEN 3
                                      WHEN java.time.Month.APRIL THEN 4
                                      WHEN java.time.Month.MAY THEN 5
                                      WHEN java.time.Month.JUNE THEN 6
                                      WHEN java.time.Month.JULY THEN 7
                                      WHEN java.time.Month.AUGUST THEN 8
                                      WHEN java.time.Month.SEPTEMBER THEN 9
                                      WHEN java.time.Month.OCTOBER THEN 10
                                      WHEN java.time.Month.NOVEMBER THEN 11
                                      WHEN java.time.Month.DECEMBER THEN 12
                                      ELSE null
                                  END)
                              )
                          )
                          OR
                          (
                              (CASE c.activeStartMonth
                                  WHEN java.time.Month.JANUARY THEN 1
                                  WHEN java.time.Month.FEBRUARY THEN 2
                                  WHEN java.time.Month.MARCH THEN 3
                                  WHEN java.time.Month.APRIL THEN 4
                                  WHEN java.time.Month.MAY THEN 5
                                  WHEN java.time.Month.JUNE THEN 6
                                  WHEN java.time.Month.JULY THEN 7
                                  WHEN java.time.Month.AUGUST THEN 8
                                  WHEN java.time.Month.SEPTEMBER THEN 9
                                  WHEN java.time.Month.OCTOBER THEN 10
                                  WHEN java.time.Month.NOVEMBER THEN 11
                                  WHEN java.time.Month.DECEMBER THEN 12
                                  ELSE null
                              END) <= (CASE c.activeEndMonth
                                  WHEN java.time.Month.JANUARY THEN 1
                                  WHEN java.time.Month.FEBRUARY THEN 2
                                  WHEN java.time.Month.MARCH THEN 3
                                  WHEN java.time.Month.APRIL THEN 4
                                  WHEN java.time.Month.MAY THEN 5
                                  WHEN java.time.Month.JUNE THEN 6
                                  WHEN java.time.Month.JULY THEN 7
                                  WHEN java.time.Month.AUGUST THEN 8
                                  WHEN java.time.Month.SEPTEMBER THEN 9
                                  WHEN java.time.Month.OCTOBER THEN 10
                                  WHEN java.time.Month.NOVEMBER THEN 11
                                  WHEN java.time.Month.DECEMBER THEN 12
                                  ELSE null
                              END)
                              AND :activeStartMonthValue > :activeEndMonthValue
                              AND (
                                  (CASE c.activeStartMonth
                                      WHEN java.time.Month.JANUARY THEN 1
                                      WHEN java.time.Month.FEBRUARY THEN 2
                                      WHEN java.time.Month.MARCH THEN 3
                                      WHEN java.time.Month.APRIL THEN 4
                                      WHEN java.time.Month.MAY THEN 5
                                      WHEN java.time.Month.JUNE THEN 6
                                      WHEN java.time.Month.JULY THEN 7
                                      WHEN java.time.Month.AUGUST THEN 8
                                      WHEN java.time.Month.SEPTEMBER THEN 9
                                      WHEN java.time.Month.OCTOBER THEN 10
                                      WHEN java.time.Month.NOVEMBER THEN 11
                                      WHEN java.time.Month.DECEMBER THEN 12
                                      ELSE null
                                  END) <= :activeEndMonthValue
                                  OR (CASE c.activeEndMonth
                                      WHEN java.time.Month.JANUARY THEN 1
                                      WHEN java.time.Month.FEBRUARY THEN 2
                                      WHEN java.time.Month.MARCH THEN 3
                                      WHEN java.time.Month.APRIL THEN 4
                                      WHEN java.time.Month.MAY THEN 5
                                      WHEN java.time.Month.JUNE THEN 6
                                      WHEN java.time.Month.JULY THEN 7
                                      WHEN java.time.Month.AUGUST THEN 8
                                      WHEN java.time.Month.SEPTEMBER THEN 9
                                      WHEN java.time.Month.OCTOBER THEN 10
                                      WHEN java.time.Month.NOVEMBER THEN 11
                                      WHEN java.time.Month.DECEMBER THEN 12
                                      ELSE null
                                  END) >= :activeStartMonthValue
                              )
                          )
                      )
                  )
              )
            """)
    Page<Course> filterCourses(
            @Param("courseTypes") List<CourseType> courseTypes,
            @Param("applyCourseTypeFilter") boolean applyCourseTypeFilter,
            @Param("ageGroups") List<AgeGroup> ageGroups,
            @Param("applyAgeGroupFilter") boolean applyAgeGroupFilter,
            @Param("minPrice") Float minPrice,
            @Param("maxPrice") Float maxPrice,
            @Param("recurrence") ScheduleRecurrence recurrence,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTimeFrom") LocalTime startTimeFrom,
            @Param("startTimeTo") LocalTime startTimeTo,
            @Param("activeStartMonthValue") Integer activeStartMonthValue,
            @Param("activeEndMonthValue") Integer activeEndMonthValue,
            @Param("applyActivePeriodFilter") boolean applyActivePeriodFilter,
            Pageable pageable
    );
}
