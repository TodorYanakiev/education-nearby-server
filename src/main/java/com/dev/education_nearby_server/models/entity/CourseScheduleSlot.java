package com.dev.education_nearby_server.models.entity;

import com.dev.education_nearby_server.enums.ScheduleRecurrence;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Describes a recurring slot during which classes for a course are held.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class CourseScheduleSlot {

    @Enumerated(EnumType.STRING)
    private ScheduleRecurrence recurrence;

    /**
     * Used when recurrence is weekly.
     */
    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    /**
     * Used when recurrence is monthly.
     */
    private Integer dayOfMonth;

    private LocalTime startTime;

    /**
     * Number of consecutive classes during this slot.
     */
    private Integer classesCount;

    /**
     * Duration of a single class in minutes to allow precise filtering.
     */
    @Column(name = "single_class_duration_minutes")
    private Integer singleClassDurationMinutes;

    /**
     * Minutes between two consecutive classes in the same slot.
     */
    @Column(name = "gap_between_classes_minutes")
    private Integer gapBetweenClassesMinutes;
}
