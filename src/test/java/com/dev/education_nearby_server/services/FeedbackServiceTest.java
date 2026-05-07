package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.exceptions.common.BadRequestException;
import com.dev.education_nearby_server.exceptions.common.NoSuchElementException;
import com.dev.education_nearby_server.models.dto.request.FeedbackRequest;
import com.dev.education_nearby_server.models.dto.response.FeedbackResponse;
import com.dev.education_nearby_server.models.entity.Feedback;
import com.dev.education_nearby_server.repositories.FeedbackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @InjectMocks
    private FeedbackService feedbackService;

    @Test
    void getAllFeedbacksReturnsMappedResponses() {
        Feedback unreadFeedback = buildFeedback(2L, "Maria Ivanova", "maria@example.com", "Second title", "Second", false);
        Feedback readFeedback = buildFeedback(1L, "Ivan Petrov", "ivan@example.com", "First title", "First", true);
        when(feedbackRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(unreadFeedback, readFeedback)));

        Page<FeedbackResponse> responses = feedbackService.getAllFeedbacks(0, 9, "all", Sort.unsorted());

        assertThat(responses.getContent()).hasSize(2);
        assertThat(responses.getContent().get(0).getId()).isEqualTo(2L);
        assertThat(responses.getContent().get(0).getTitle()).isEqualTo("Second title");
        assertThat(responses.getContent().get(0).isRead()).isFalse();
        assertThat(responses.getContent().get(1).getId()).isEqualTo(1L);
        assertThat(responses.getContent().get(1).getTitle()).isEqualTo("First title");
        assertThat(responses.getContent().get(1).isRead()).isTrue();
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(feedbackRepository).findAll(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(9);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getAllFeedbacksFiltersUnreadWithRequestedSort() {
        Feedback unreadFeedback = buildFeedback(2L, "Maria Ivanova", "maria@example.com", "Second title", "Second", false);
        when(feedbackRepository.findAllByRead(anyBoolean(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(unreadFeedback)));
        Sort sort = Sort.by(Sort.Direction.ASC, "email");

        Page<FeedbackResponse> responses = feedbackService.getAllFeedbacks(1, 3, "unread", sort);

        assertThat(responses.getContent()).hasSize(1);
        assertThat(responses.getContent().getFirst().isRead()).isFalse();
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(feedbackRepository).findAllByRead(eq(false), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(3);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("email")).isNotNull();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("email").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void getAllFeedbacksFiltersRead() {
        Feedback readFeedback = buildFeedback(1L, "Ivan Petrov", "ivan@example.com", "First title", "First", true);
        when(feedbackRepository.findAllByRead(anyBoolean(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(readFeedback)));

        Page<FeedbackResponse> responses = feedbackService.getAllFeedbacks(0, 2, "READ", Sort.by("createdAt"));

        assertThat(responses.getContent()).hasSize(1);
        assertThat(responses.getContent().getFirst().isRead()).isTrue();
        verify(feedbackRepository).findAllByRead(anyBoolean(), any(Pageable.class));
    }

    @Test
    void getAllFeedbacksThrowsWhenFilterUnsupported() {
        assertThrows(BadRequestException.class, () ->
                feedbackService.getAllFeedbacks(0, 9, "archived", Sort.unsorted())
        );
    }

    @Test
    void getAllFeedbacksThrowsWhenSortUnsupported() {
        assertThrows(BadRequestException.class, () ->
                feedbackService.getAllFeedbacks(0, 9, "all", Sort.by("message"))
        );
    }

    @Test
    void getAllFeedbacksThrowsWhenPageInvalid() {
        assertThrows(BadRequestException.class, () ->
                feedbackService.getAllFeedbacks(-1, 9, "all", Sort.unsorted())
        );
    }

    @Test
    void createFeedbackTrimsAndSavesSubmission() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 7, 11, 15);
        FeedbackRequest request = FeedbackRequest.builder()
                .fullName("  Maria Ivanova  ")
                .email("  maria.ivanova@example.com  ")
                .title("  Course filters  ")
                .message("  This is useful feedback.  ")
                .build();
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> {
            Feedback saved = invocation.getArgument(0);
            saved.setId(9L);
            saved.setCreatedAt(createdAt);
            return saved;
        });

        FeedbackResponse response = feedbackService.createFeedback(request);

        ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).save(captor.capture());
        assertThat(captor.getValue().getFullName()).isEqualTo("Maria Ivanova");
        assertThat(captor.getValue().getEmail()).isEqualTo("maria.ivanova@example.com");
        assertThat(captor.getValue().getTitle()).isEqualTo("Course filters");
        assertThat(captor.getValue().getMessage()).isEqualTo("This is useful feedback.");
        assertThat(captor.getValue().isRead()).isFalse();
        assertThat(response.getId()).isEqualTo(9L);
        assertThat(response.getFullName()).isEqualTo("Maria Ivanova");
        assertThat(response.getEmail()).isEqualTo("maria.ivanova@example.com");
        assertThat(response.getTitle()).isEqualTo("Course filters");
        assertThat(response.getMessage()).isEqualTo("This is useful feedback.");
        assertThat(response.isRead()).isFalse();
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void createFeedbackThrowsWhenRequestNull() {
        assertThrows(BadRequestException.class, () -> feedbackService.createFeedback(null));

        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    @Test
    void createFeedbackThrowsWhenMessageBlank() {
        FeedbackRequest request = FeedbackRequest.builder()
                .fullName("Ivan Petrov")
                .email("ivan.petrov@example.com")
                .title("Course filters")
                .message("   ")
                .build();

        assertThrows(BadRequestException.class, () -> feedbackService.createFeedback(request));

        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    @Test
    void markFeedbackAsReadSetsReadFlag() {
        Feedback feedback = buildFeedback(5L, "Ivan Petrov", "ivan@example.com", "Title", "Message", false);
        when(feedbackRepository.findById(5L)).thenReturn(Optional.of(feedback));
        when(feedbackRepository.save(feedback)).thenReturn(feedback);

        FeedbackResponse response = feedbackService.markFeedbackAsRead(5L);

        assertThat(feedback.isRead()).isTrue();
        assertThat(response.isRead()).isTrue();
        verify(feedbackRepository).save(feedback);
    }

    @Test
    void markFeedbackAsUnreadClearsReadFlag() {
        Feedback feedback = buildFeedback(5L, "Ivan Petrov", "ivan@example.com", "Title", "Message", true);
        when(feedbackRepository.findById(5L)).thenReturn(Optional.of(feedback));
        when(feedbackRepository.save(feedback)).thenReturn(feedback);

        FeedbackResponse response = feedbackService.markFeedbackAsUnread(5L);

        assertThat(feedback.isRead()).isFalse();
        assertThat(response.isRead()).isFalse();
        verify(feedbackRepository).save(feedback);
    }

    @Test
    void deleteFeedbackDeletesExistingSubmission() {
        Feedback feedback = buildFeedback(7L, "Ivan Petrov", "ivan@example.com", "Title", "Message", false);
        when(feedbackRepository.findById(7L)).thenReturn(Optional.of(feedback));

        feedbackService.deleteFeedback(7L);

        verify(feedbackRepository).delete(feedback);
    }

    @Test
    void markFeedbackAsReadThrowsWhenIdNull() {
        assertThrows(BadRequestException.class, () -> feedbackService.markFeedbackAsRead(null));

        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    @Test
    void markFeedbackAsReadThrowsWhenMissing() {
        when(feedbackRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> feedbackService.markFeedbackAsRead(99L));

        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    private Feedback buildFeedback(Long id, String fullName, String email, String title, String message, boolean read) {
        return Feedback.builder()
                .id(id)
                .fullName(fullName)
                .email(email)
                .title(title)
                .message(message)
                .read(read)
                .createdAt(LocalDateTime.of(2026, 5, 7, 12, 0))
                .build();
    }
}
