package com.dev.education_nearby_server.integration.controllers;

import com.dev.education_nearby_server.enums.AgeGroup;
import com.dev.education_nearby_server.enums.CourseType;
import com.dev.education_nearby_server.models.dto.request.CourseRequest;
import com.dev.education_nearby_server.models.dto.response.CourseResponse;
import com.dev.education_nearby_server.services.CourseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CourseControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CourseService courseService;

    @Test
    void createCourseRequiresAuthentication() throws Exception {
        CourseRequest request = CourseRequest.builder()
                .name("Course")
                .description("Description")
                .type(CourseType.MUSIC)
                .ageGroupList(List.of(AgeGroup.ADULT))
                .build();

        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(courseService);
    }

    @Test
    void createCourseReturnsCreatedPayload() throws Exception {
        CourseRequest request = CourseRequest.builder()
                .name("Course")
                .description("Description")
                .type(CourseType.MUSIC)
                .ageGroupList(List.of(AgeGroup.ADULT))
                .lyceumId(2L)
                .lecturerIds(List.of(3L))
                .build();

        CourseResponse response = CourseResponse.builder()
                .id(15L)
                .name("Course")
                .type(CourseType.MUSIC)
                .ageGroupList(List.of(AgeGroup.ADULT))
                .lyceumId(2L)
                .lecturerIds(List.of(3L))
                .build();

        when(courseService.createCourse(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/courses")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(15L))
                .andExpect(jsonPath("$.name").value("Course"))
                .andExpect(jsonPath("$.lyceumId").value(2L));

        ArgumentCaptor<CourseRequest> captor = ArgumentCaptor.forClass(CourseRequest.class);
        verify(courseService).createCourse(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Course");
        assertThat(captor.getValue().getLecturerIds()).containsExactly(3L);
    }

    @Test
    void createCourseValidatesInput() throws Exception {
        CourseRequest request = CourseRequest.builder()
                .description("Description")
                .type(CourseType.MUSIC)
                .ageGroupList(List.of(AgeGroup.ADULT))
                .build();

        mockMvc.perform(post("/api/v1/courses")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }
}
