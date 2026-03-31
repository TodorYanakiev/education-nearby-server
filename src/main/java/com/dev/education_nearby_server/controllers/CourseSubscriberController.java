package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.enums.SubscriberExportFormat;
import com.dev.education_nearby_server.models.dto.response.SubscriberExportJobResponse;
import com.dev.education_nearby_server.models.dto.response.UserResponse;
import com.dev.education_nearby_server.services.CourseService;
import com.dev.education_nearby_server.services.SubscriberExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints for course subscriptions, subscribers listing, and subscribers exports.
 */
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Slf4j
public class CourseSubscriberController {

    private final CourseService courseService;
    private final SubscriberExportService subscriberExportService;

    /**
     * Subscribes the authenticated user to a course.
     *
     * @param courseId course identifier
     * @return empty 204 on success
     */
    @PostMapping("/{courseId}/subscribe")
    public ResponseEntity<Void> subscribeToCourse(@PathVariable Long courseId) {
        log.debug("Subscribe to course request received. courseId={}", courseId);
        courseService.subscribeToCourse(courseId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Unsubscribes the authenticated user from a course.
     *
     * @param courseId course identifier
     * @return empty 204 on success
     */
    @DeleteMapping("/{courseId}/subscribe")
    public ResponseEntity<Void> unsubscribeFromCourse(@PathVariable Long courseId) {
        log.debug("Unsubscribe from course request received. courseId={}", courseId);
        courseService.unsubscribeFromCourse(courseId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lists users subscribed to a course.
     *
     * @param courseId course identifier
     * @return subscribers associated with the course
     */
    @GetMapping("/{courseId}/subscribers")
    public ResponseEntity<List<UserResponse>> getCourseSubscribers(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourseSubscribers(courseId));
    }

    /**
     * Starts asynchronous export generation for course subscribers.
     *
     * @param courseId course identifier
     * @param format export format (CSV or XLSX)
     * @return export job metadata
     */
    @PostMapping("/{courseId}/subscribers/export")
    public ResponseEntity<SubscriberExportJobResponse> exportCourseSubscribers(
            @PathVariable Long courseId,
            @RequestParam SubscriberExportFormat format
    ) {
        SubscriberExportJobResponse response = subscriberExportService.createCourseSubscribersExport(courseId, format);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Returns status for a previously requested course subscriber export.
     *
     * @param courseId course identifier
     * @param exportId export job identifier
     * @return export job metadata
     */
    @GetMapping("/{courseId}/subscribers/export/{exportId}")
    public ResponseEntity<SubscriberExportJobResponse> getCourseSubscribersExportStatus(
            @PathVariable Long courseId,
            @PathVariable Long exportId
    ) {
        return ResponseEntity.ok(subscriberExportService.getCourseSubscribersExportStatus(courseId, exportId));
    }

    /**
     * Downloads generated course subscribers export file when the job is completed.
     *
     * @param courseId course identifier
     * @param exportId export job identifier
     * @return downloadable file response
     */
    @GetMapping("/{courseId}/subscribers/export/{exportId}/download")
    public ResponseEntity<Resource> downloadCourseSubscribersExport(
            @PathVariable Long courseId,
            @PathVariable Long exportId
    ) {
        SubscriberExportService.ExportFile exportFile = subscriberExportService.downloadCourseSubscribersExport(courseId, exportId);
        MediaType mediaType = MediaType.parseMediaType(exportFile.contentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportFile.fileName() + "\"")
                .body(exportFile.resource());
    }
}
