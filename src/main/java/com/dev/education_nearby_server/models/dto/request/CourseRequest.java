package com.dev.education_nearby_server.models.dto.request;

import com.dev.education_nearby_server.enums.AgeGroup;
import com.dev.education_nearby_server.enums.CourseExecutionType;
import com.dev.education_nearby_server.enums.CourseType;
import com.dev.education_nearby_server.models.entity.CourseSchedule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Month;
import java.util.List;

/**
 * Payload for creating a course with required metadata and optional associations.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRequest {

    @NotBlank(message = "Course name must not be blank.")
    private String name;

    @NotBlank(message = "Course description must not be blank.")
    private String description;

    @NotNull(message = "Course type must not be null.")
    private CourseType type;

    private CourseExecutionType executionType;

    @NotNull(message = "Age group list must not be null.")
    @Size(min = 1, message = "At least one age group must be specified.")
    private List<AgeGroup> ageGroupList;

    private CourseSchedule schedule;

    private String address;

    private Float price;

    private String facebookLink;

    private String websiteLink;

    private Long lyceumId;

    private String achievements;

    private Month activeStartMonth;

    private Month activeEndMonth;

    private List<Long> lecturerIds;
}
