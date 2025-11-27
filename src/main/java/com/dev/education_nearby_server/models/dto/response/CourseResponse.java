package com.dev.education_nearby_server.models.dto.response;

import com.dev.education_nearby_server.enums.AgeGroup;
import com.dev.education_nearby_server.enums.CourseType;
import com.dev.education_nearby_server.models.entity.CourseSchedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Representation of a course returned to API consumers.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {
    private Long id;
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
    private List<Long> lecturerIds;
}
