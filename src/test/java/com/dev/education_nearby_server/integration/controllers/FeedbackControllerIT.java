package com.dev.education_nearby_server.integration.controllers;

import com.dev.education_nearby_server.models.dto.request.FeedbackRequest;
import com.dev.education_nearby_server.models.dto.response.FeedbackResponse;
import com.dev.education_nearby_server.services.FeedbackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FeedbackControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FeedbackService feedbackService;

    @Test
    void createFeedbackDoesNotRequireAuthentication() throws Exception {
        FeedbackRequest request = FeedbackRequest.builder()
                .fullName("Ivan Petrov")
                .email("ivan.petrov@example.com")
                .title("Feature request")
                .message("I would like to suggest a feature.")
                .build();
        FeedbackResponse response = FeedbackResponse.builder()
                .id(88L)
                .fullName("Ivan Petrov")
                .email("ivan.petrov@example.com")
                .title("Feature request")
                .message("I would like to suggest a feature.")
                .createdAt(LocalDateTime.of(2026, 5, 7, 12, 0))
                .build();
        when(feedbackService.createFeedback(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(88L))
                .andExpect(jsonPath("$.fullName").value("Ivan Petrov"))
                .andExpect(jsonPath("$.email").value("ivan.petrov@example.com"))
                .andExpect(jsonPath("$.title").value("Feature request"))
                .andExpect(jsonPath("$.message").value("I would like to suggest a feature."))
                .andExpect(jsonPath("$.read").value(false));

        ArgumentCaptor<FeedbackRequest> captor = ArgumentCaptor.forClass(FeedbackRequest.class);
        verify(feedbackService).createFeedback(captor.capture());
        assertThat(captor.getValue().getFullName()).isEqualTo("Ivan Petrov");
        assertThat(captor.getValue().getEmail()).isEqualTo("ivan.petrov@example.com");
        assertThat(captor.getValue().getTitle()).isEqualTo("Feature request");
        assertThat(captor.getValue().getMessage()).isEqualTo("I would like to suggest a feature.");
    }

    @Test
    void createFeedbackValidatesPayloadBeforeService() throws Exception {
        FeedbackRequest request = FeedbackRequest.builder()
                .fullName(" ")
                .email("not-an-email")
                .title(" ")
                .message(" ")
                .build();

        mockMvc.perform(post("/api/v1/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(feedbackService);
    }

    @Test
    void getAllFeedbacksRequiresAuthenticationForAnonymousUsers() throws Exception {
        mockMvc.perform(get("/api/v1/feedback"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(feedbackService);
    }

    @Test
    void getAllFeedbacksRejectsNonAdminUsers() throws Exception {
        mockMvc.perform(get("/api/v1/feedback")
                        .with(user("member").roles("USER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(feedbackService);
    }

    @Test
    void getAllFeedbacksReturnsPayloadForAdmins() throws Exception {
        when(feedbackService.getAllFeedbacks(eq(1), eq(2), eq("unread"), any(Sort.class))).thenReturn(new PageImpl<>(List.of(
                FeedbackResponse.builder()
                        .id(2L)
                        .fullName("Maria Ivanova")
                        .email("maria@example.com")
                        .title("Second title")
                        .message("Second")
                        .read(false)
                        .build(),
                FeedbackResponse.builder()
                        .id(1L)
                        .fullName("Ivan Petrov")
                        .email("ivan@example.com")
                        .title("First title")
                        .message("First")
                        .read(true)
                        .build()
        ), PageRequest.of(1, 2), 4));

        mockMvc.perform(get("/api/v1/feedback")
                        .with(user("admin").roles("ADMIN"))
                        .param("page", "1")
                        .param("size", "2")
                        .param("filter", "unread")
                        .param("sort", "email,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(2L))
                .andExpect(jsonPath("$.content[0].title").value("Second title"))
                .andExpect(jsonPath("$.content[0].read").value(false))
                .andExpect(jsonPath("$.content[1].id").value(1L))
                .andExpect(jsonPath("$.content[1].title").value("First title"))
                .andExpect(jsonPath("$.content[1].read").value(true))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.number").value(1));

        verify(feedbackService).getAllFeedbacks(eq(1), eq(2), eq("unread"), any(Sort.class));
    }

    @Test
    void markFeedbackAsReadRequiresAdminRole() throws Exception {
        mockMvc.perform(patch("/api/v1/feedback/{feedbackId}/read", 44L)
                        .with(user("member").roles("USER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(feedbackService);
    }

    @Test
    void markFeedbackAsReadReturnsPayloadForAdmins() throws Exception {
        when(feedbackService.markFeedbackAsRead(44L)).thenReturn(FeedbackResponse.builder()
                .id(44L)
                .read(true)
                .build());

        mockMvc.perform(patch("/api/v1/feedback/{feedbackId}/read", 44L)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(44L))
                .andExpect(jsonPath("$.read").value(true));

        verify(feedbackService).markFeedbackAsRead(44L);
    }

    @Test
    void markFeedbackAsUnreadReturnsPayloadForAdmins() throws Exception {
        when(feedbackService.markFeedbackAsUnread(44L)).thenReturn(FeedbackResponse.builder()
                .id(44L)
                .read(false)
                .build());

        mockMvc.perform(patch("/api/v1/feedback/{feedbackId}/unread", 44L)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(44L))
                .andExpect(jsonPath("$.read").value(false));

        verify(feedbackService).markFeedbackAsUnread(44L);
    }

    @Test
    void deleteFeedbackReturnsNoContentForAdmins() throws Exception {
        mockMvc.perform(delete("/api/v1/feedback/{feedbackId}", 44L)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());

        verify(feedbackService).deleteFeedback(44L);
    }

    @Test
    void deleteFeedbackRejectsNonAdminUsers() throws Exception {
        mockMvc.perform(delete("/api/v1/feedback/{feedbackId}", 44L)
                        .with(user("member").roles("USER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(feedbackService);
    }
}
