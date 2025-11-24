package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.models.dto.request.CourseImageRequest;
import com.dev.education_nearby_server.models.dto.request.CourseRequest;
import com.dev.education_nearby_server.models.dto.request.CourseUpdateRequest;
import com.dev.education_nearby_server.models.dto.response.CourseImageResponse;
import com.dev.education_nearby_server.models.dto.response.CourseResponse;
import com.dev.education_nearby_server.services.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<List<CourseResponse>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<CourseResponse> getCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourseById(courseId));
    }

    @GetMapping("/{courseId}/images")
    public ResponseEntity<List<CourseImageResponse>> getCourseImages(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourseImages(courseId));
    }

    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CourseRequest request) {
        CourseResponse response = courseService.createCourse(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{courseId}")
    public ResponseEntity<CourseResponse> updateCourse(
            @PathVariable Long courseId,
            @RequestBody CourseUpdateRequest request
    ) {
        CourseResponse response = courseService.updateCourse(courseId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{courseId}/images")
    public ResponseEntity<CourseImageResponse> addCourseImage(
            @PathVariable Long courseId,
            @Valid @RequestBody CourseImageRequest request
    ) {
        CourseImageResponse response = courseService.addCourseImage(courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long courseId) {
        courseService.deleteCourse(courseId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{courseId}/images/{imageId}")
    public ResponseEntity<Void> deleteCourseImage(
            @PathVariable Long courseId,
            @PathVariable Long imageId
    ) {
        courseService.deleteCourseImage(courseId, imageId);
        return ResponseEntity.noContent().build();
    }
}
