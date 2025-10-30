package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.exceptions.common.BadRequestException;
import com.dev.education_nearby_server.exceptions.common.UnauthorizedException;
import com.dev.education_nearby_server.models.dto.request.LyceumRightsRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumRightsVerificationRequest;
import com.dev.education_nearby_server.services.LyceumService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
