package com.dev.education_nearby_server.models.dto.request;

import com.dev.education_nearby_server.enums.AgeGroup;
import com.dev.education_nearby_server.enums.CourseType;
import com.dev.education_nearby_server.models.entity.CourseSchedule;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Partial update payload for courses; null fields are ignored by the service layer.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CourseUpdateRequest {

    private String name;

    private String description;

    private CourseType type;

    private List<AgeGroup> ageGroupList;

    private CourseSchedule schedule;

    private String address;

    private Float price;

    private String facebookLink;

    private String websiteLink;

    private Long lyceumId;

    private String achievements;

    /**
     * When present, replaces the entire lecturer list.
     */
    private List<Long> lecturerIds;

    /**
     * Adds the provided lecturers to the existing list (ignored if {@code lecturerIds} is present).
     */
    private List<Long> lecturerIdsToAdd;

    /**
     * Removes the provided lecturers from the existing list (ignored if {@code lecturerIds} is present).
     */
    private List<Long> lecturerIdsToRemove;
}
