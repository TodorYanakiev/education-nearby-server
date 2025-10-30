package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.models.dto.request.LyceumRightsRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumRightsVerificationRequest;
import com.dev.education_nearby_server.services.LyceumService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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

    @BeforeEach
    void setUp() {
        rightsRequest = LyceumRightsRequest.builder()
                .lyceumName("Lyceum")
                .town("Varna")
                .build();
        verificationRequest = LyceumRightsVerificationRequest.builder()
                .verificationCode("code")
                .build();
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
    void verifyRightsReturnsServiceResponse() {
        when(lyceumService.verifyRightsOverLyceum(verificationRequest)).thenReturn("verified");

        ResponseEntity<String> response = lyceumController.verifyRightsOverLyceum(verificationRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("verified");
        verify(lyceumService).verifyRightsOverLyceum(verificationRequest);
    }
}
