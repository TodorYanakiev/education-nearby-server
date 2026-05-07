package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.models.dto.request.FeedbackRequest;
import com.dev.education_nearby_server.models.dto.response.FeedbackResponse;
import com.dev.education_nearby_server.services.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Public feedback form endpoint.
 */
@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
@Slf4j
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * Lists all feedback submissions for global admins.
     *
     * @param page zero-based page index
     * @param size page size
     * @param filter read-state filter: all, read, or unread
     * @param sort sorting configuration
     * @return paginated feedback submissions
     */
    @GetMapping
    public ResponseEntity<Page<FeedbackResponse>> getAllFeedbacks(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "9") Integer size,
            @RequestParam(defaultValue = "all") String filter,
            Sort sort
    ) {
        return ResponseEntity.ok(feedbackService.getAllFeedbacks(page, size, filter, sort));
    }

    /**
     * Creates a feedback submission from an unauthenticated visitor.
     *
     * @param request validated feedback payload
     * @return created feedback submission
     */
    @PostMapping
    public ResponseEntity<FeedbackResponse> createFeedback(@Valid @RequestBody FeedbackRequest request) {
        log.debug("Feedback submission request received.");
        FeedbackResponse response = feedbackService.createFeedback(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Marks a feedback submission as read.
     *
     * @param feedbackId feedback identifier
     * @return updated feedback submission
     */
    @PatchMapping("/{feedbackId}/read")
    public ResponseEntity<FeedbackResponse> markFeedbackAsRead(@PathVariable Long feedbackId) {
        return ResponseEntity.ok(feedbackService.markFeedbackAsRead(feedbackId));
    }

    /**
     * Marks a feedback submission as unread.
     *
     * @param feedbackId feedback identifier
     * @return updated feedback submission
     */
    @PatchMapping("/{feedbackId}/unread")
    public ResponseEntity<FeedbackResponse> markFeedbackAsUnread(@PathVariable Long feedbackId) {
        return ResponseEntity.ok(feedbackService.markFeedbackAsUnread(feedbackId));
    }

    /**
     * Deletes a feedback submission.
     *
     * @param feedbackId feedback identifier
     * @return empty 204 on success
     */
    @DeleteMapping("/{feedbackId}")
    public ResponseEntity<Void> deleteFeedback(@PathVariable Long feedbackId) {
        feedbackService.deleteFeedback(feedbackId);
        return ResponseEntity.noContent().build();
    }
}
