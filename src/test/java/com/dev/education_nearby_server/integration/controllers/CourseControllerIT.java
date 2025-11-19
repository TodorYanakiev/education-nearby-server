package com.dev.education_nearby_server.integration.controllers;

import com.dev.education_nearby_server.enums.AgeGroup;
import com.dev.education_nearby_server.enums.CourseType;
import com.dev.education_nearby_server.enums.ImageRole;
import com.dev.education_nearby_server.models.dto.request.CourseImageRequest;
import com.dev.education_nearby_server.models.dto.request.CourseRequest;
import com.dev.education_nearby_server.models.dto.response.CourseImageResponse;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void getCourseImagesReturnsPayload() throws Exception {
        Long courseId = 11L;
        List<CourseImageResponse> responses = List.of(
                CourseImageResponse.builder().id(1L).courseId(courseId).url("https://example.com/cover.jpg").role(ImageRole.MAIN).build(),
                CourseImageResponse.builder().id(2L).courseId(courseId).url("https://example.com/gallery.jpg").role(ImageRole.GALLERY).build()
        );
        when(courseService.getCourseImages(courseId)).thenReturn(responses);

        mockMvc.perform(get("/api/v1/courses/{courseId}/images", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].role").value("GALLERY"));

        verify(courseService).getCourseImages(courseId);
    }

    @Test
    void addCourseImageRequiresAuthentication() throws Exception {
        CourseImageRequest request = CourseImageRequest.builder()
                .url("https://example.com/cover.jpg")
                .role(ImageRole.MAIN)
                .build();

        mockMvc.perform(post("/api/v1/courses/{courseId}/images", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(courseService);
    }

    @Test
    void addCourseImageReturnsCreatedPayload() throws Exception {
        Long courseId = 6L;
        CourseImageRequest request = CourseImageRequest.builder()
                .s3Key("images/cover.png")
                .role(ImageRole.MAIN)
                .altText("Cover image")
                .orderIndex(0)
                .build();
        CourseImageResponse response = CourseImageResponse.builder()
                .id(44L)
                .courseId(courseId)
                .url("https://cdn.example.com/images/cover.png")
                .role(ImageRole.MAIN)
                .orderIndex(0)
                .build();
        when(courseService.addCourseImage(eq(courseId), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/courses/{courseId}/images", courseId)
                        .with(user("editor").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(44L))
                .andExpect(jsonPath("$.role").value("MAIN"));

        ArgumentCaptor<CourseImageRequest> captor = ArgumentCaptor.forClass(CourseImageRequest.class);
        verify(courseService).addCourseImage(eq(courseId), captor.capture());
        assertThat(captor.getValue().getS3Key()).isEqualTo("images/cover.png");
        assertThat(captor.getValue().getRole()).isEqualTo(ImageRole.MAIN);
    }

    @Test
    void addCourseImageValidatesPayload() throws Exception {
        CourseImageRequest request = CourseImageRequest.builder()
                .url("https://example.com/missing-role.jpg")
                .build();

        mockMvc.perform(post("/api/v1/courses/{courseId}/images", 10L)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(courseService);
    }

    @Test
    void deleteCourseImageRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/{courseId}/images/{imageId}", 7L, 8L))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(courseService);
    }

    @Test
    void deleteCourseImageReturnsNoContent() throws Exception {
        Long courseId = 9L;
        Long imageId = 3L;

        mockMvc.perform(delete("/api/v1/courses/{courseId}/images/{imageId}", courseId, imageId)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());

        verify(courseService).deleteCourseImage(courseId, imageId);
    }
}
