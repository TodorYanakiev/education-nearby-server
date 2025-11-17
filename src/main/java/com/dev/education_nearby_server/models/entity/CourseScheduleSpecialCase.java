package com.dev.education_nearby_server.models.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Captures a date that deviates from the recurring schedule.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class CourseScheduleSpecialCase {

    private LocalDate date;
/*[P2] Prevent NULL writes to classes_count — D:\Spring projects\EducationNearby\education-nearby-server\education-nearby-
    server\src\main\java\com\dev\education_nearby_server\models\entity\CourseScheduleSlot.java:38-44
    The Flyway migration V5__Add_Course_Module.sql declares course_schedule_slots.classes_count as NOT NULL DEFAULT 1 (lines 70‑78), but CourseScheduleSlot.classesCount (lines 38‑44
            of CourseScheduleSlot.java) is an uninitialized Integer. When a new slot is persisted without explicitly setting classesCount, Hibernate will bind NULL to that column, immediately
    violating the not‑null constraint and failing the insert. Please align the entity with the schema by giving the field a default value (e.g., private Integer classesCount = 1;) or
    by relaxing the database constraint.*/
    /**
     * When true, the lesson is skipped despite being on the schedule.
     */
    @Column(name = "is_cancelled")
    private boolean cancelled = true;

    private String reason;
}
