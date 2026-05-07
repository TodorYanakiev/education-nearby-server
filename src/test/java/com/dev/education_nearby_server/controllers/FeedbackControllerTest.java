package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.models.dto.request.FeedbackRequest;
import com.dev.education_nearby_server.models.dto.response.FeedbackResponse;
import com.dev.education_nearby_server.services.FeedbackService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackControllerTest {

    @Mock
    private FeedbackService feedbackService;

    @InjectMocks
    private FeedbackController feedbackController;

    @Test
    void getAllFeedbacksReturnsServicePayload() {
        List<FeedbackResponse> responses = List.of(
                FeedbackResponse.builder().id(2L).fullName("Second User").read(false).build(),
                FeedbackResponse.builder().id(1L).fullName("First User").read(true).build()
        );
        Page<FeedbackResponse> page = new PageImpl<>(responses);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        when(feedbackService.getAllFeedbacks(0, 9, "all", sort)).thenReturn(page);

        ResponseEntity<Page<FeedbackResponse>> result = feedbackController.getAllFeedbacks(0, 9, "all", sort);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(page);
        verify(feedbackService).getAllFeedbacks(0, 9, "all", sort);
    }

    @Test
    void createFeedbackReturnsCreatedResponse() {
        FeedbackRequest request = FeedbackRequest.builder()
                .fullName("Ivan Petrov")
                .email("ivan.petrov@example.com")
                .title("Course filters")
                .message("Please add more course filters.")
                .build();
        FeedbackResponse response = FeedbackResponse.builder()
                .id(42L)
                .fullName("Ivan Petrov")
                .email("ivan.petrov@example.com")
                .title("Course filters")
                .message("Please add more course filters.")
                .createdAt(LocalDateTime.of(2026, 5, 7, 10, 30))
                .build();
        when(feedbackService.createFeedback(request)).thenReturn(response);

        ResponseEntity<FeedbackResponse> result = feedbackController.createFeedback(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
        verify(feedbackService).createFeedback(request);
    }

    @Test
    void markFeedbackAsReadReturnsServicePayload() {
        FeedbackResponse response = FeedbackResponse.builder()
                .id(42L)
                .read(true)
                .build();
        when(feedbackService.markFeedbackAsRead(42L)).thenReturn(response);

        ResponseEntity<FeedbackResponse> result = feedbackController.markFeedbackAsRead(42L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
        verify(feedbackService).markFeedbackAsRead(42L);
    }

    @Test
    void markFeedbackAsUnreadReturnsServicePayload() {
        FeedbackResponse response = FeedbackResponse.builder()
                .id(42L)
                .read(false)
                .build();
        when(feedbackService.markFeedbackAsUnread(42L)).thenReturn(response);

        ResponseEntity<FeedbackResponse> result = feedbackController.markFeedbackAsUnread(42L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
        verify(feedbackService).markFeedbackAsUnread(42L);
    }

    @Test
    void deleteFeedbackReturnsNoContent() {
        ResponseEntity<Void> result = feedbackController.deleteFeedback(42L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.hasBody()).isFalse();
        verify(feedbackService).deleteFeedback(42L);
    }
}
