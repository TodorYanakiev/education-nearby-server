package com.dev.education_nearby_server.models.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregates the recurring slots and one-off special cases for a course.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class CourseSchedule {

    @ElementCollection
    @CollectionTable(name = "course_schedule_slots", joinColumns = @JoinColumn(name = "course_id"))
    @OrderColumn(name = "slot_order")
    private List<CourseScheduleSlot> slots = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "course_schedule_special_cases", joinColumns = @JoinColumn(name = "course_id"))
    @OrderColumn(name = "special_case_order")
    private List<CourseScheduleSpecialCase> specialCases = new ArrayList<>();
}
