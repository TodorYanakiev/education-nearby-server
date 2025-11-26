package com.dev.education_nearby_server.models.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregates the recurring slots and one-off exceptions for a course.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class CourseSchedule implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @ElementCollection
    @CollectionTable(name = "course_schedule_slots", joinColumns = @JoinColumn(name = "course_id"))
    @OrderColumn(name = "slot_order")
    private List<CourseScheduleSlot> slots = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "course_schedule_special_cases", joinColumns = @JoinColumn(name = "course_id"))
    @OrderColumn(name = "special_case_order")
    @JsonAlias("exceptions")
    private List<CourseScheduleSpecialCase> specialCases = new ArrayList<>();
}
