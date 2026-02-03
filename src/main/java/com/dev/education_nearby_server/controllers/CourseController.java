package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.models.dto.request.CourseImageRequest;
import com.dev.education_nearby_server.models.dto.request.CourseRequest;
import com.dev.education_nearby_server.models.dto.request.CourseUpdateRequest;
import com.dev.education_nearby_server.models.dto.request.CourseFilterRequest;
import com.dev.education_nearby_server.models.dto.response.CourseImageResponse;
import com.dev.education_nearby_server.models.dto.response.CourseResponse;
import com.dev.education_nearby_server.services.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for course listing, filtering, CRUD operations, and course image management.
 */
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    /**
     * Lists all courses without filtering.
     *
     * @return every course available to the caller
     */
    @GetMapping
    public ResponseEntity<List<CourseResponse>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    /**
     * Returns courses that match the provided optional filters; empty filters return all courses.
     *
     * @param request optional filter fields (category, price, etc.)
     * @param page zero-based page index
     * @param size page size
     * @return courses that satisfy the filters
     */
    @GetMapping("/filter")
    public ResponseEntity<Page<CourseResponse>> filterCourses(
            @Valid @ModelAttribute CourseFilterRequest request,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "9") Integer size,
            Sort sort
    ) {
        return ResponseEntity.ok(courseService.filterCourses(request, page, size, sort));
    }

    /**
     * Lists courses assigned to a lecturer.
     *
     * @param lecturerId lecturer identifier
     * @return courses taught by the lecturer
     */
    @GetMapping("/lecturers/{lecturerId}")
    public ResponseEntity<List<CourseResponse>> getCoursesByLecturer(@PathVariable Long lecturerId) {
        return ResponseEntity.ok(courseService.getCoursesByLecturerId(lecturerId));
    }

    /**
     * Fetches a course by id.
     *
     * @param courseId course identifier
     * @return course details
     */
    @GetMapping("/{courseId}")
    public ResponseEntity<CourseResponse> getCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourseById(courseId));
    }

    /**
     * Lists images attached to a course.
     *
     * @param courseId course identifier
     * @return images associated with the course
     */
    @GetMapping("/{courseId}/images")
    public ResponseEntity<List<CourseImageResponse>> getCourseImages(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourseImages(courseId));
    }

    /**
     * Creates a new course.
     *
     * @param request validated course payload
     * @return created course with generated id
     */
    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CourseRequest request) {
        CourseResponse response = courseService.createCourse(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing course.
     *
     * @param courseId course identifier
     * @param request fields to update
     * @return updated course
     */
    @PutMapping("/{courseId}")
    public ResponseEntity<CourseResponse> updateCourse(
            @PathVariable Long courseId,
            @RequestBody CourseUpdateRequest request
    ) {
        CourseResponse response = courseService.updateCourse(courseId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Adds a lecturer to a course.
     *
     * @param courseId course identifier
     * @param userId lecturer identifier
     * @return empty 204 on success
     */
    @PostMapping("/{courseId}/lecturers/{userId}")
    public ResponseEntity<Void> addLecturerToCourse(
            @PathVariable Long courseId,
            @PathVariable Long userId
    ) {
        courseService.addLecturerToCourse(courseId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Registers a new course image; validates S3 key/url and role before saving.
     *
     * @param courseId course identifier
     * @param request validated image payload
     * @return persisted course image
     */
    @PostMapping("/{courseId}/images")
    public ResponseEntity<CourseImageResponse> addCourseImage(
            @PathVariable Long courseId,
            @Valid @RequestBody CourseImageRequest request
    ) {
        CourseImageResponse response = courseService.addCourseImage(courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Deletes a course.
     *
     * @param courseId course identifier
     * @return empty 204 on success
     */
    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long courseId) {
        courseService.deleteCourse(courseId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deletes a course image by id.
     *
     * @param courseId course identifier
     * @param imageId image identifier
     * @return empty 204 on success
     */
    @DeleteMapping("/{courseId}/images/{imageId}")
    public ResponseEntity<Void> deleteCourseImage(
            @PathVariable Long courseId,
            @PathVariable Long imageId
    ) {
        courseService.deleteCourseImage(courseId, imageId);
        return ResponseEntity.noContent().build();
    }
}
