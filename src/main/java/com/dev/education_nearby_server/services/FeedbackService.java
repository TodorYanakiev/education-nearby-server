package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.enums.FeedbackReadFilter;
import com.dev.education_nearby_server.exceptions.common.BadRequestException;
import com.dev.education_nearby_server.exceptions.common.NoSuchElementException;
import com.dev.education_nearby_server.models.dto.request.FeedbackRequest;
import com.dev.education_nearby_server.models.dto.response.FeedbackResponse;
import com.dev.education_nearby_server.models.entity.Feedback;
import com.dev.education_nearby_server.repositories.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * Handles public feedback submissions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "fullName", "email", "title", "read", "createdAt");

    private final FeedbackRepository feedbackRepository;

    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getAllFeedbacks(Integer page, Integer size, String filter, Sort sort) {
        validatePageRequest(page, size);

        FeedbackReadFilter readFilter = FeedbackReadFilter.from(filter);
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));
        Page<Feedback> feedbacks = switch (readFilter) {
            case READ -> feedbackRepository.findAllByRead(true, pageable);
            case UNREAD -> feedbackRepository.findAllByRead(false, pageable);
            case ALL -> feedbackRepository.findAll(pageable);
        };

        return feedbacks.map(this::mapToResponse);
    }

    @Transactional
    public FeedbackResponse createFeedback(FeedbackRequest request) {
        FeedbackRequest payload = requireFeedbackRequest(request);
        Feedback feedback = Feedback.builder()
                .fullName(requireText(payload.getFullName(), "Full name must not be blank."))
                .email(requireText(payload.getEmail(), "Email must not be blank."))
                .title(requireText(payload.getTitle(), "Title must not be blank."))
                .message(requireText(payload.getMessage(), "Message must not be blank."))
                .build();

        Feedback saved = feedbackRepository.save(feedback);
        log.info("Created feedback submission. feedbackId={}", saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    public FeedbackResponse markFeedbackAsRead(Long feedbackId) {
        Feedback feedback = requireFeedback(feedbackId);
        feedback.setRead(true);
        Feedback saved = feedbackRepository.save(feedback);
        log.info("Marked feedback as read. feedbackId={}", saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    public FeedbackResponse markFeedbackAsUnread(Long feedbackId) {
        Feedback feedback = requireFeedback(feedbackId);
        feedback.setRead(false);
        Feedback saved = feedbackRepository.save(feedback);
        log.info("Marked feedback as unread. feedbackId={}", saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteFeedback(Long feedbackId) {
        Feedback feedback = requireFeedback(feedbackId);
        feedbackRepository.delete(feedback);
        log.info("Deleted feedback. feedbackId={}", feedbackId);
    }

    private FeedbackRequest requireFeedbackRequest(FeedbackRequest request) {
        if (request == null) {
            throw new BadRequestException("Feedback payload must not be null.");
        }
        return request;
    }

    private Feedback requireFeedback(Long feedbackId) {
        if (feedbackId == null) {
            throw new BadRequestException("Feedback id must be provided.");
        }
        return feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Feedback with id " + feedbackId + " not found."
                ));
    }

    private void validatePageRequest(Integer page, Integer size) {
        if (page == null || page < 0) {
            throw new BadRequestException("Page index must be zero or positive.");
        }
        if (size == null || size <= 0) {
            throw new BadRequestException("Page size must be greater than zero.");
        }
    }

    private Sort resolveSort(Sort sort) {
        Sort resolved = (sort == null || sort.isUnsorted()) ? Sort.by(Sort.Direction.DESC, "createdAt") : sort;
        for (Sort.Order order : resolved) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException("Sorting by '" + order.getProperty() + "' is not supported.");
            }
        }
        return resolved;
    }

    private String requireText(String value, String message) {
        String trimmed = value == null ? null : value.trim();
        if (!StringUtils.hasText(trimmed)) {
            throw new BadRequestException(message);
        }
        return trimmed;
    }

    private FeedbackResponse mapToResponse(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .fullName(feedback.getFullName())
                .email(feedback.getEmail())
                .title(feedback.getTitle())
                .message(feedback.getMessage())
                .read(feedback.isRead())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}
