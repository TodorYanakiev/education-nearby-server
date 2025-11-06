package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.exceptions.common.BadRequestException;
import com.dev.education_nearby_server.exceptions.common.NoSuchElementException;
import com.dev.education_nearby_server.exceptions.common.UnauthorizedException;
import com.dev.education_nearby_server.models.dto.request.LyceumCreateRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumRightsRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumRightsVerificationRequest;
import com.dev.education_nearby_server.models.dto.response.LyceumResponse;
import com.dev.education_nearby_server.services.LyceumService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void getAllLyceumsRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/lyceums")
                        .with(user("tester").roles("USER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(lyceumService);
    }

    @Test
    void getAllLyceumsReturnsDataForAdmin() throws Exception {
        LyceumResponse response = LyceumResponse.builder()
                .id(5L)
                .name("Test")
                .town("Varna")
                .build();
        when(lyceumService.getAllLyceums()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/lyceums")
                        .with(user("admin").roles("ADMIN")))
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
    void createLyceumRequiresAdminRole() throws Exception {
        LyceumCreateRequest request = LyceumCreateRequest.builder()
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
        LyceumCreateRequest request = LyceumCreateRequest.builder()
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
