package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.models.dto.request.LyceumRightsRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumRightsVerificationRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumLecturerRequest;
import com.dev.education_nearby_server.models.dto.response.CourseResponse;
import com.dev.education_nearby_server.models.dto.response.LyceumResponse;
import com.dev.education_nearby_server.models.dto.response.UserResponse;
import com.dev.education_nearby_server.services.LyceumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints for browsing, managing, and moderating lyceums.
 */
@RestController
@RequestMapping("/api/v1/lyceums")
@RequiredArgsConstructor
public class LyceumController {

    private final LyceumService lyceumService;

    /**
     * Returns all lyceums regardless of verification status.
     *
     * @return list of lyceums
     */
    @GetMapping
    public ResponseEntity<List<LyceumResponse>> getAllLyceums() {
        return ResponseEntity.ok(lyceumService.getAllLyceums());
    }

    /**
     * Returns only lyceums that were verified by administrators.
     *
     * @return list of verified lyceums
     */
    @GetMapping("/verified")
    public ResponseEntity<List<LyceumResponse>> getVerifiedLyceums() {
        return ResponseEntity.ok(lyceumService.getVerifiedLyceums());
    }

    /**
     * Fetches a single lyceum by id.
     *
     * @param id lyceum identifier
     * @return lyceum details
     */
    @GetMapping("/{id}")
    public ResponseEntity<LyceumResponse> getLyceumById(@PathVariable Long id) {
        return ResponseEntity.ok(lyceumService.getLyceumById(id));
    }

    /**
     * Lists courses for a specific lyceum.
     *
     * @param lyceumId lyceum identifier
     * @return courses offered by the lyceum
     */
    @GetMapping("/{lyceumId}/courses")
    public ResponseEntity<List<CourseResponse>> getLyceumCourses(@PathVariable Long lyceumId) {
        return ResponseEntity.ok(lyceumService.getLyceumCourses(lyceumId));
    }

    /**
     * Fetches lyceums that match the provided ids.
     *
     * @param ids list of lyceum identifiers
     * @return lyceums with matching ids
     */
    @GetMapping("/by-ids")
    public ResponseEntity<List<LyceumResponse>> getLyceumsByIds(@RequestParam List<Long> ids) {
        return ResponseEntity.ok(lyceumService.getLyceumsByIds(ids));
    }

    /**
     * Filters lyceums by location and pagination.
     *
     * @param town optional town name
     * @param latitude optional latitude for geo filter
     * @param longitude optional longitude for geo filter
     * @param limit optional max number of results
     * @return lyceums matching the supplied filters
     */
    @GetMapping("/filter")
    public ResponseEntity<List<LyceumResponse>> filterLyceums(
            @RequestParam(required = false) String town,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(lyceumService.filterLyceums(town, latitude, longitude, limit));
    }

    /**
     * Starts the verification flow by requesting rights over a lyceum.
     *
     * @param request metadata about the lyceum and requester
     * @return confirmation message
     */
    @PostMapping("/request-rights")
    public ResponseEntity<String> requestRightsOverLyceum(@Valid @RequestBody LyceumRightsRequest request) {
        return ResponseEntity.ok(lyceumService.requestRightsOverLyceum(request));
    }

    /**
     * Creates a new lyceum entry.
     *
     * @param request lyceum details to persist
     * @return created lyceum with generated id
     */
    @PostMapping
    public ResponseEntity<LyceumResponse> createLyceum(@Valid @RequestBody LyceumRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lyceumService.createLyceum(request));
    }

    /**
     * Verifies a previously requested right over a lyceum.
     *
     * @param request verification payload (e.g., codes or proofs)
     * @return confirmation message
     */
    @PostMapping("/verify-rights")
    public ResponseEntity<String> verifyRightsOverLyceum(@Valid @RequestBody LyceumRightsVerificationRequest request) {
        return ResponseEntity.ok(lyceumService.verifyRightsOverLyceum(request));
    }

    /**
     * Updates an existing lyceum.
     *
     * @param id lyceum identifier
     * @param request updated lyceum fields
     * @return updated lyceum data
     */
    @PutMapping("/{id}")
    public ResponseEntity<LyceumResponse> updateLyceum(
            @PathVariable Long id,
            @Valid @RequestBody LyceumRequest request
    ) {
        return ResponseEntity.ok(lyceumService.updateLyceum(id, request));
    }

    /**
     * Assigns a user as an administrator of a lyceum.
     *
     * @param lyceumId lyceum identifier
     * @param userId user identifier to promote
     * @return empty 204 on success
     */
    @PutMapping("/{lyceumId}/administrators/{userId}")
    public ResponseEntity<Void> assignAdministrator(
            @PathVariable Long lyceumId,
            @PathVariable Long userId
    ) {
        lyceumService.assignAdministrator(lyceumId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Adds a lecturer to a lyceum.
     *
     * @param request lecturer details and target lyceum
     * @return empty 204 on success
     */
    @PostMapping("/lecturers")
    public ResponseEntity<Void> addLecturer(@Valid @RequestBody LyceumLecturerRequest request) {
        lyceumService.addLecturerToLyceum(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Removes a lecturer from a lyceum.
     *
     * @param lyceumId lyceum identifier
     * @param userId lecturer identifier
     * @return empty 204 on success
     */
    @DeleteMapping("/{lyceumId}/lecturers/{userId}")
    public ResponseEntity<Void> removeLecturer(
            @PathVariable Long lyceumId,
            @PathVariable Long userId
    ) {
        lyceumService.removeLecturerFromLyceum(lyceumId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lists lecturers associated with a lyceum.
     *
     * @param lyceumId lyceum identifier
     * @return lecturers assigned to the lyceum
     */
    @GetMapping("/{lyceumId}/lecturers")
    public ResponseEntity<List<UserResponse>> getLyceumLecturers(@PathVariable Long lyceumId) {
        return ResponseEntity.ok(lyceumService.getLyceumLecturers(lyceumId));
    }

    /**
     * Lists administrators associated with a lyceum.
     *
     * @param lyceumId lyceum identifier
     * @return administrators assigned to the lyceum
     */
    @GetMapping("/{lyceumId}/admins")
    public ResponseEntity<List<UserResponse>> getLyceumAdministrators(@PathVariable Long lyceumId) {
        return ResponseEntity.ok(lyceumService.getLyceumAdministrators(lyceumId));
    }

    /**
     * Deletes a lyceum; only administrators are allowed.
     *
     * @param id lyceum identifier
     * @return empty 204 on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteLyceum(@PathVariable Long id) {
        lyceumService.deleteLyceum(id);
        return ResponseEntity.noContent().build();
    }
}
