package com.dev.education_nearby_server.models.dto.request;

import com.dev.education_nearby_server.enums.AgeGroup;
import com.dev.education_nearby_server.enums.CourseType;
import com.dev.education_nearby_server.enums.ScheduleRecurrence;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Query parameters for filtering courses by type, age group, price, recurrence, and schedule.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseFilterRequest {

    /**
     * Optional list of course types to include. When empty or missing, no type filter is applied.
     */
    private List<CourseType> courseTypes;

    /**
     * Optional list of age groups to include. When empty or missing, no age group filter is applied.
     */
    private List<AgeGroup> ageGroups;

    @PositiveOrZero(message = "Minimum price cannot be negative.")
    private Float minPrice;

    @PositiveOrZero(message = "Maximum price cannot be negative.")
    private Float maxPrice;

    private ScheduleRecurrence recurrence;

    private DayOfWeek dayOfWeek;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime startTimeFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime startTimeTo;
}
