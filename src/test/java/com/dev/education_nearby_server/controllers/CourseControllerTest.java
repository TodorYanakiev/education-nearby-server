package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.enums.AgeGroup;
import com.dev.education_nearby_server.enums.CourseType;
import com.dev.education_nearby_server.enums.ImageRole;
import com.dev.education_nearby_server.enums.ScheduleRecurrence;
import com.dev.education_nearby_server.models.dto.request.CourseFilterRequest;
import com.dev.education_nearby_server.models.dto.request.CourseImageRequest;
import com.dev.education_nearby_server.models.dto.request.CourseRequest;
import com.dev.education_nearby_server.models.dto.request.CourseUpdateRequest;
import com.dev.education_nearby_server.models.dto.response.CourseImageResponse;
import com.dev.education_nearby_server.models.dto.response.CourseResponse;
import com.dev.education_nearby_server.services.CourseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    void getCourseReturnsResponseFromService() {
        Long courseId = 4L;
        CourseResponse response = CourseResponse.builder()
                .id(courseId)
                .name("Course 4")
                .build();
        when(courseService.getCourseById(courseId)).thenReturn(response);

        ResponseEntity<CourseResponse> result = courseController.getCourse(courseId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
        verify(courseService).getCourseById(courseId);
    }

    @Test
    void getAllCoursesReturnsResponseFromService() {
        List<CourseResponse> responses = List.of(
                CourseResponse.builder().id(1L).name("Course 1").build(),
                CourseResponse.builder().id(2L).name("Course 2").build()
        );
        when(courseService.getAllCourses()).thenReturn(responses);

        ResponseEntity<List<CourseResponse>> result = courseController.getAllCourses();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
        verify(courseService).getAllCourses();
    }

    @Test
    void filterCoursesReturnsResponseFromService() {
        CourseFilterRequest request = CourseFilterRequest.builder()
                .courseTypes(List.of(CourseType.MUSIC, CourseType.SPORT))
                .minPrice(10.0f)
                .maxPrice(50.0f)
                .recurrence(ScheduleRecurrence.WEEKLY)
                .build();
        Page<CourseResponse> responses = new PageImpl<>(
                List.of(
                        CourseResponse.builder().id(3L).name("Music course").build(),
                        CourseResponse.builder().id(4L).name("Sport course").build()
                ),
                PageRequest.of(0, 9),
                2
        );
        when(courseService.filterCourses(request, 0, 9, Sort.unsorted())).thenReturn(responses);

        ResponseEntity<Page<CourseResponse>> result = courseController.filterCourses(request, 0, 9, Sort.unsorted());

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
        verify(courseService).filterCourses(request, 0, 9, Sort.unsorted());
    }

    @Test
    void getCoursesByLecturerReturnsResponseFromService() {
        Long lecturerId = 12L;
        List<CourseResponse> responses = List.of(
                CourseResponse.builder().id(5L).name("Course 5").build(),
                CourseResponse.builder().id(6L).name("Course 6").build()
        );
        when(courseService.getCoursesByLecturerId(lecturerId)).thenReturn(responses);

        ResponseEntity<List<CourseResponse>> result = courseController.getCoursesByLecturer(lecturerId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
        verify(courseService).getCoursesByLecturerId(lecturerId);
    }

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

    @Test
    void updateCourseReturnsResponseFromService() {
        Long courseId = 11L;
        CourseUpdateRequest request = CourseUpdateRequest.builder()
                .name("Updated course")
                .build();
        CourseResponse response = CourseResponse.builder()
                .id(courseId)
                .name("Updated course")
                .build();
        when(courseService.updateCourse(courseId, request)).thenReturn(response);

        ResponseEntity<CourseResponse> result = courseController.updateCourse(courseId, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
        verify(courseService).updateCourse(courseId, request);
    }

    @Test
    void addLecturerToCourseReturnsNoContent() {
        Long courseId = 20L;
        Long userId = 30L;

        ResponseEntity<Void> result = courseController.addLecturerToCourse(courseId, userId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.hasBody()).isFalse();
        verify(courseService).addLecturerToCourse(courseId, userId);
    }

    @Test
    void getCourseImagesReturnsResponseFromService() {
        Long courseId = 7L;
        List<CourseImageResponse> responses = List.of(
                CourseImageResponse.builder().id(1L).courseId(courseId).url("url-1").build(),
                CourseImageResponse.builder().id(2L).courseId(courseId).url("url-2").build()
        );
        when(courseService.getCourseImages(courseId)).thenReturn(responses);

        ResponseEntity<List<CourseImageResponse>> result = courseController.getCourseImages(courseId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
        verify(courseService).getCourseImages(courseId);
    }

    @Test
    void addCourseImageReturnsCreatedResponse() {
        Long courseId = 9L;
        CourseImageRequest request = CourseImageRequest.builder()
                .s3Key("key")
                .role(ImageRole.MAIN)
                .altText("alt")
                .width(100)
                .height(200)
                .build();
        CourseImageResponse response = CourseImageResponse.builder()
                .id(33L)
                .courseId(courseId)
                .url("https://example.com/image.png")
                .role(ImageRole.MAIN)
                .build();
        when(courseService.addCourseImage(courseId, request)).thenReturn(response);

        ResponseEntity<CourseImageResponse> result = courseController.addCourseImage(courseId, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
        verify(courseService).addCourseImage(courseId, request);
    }

    @Test
    void deleteCourseReturnsNoContent() {
        Long courseId = 15L;

        ResponseEntity<Void> result = courseController.deleteCourse(courseId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.hasBody()).isFalse();
        verify(courseService).deleteCourse(courseId);
    }

    @Test
    void deleteCourseImageReturnsNoContent() {
        Long courseId = 5L;
        Long imageId = 12L;

        ResponseEntity<Void> result = courseController.deleteCourseImage(courseId, imageId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.hasBody()).isFalse();
        verify(courseService).deleteCourseImage(courseId, imageId);
    }
}
