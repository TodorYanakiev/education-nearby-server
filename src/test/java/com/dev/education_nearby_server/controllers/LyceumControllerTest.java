package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.models.dto.request.LyceumCreateRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumRightsRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumRightsVerificationRequest;
import com.dev.education_nearby_server.models.dto.response.LyceumResponse;
import com.dev.education_nearby_server.services.LyceumService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LyceumControllerTest {

    @Mock
    private LyceumService lyceumService;

    @InjectMocks
    private LyceumController lyceumController;

    private LyceumRightsRequest rightsRequest;
    private LyceumRightsVerificationRequest verificationRequest;
    private LyceumResponse lyceumResponse;

    @BeforeEach
    void setUp() {
        rightsRequest = LyceumRightsRequest.builder()
                .lyceumName("Lyceum")
                .town("Varna")
                .build();
        verificationRequest = LyceumRightsVerificationRequest.builder()
                .verificationCode("code")
                .build();
        lyceumResponse = LyceumResponse.builder()
                .id(1L)
                .name("Lyceum")
                .town("Varna")
                .build();
    }

    @Test
    void getAllLyceumsReturnsServiceResponse() {
        List<LyceumResponse> lyceums = List.of(lyceumResponse);
        when(lyceumService.getAllLyceums()).thenReturn(lyceums);

        ResponseEntity<List<LyceumResponse>> response = lyceumController.getAllLyceums();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(lyceums);
        verify(lyceumService).getAllLyceums();
    }

    @Test
    void getVerifiedLyceumsReturnsServiceResponse() {
        List<LyceumResponse> lyceums = List.of(lyceumResponse);
        when(lyceumService.getVerifiedLyceums()).thenReturn(lyceums);

        ResponseEntity<List<LyceumResponse>> response = lyceumController.getVerifiedLyceums();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(lyceums);
        verify(lyceumService).getVerifiedLyceums();
    }

    @Test
    void getLyceumByIdReturnsServiceResponse() {
        when(lyceumService.getLyceumById(1L)).thenReturn(lyceumResponse);

        ResponseEntity<LyceumResponse> response = lyceumController.getLyceumById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(lyceumResponse);
        verify(lyceumService).getLyceumById(1L);
    }

    @Test
    void requestRightsReturnsServiceResponse() {
        when(lyceumService.requestRightsOverLyceum(rightsRequest)).thenReturn("ok");

        ResponseEntity<String> response = lyceumController.requestRightsOverLyceum(rightsRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("ok");
        verify(lyceumService).requestRightsOverLyceum(rightsRequest);
    }

    @Test
    void createLyceumReturnsCreatedResponse() {
        LyceumCreateRequest request = LyceumCreateRequest.builder()
                .name("Lyceum")
                .town("Varna")
                .build();
        when(lyceumService.createLyceum(request)).thenReturn(lyceumResponse);

        ResponseEntity<LyceumResponse> response = lyceumController.createLyceum(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(lyceumResponse);
        verify(lyceumService).createLyceum(request);
    }

    @Test
    void verifyRightsReturnsServiceResponse() {
        when(lyceumService.verifyRightsOverLyceum(verificationRequest)).thenReturn("verified");

        ResponseEntity<String> response = lyceumController.verifyRightsOverLyceum(verificationRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("verified");
        verify(lyceumService).verifyRightsOverLyceum(verificationRequest);
    }

    @Test
    void deleteLyceumReturnsNoContent() {
        ResponseEntity<Void> response = lyceumController.deleteLyceum(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(lyceumService).deleteLyceum(1L);
    }
}
