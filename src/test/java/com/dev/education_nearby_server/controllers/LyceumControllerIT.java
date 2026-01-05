package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.enums.Role;
import com.dev.education_nearby_server.exceptions.common.AccessDeniedException;
import com.dev.education_nearby_server.exceptions.common.BadRequestException;
import com.dev.education_nearby_server.exceptions.common.NoSuchElementException;
import com.dev.education_nearby_server.exceptions.common.UnauthorizedException;
import com.dev.education_nearby_server.models.dto.request.LyceumLecturerRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumRightsRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumRightsVerificationRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumRequest;
import com.dev.education_nearby_server.models.dto.response.CourseResponse;
import com.dev.education_nearby_server.models.dto.response.LyceumResponse;
import com.dev.education_nearby_server.models.dto.response.UserResponse;
import com.dev.education_nearby_server.models.entity.Lyceum;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.repositories.LyceumRepository;
import com.dev.education_nearby_server.repositories.UserRepository;
import com.dev.education_nearby_server.services.LyceumService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LyceumControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LyceumService lyceumService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LyceumRepository lyceumRepository;

    @AfterEach
    void cleanUpData() {
        userRepository.deleteAll();
        lyceumRepository.deleteAll();
    }

    @Test
    void getVerifiedLyceumsReturnsServicePayload() throws Exception {
        LyceumResponse response = LyceumResponse.builder()
                .id(1L)
                .name("Lyceum")
                .town("Varna")
                .build();
        when(lyceumService.getVerifiedLyceums()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/lyceums/verified"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Lyceum"));

        verify(lyceumService).getVerifiedLyceums();
    }

    @Test
    void getAllLyceumsReturnsDataForAnonymous() throws Exception {
        LyceumResponse response = LyceumResponse.builder()
                .id(5L)
                .name("Test")
                .town("Varna")
                .build();
        when(lyceumService.getAllLyceums()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/lyceums"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5L))
                .andExpect(jsonPath("$[0].name").value("Test"));

        verify(lyceumService).getAllLyceums();
    }

    @Test
    void getLyceumByIdReturnsPayload() throws Exception {
        LyceumResponse response = LyceumResponse.builder()
                .id(2L)
                .name("Lyceum")
                .town("Sofia")
                .build();
        when(lyceumService.getLyceumById(2L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/lyceums/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.name").value("Lyceum"));

        verify(lyceumService).getLyceumById(2L);
    }

    @Test
    void getLyceumCoursesReturnsServicePayload() throws Exception {
        CourseResponse response = CourseResponse.builder()
                .id(3L)
                .name("Course")
                .lyceumId(2L)
                .build();
        when(lyceumService.getLyceumCourses(2L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/lyceums/2/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3L))
                .andExpect(jsonPath("$[0].name").value("Course"))
                .andExpect(jsonPath("$[0].lyceumId").value(2L));

        verify(lyceumService).getLyceumCourses(2L);
    }

    @Test
    void getLyceumsByIdsReturnsServicePayload() throws Exception {
        LyceumResponse response = LyceumResponse.builder()
                .id(2L)
                .name("Lyceum")
                .town("Sofia")
                .build();
        when(lyceumService.getLyceumsByIds(List.of(2L, 5L))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/lyceums/by-ids")
                        .param("ids", "2", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2L))
                .andExpect(jsonPath("$[0].name").value("Lyceum"));

        verify(lyceumService).getLyceumsByIds(List.of(2L, 5L));
    }

    @Test
    void getLyceumByIdMapsServiceNotFound() throws Exception {
        when(lyceumService.getLyceumById(9L))
                .thenThrow(new NoSuchElementException("missing"));

        mockMvc.perform(get("/api/v1/lyceums/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("missing"))
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));

        verify(lyceumService).getLyceumById(9L);
    }

    @Test
    void filterLyceumsReturnsServicePayload() throws Exception {
        LyceumResponse response = LyceumResponse.builder()
                .id(10L)
                .name("Nearby Lyceum")
                .town("Varna")
                .build();
        when(lyceumService.filterLyceums("Varna", 42.5, 23.3, 3)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/lyceums/filter")
                        .param("town", "Varna")
                        .param("latitude", "42.5")
                        .param("longitude", "23.3")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[0].name").value("Nearby Lyceum"));

        verify(lyceumService).filterLyceums("Varna", 42.5, 23.3, 3);
    }

    @Test
    void filterLyceumsWorksWithoutParams() throws Exception {
        when(lyceumService.filterLyceums(null, null, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/lyceums/filter"))
                .andExpect(status().isOk());

        verify(lyceumService).filterLyceums(null, null, null, null);
    }

    @Test
    void createLyceumRequiresAdminRole() throws Exception {
        LyceumRequest request = LyceumRequest.builder()
                .name("Lyceum")
                .town("Varna")
                .build();

        mockMvc.perform(post("/api/v1/lyceums")
                        .with(user("tester").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(lyceumService);
    }

    @Test
    void createLyceumValidatesInput() throws Exception {
        LyceumRequest request = LyceumRequest.builder()
                .name("")
                .town("")
                .build();

        mockMvc.perform(post("/api/v1/lyceums")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(lyceumService);
    }

    @Test
    void updateLyceumRequiresAuthentication() throws Exception {
        LyceumRequest request = LyceumRequest.builder()
                .name("Updated")
                .town("Varna")
                .build();

        mockMvc.perform(put("/api/v1/lyceums/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(lyceumService);
    }

    @Test
    void updateLyceumReturnsPayloadForAdmin() throws Exception {
        LyceumRequest request = LyceumRequest.builder()
                .name("Updated")
                .town("Varna")
                .build();
        LyceumResponse response = LyceumResponse.builder()
                .id(7L)
                .name("Updated")
                .town("Varna")
                .build();
        when(lyceumService.updateLyceum(eq(7L), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/lyceums/7")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7L))
                .andExpect(jsonPath("$.name").value("Updated"));

        verify(lyceumService).updateLyceum(eq(7L), any());
    }

    @Test
    void updateLyceumReturnsPayloadForLyceumAdministrator() throws Exception {
        Lyceum lyceum = new Lyceum();
        lyceum.setName("Lyceum");
        lyceum.setTown("Varna");
        lyceum = lyceumRepository.save(lyceum);

        User user = User.builder()
                .firstname("Test")
                .lastname("User")
                .email("lyc-admin@example.com")
                .username("lyc-admin")
                .password("password123")
                .role(Role.USER)
                .enabled(true)
                .build();
        user.setAdministratedLyceum(lyceum);
        userRepository.save(user);

        LyceumRequest request = LyceumRequest.builder()
                .name("Updated")
                .town("Varna")
                .build();
        LyceumResponse response = LyceumResponse.builder()
                .id(lyceum.getId())
                .name("Updated")
                .town("Varna")
                .build();
        when(lyceumService.updateLyceum(eq(lyceum.getId()), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/lyceums/" + lyceum.getId())
                        .with(user("lyc-admin").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lyceum.getId()))
                .andExpect(jsonPath("$.name").value("Updated"));

        verify(lyceumService).updateLyceum(eq(lyceum.getId()), any());
    }

    @Test
    void updateLyceumForbiddenForNonAdministratingUser() throws Exception {
        User user = User.builder()
                .firstname("Test")
                .lastname("User")
                .email("regular@example.com")
                .username("regular")
                .password("password123")
                .role(Role.USER)
                .enabled(true)
                .build();
        userRepository.save(user);

        LyceumRequest request = LyceumRequest.builder()
                .name("Updated")
                .town("Varna")
                .build();

        when(lyceumService.updateLyceum(eq(9L), any()))
                .thenThrow(new AccessDeniedException("You do not have permission to modify this lyceum."));

        mockMvc.perform(put("/api/v1/lyceums/9")
                        .with(user("regular").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to modify this lyceum."));

        verify(lyceumService).updateLyceum(eq(9L), any());
    }

    @Test
    void assignAdministratorRequiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/v1/lyceums/3/administrators/7"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(lyceumService);
    }

    @Test
    void assignAdministratorReturnsNoContentForAdmin() throws Exception {
        mockMvc.perform(put("/api/v1/lyceums/3/administrators/7")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());

        verify(lyceumService).assignAdministrator(3L, 7L);
    }

    @Test
    void assignAdministratorForbiddenWhenServiceDeniesAccess() throws Exception {
        doThrow(new AccessDeniedException("You do not have permission to modify this lyceum."))
                .when(lyceumService).assignAdministrator(3L, 7L);

        mockMvc.perform(put("/api/v1/lyceums/3/administrators/7")
                        .with(user("tester").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to modify this lyceum."));

        verify(lyceumService).assignAdministrator(3L, 7L);
    }

    @Test
    void addLecturerRequiresAuthentication() throws Exception {
        LyceumLecturerRequest request = LyceumLecturerRequest.builder()
                .userId(10L)
                .lyceumId(3L)
                .build();

        mockMvc.perform(post("/api/v1/lyceums/lecturers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(lyceumService);
    }

    @Test
    void addLecturerReturnsNoContentForAdmin() throws Exception {
        LyceumLecturerRequest request = LyceumLecturerRequest.builder()
                .userId(10L)
                .lyceumId(3L)
                .build();

        mockMvc.perform(post("/api/v1/lyceums/lecturers")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        ArgumentCaptor<LyceumLecturerRequest> captor = ArgumentCaptor.forClass(LyceumLecturerRequest.class);
        verify(lyceumService).addLecturerToLyceum(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(10L);
        assertThat(captor.getValue().getLyceumId()).isEqualTo(3L);
    }

    @Test
    void addLecturerForbiddenWhenServiceDeniesAccess() throws Exception {
        LyceumLecturerRequest request = LyceumLecturerRequest.builder()
                .userId(10L)
                .lyceumId(3L)
                .build();
        doThrow(new AccessDeniedException("You do not have permission to modify this lyceum."))
                .when(lyceumService).addLecturerToLyceum(any());

        mockMvc.perform(post("/api/v1/lyceums/lecturers")
                        .with(user("tester").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to modify this lyceum."));

        verify(lyceumService).addLecturerToLyceum(any());
    }

    @Test
    void addLecturerValidatesInput() throws Exception {
        LyceumLecturerRequest request = LyceumLecturerRequest.builder()
                .lyceumId(3L)
                .build();

        mockMvc.perform(post("/api/v1/lyceums/lecturers")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(lyceumService);
    }

    @Test
    void getLyceumLecturersReturnsServicePayload() throws Exception {
        UserResponse lecturer = UserResponse.builder()
                .id(4L)
                .firstname("Tanya")
                .lastname("Petrova")
                .lecturedCourseIds(List.of(11L, 12L))
                .lecturedLyceumIds(List.of(6L))
                .build();
        when(lyceumService.getLyceumLecturers(6L)).thenReturn(List.of(lecturer));

        mockMvc.perform(get("/api/v1/lyceums/6/lecturers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(4L))
                .andExpect(jsonPath("$[0].firstname").value("Tanya"))
                .andExpect(jsonPath("$[0].lastname").value("Petrova"))
                .andExpect(jsonPath("$[0].lecturedCourseIds[0]").value(11L))
                .andExpect(jsonPath("$[0].lecturedCourseIds[1]").value(12L))
                .andExpect(jsonPath("$[0].lecturedLyceumIds[0]").value(6L));

        verify(lyceumService).getLyceumLecturers(6L);
    }

    @Test
    void deleteLyceumRequiresAdminRole() throws Exception {
        mockMvc.perform(delete("/api/v1/lyceums/7")
                        .with(user("tester").roles("USER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(lyceumService);
    }

    @Test
    void deleteLyceumReturnsNoContentForAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/lyceums/7")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());

        verify(lyceumService).deleteLyceum(7L);
    }

    @Test
    void requestRightsEndpointReturnsOkWhenServiceSucceeds() throws Exception {
        when(lyceumService.requestRightsOverLyceum(any())).thenReturn("email-sent");
        LyceumRightsRequest request = LyceumRightsRequest.builder()
                .lyceumName("Lyceum")
                .town("Varna")
                .build();

        mockMvc.perform(post("/api/v1/lyceums/request-rights")
                        .with(user("tester").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("email-sent"));

        verify(lyceumService).requestRightsOverLyceum(any());
    }

    @Test
    void requestRightsEndpointRequiresAuthentication() throws Exception {
        LyceumRightsRequest request = LyceumRightsRequest.builder()
                .lyceumName("Lyceum")
                .town("Varna")
                .build();

        mockMvc.perform(post("/api/v1/lyceums/request-rights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"));

        verifyNoInteractions(lyceumService);
    }

    @Test
    void requestRightsEndpointMapsServiceBadRequestException() throws Exception {
        when(lyceumService.requestRightsOverLyceum(any()))
                .thenThrow(new BadRequestException("bad request"));
        LyceumRightsRequest request = LyceumRightsRequest.builder()
                .lyceumName("Lyceum")
                .town("Varna")
                .build();

        mockMvc.perform(post("/api/v1/lyceums/request-rights")
                        .with(user("tester").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("bad request"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));

        verify(lyceumService).requestRightsOverLyceum(any());
    }

    @Test
    void requestRightsEndpointValidatesInput() throws Exception {
        LyceumRightsRequest request = LyceumRightsRequest.builder()
                .lyceumName("")
                .town("")
                .build();

        mockMvc.perform(post("/api/v1/lyceums/request-rights")
                        .with(user("tester").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(lyceumService);
    }

    @Test
    void verifyRightsEndpointReturnsOkWhenServiceSucceeds() throws Exception {
        when(lyceumService.verifyRightsOverLyceum(any())).thenReturn("verified");
        LyceumRightsVerificationRequest request = LyceumRightsVerificationRequest.builder()
                .verificationCode("code")
                .build();

        mockMvc.perform(post("/api/v1/lyceums/verify-rights")
                        .with(user("tester").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("verified"));

        verify(lyceumService).verifyRightsOverLyceum(any());
    }

    @Test
    void verifyRightsEndpointRequiresAuthentication() throws Exception {
        LyceumRightsVerificationRequest request = LyceumRightsVerificationRequest.builder()
                .verificationCode("code")
                .build();

        mockMvc.perform(post("/api/v1/lyceums/verify-rights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"));

        verifyNoInteractions(lyceumService);
    }

    @Test
    void verifyRightsEndpointMapsServiceUnauthorizedException() throws Exception {
        when(lyceumService.verifyRightsOverLyceum(any()))
                .thenThrow(new UnauthorizedException("not allowed"));
        LyceumRightsVerificationRequest request = LyceumRightsVerificationRequest.builder()
                .verificationCode("code")
                .build();

        mockMvc.perform(post("/api/v1/lyceums/verify-rights")
                        .with(user("tester").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("not allowed"))
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"));

        verify(lyceumService).verifyRightsOverLyceum(any());
    }

    @Test
    void verifyRightsEndpointValidatesInput() throws Exception {
        LyceumRightsVerificationRequest request = LyceumRightsVerificationRequest.builder()
                .verificationCode("")
                .build();

        mockMvc.perform(post("/api/v1/lyceums/verify-rights")
                        .with(user("tester").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(lyceumService);
    }
}
