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

    /**
     * When true, the lesson is skipped despite being on the schedule.
     */
    @Column(name = "is_cancelled")
    private boolean cancelled = true;

    private String reason;
}
