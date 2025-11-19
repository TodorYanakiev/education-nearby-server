package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.enums.AgeGroup;
import com.dev.education_nearby_server.enums.CourseType;
import com.dev.education_nearby_server.models.dto.request.CourseRequest;
import com.dev.education_nearby_server.models.dto.response.CourseResponse;
import com.dev.education_nearby_server.services.CourseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    @Mock
    private CourseService courseService;

    @InjectMocks
    private CourseController courseController;

    @Test
    void createCourseReturnsCreatedResponse() {
        CourseRequest request = CourseRequest.builder()
                .name("Course")
                .description("Description")
                .type(CourseType.MUSIC)
                .ageGroupList(List.of(AgeGroup.ADULT))
                .lyceumId(2L)
                .lecturerIds(List.of(5L, 6L))
                .build();

        CourseResponse response = CourseResponse.builder()
                .id(10L)
                .name("Course")
                .build();

        when(courseService.createCourse(request)).thenReturn(response);

        ResponseEntity<CourseResponse> result = courseController.createCourse(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
        verify(courseService).createCourse(request);
    }
}
