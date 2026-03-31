package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.enums.SubscriberExportFormat;
import com.dev.education_nearby_server.models.dto.response.SubscriberExportJobResponse;
import com.dev.education_nearby_server.models.dto.response.UserResponse;
import com.dev.education_nearby_server.services.LyceumService;
import com.dev.education_nearby_server.services.SubscriberExportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LyceumSubscriberControllerTest {

    @Mock
    private LyceumService lyceumService;
    @Mock
    private SubscriberExportService subscriberExportService;

    @InjectMocks
    private LyceumSubscriberController lyceumSubscriberController;

    @Test
    void subscribeToLyceumReturnsNoContent() {
        ResponseEntity<Void> response = lyceumSubscriberController.subscribeToLyceum(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(lyceumService).subscribeToLyceum(1L);
    }

    @Test
    void unsubscribeFromLyceumReturnsNoContent() {
        ResponseEntity<Void> response = lyceumSubscriberController.unsubscribeFromLyceum(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(lyceumService).unsubscribeFromLyceum(1L);
    }

    @Test
    void getLyceumSubscribersReturnsServiceResponse() {
        UserResponse subscriber = UserResponse.builder()
                .id(14L)
                .firstname("Petya")
                .lastname("Dimitrova")
                .build();
        List<UserResponse> subscribers = List.of(subscriber);
        when(lyceumService.getLyceumSubscribers(1L)).thenReturn(subscribers);

        ResponseEntity<List<UserResponse>> response = lyceumSubscriberController.getLyceumSubscribers(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(subscribers);
        verify(lyceumService).getLyceumSubscribers(1L);
    }

    @Test
    void exportLyceumSubscribersReturnsAcceptedResponse() {
        SubscriberExportJobResponse exportResponse = SubscriberExportJobResponse.builder()
                .id(201L)
                .format(SubscriberExportFormat.CSV)
                .build();
        when(subscriberExportService.createLyceumSubscribersExport(1L, SubscriberExportFormat.CSV))
                .thenReturn(exportResponse);

        ResponseEntity<SubscriberExportJobResponse> response =
                lyceumSubscriberController.exportLyceumSubscribers(1L, SubscriberExportFormat.CSV);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isEqualTo(exportResponse);
        verify(subscriberExportService).createLyceumSubscribersExport(1L, SubscriberExportFormat.CSV);
    }

    @Test
    void getLyceumSubscribersExportStatusReturnsServiceResponse() {
        SubscriberExportJobResponse exportResponse = SubscriberExportJobResponse.builder()
                .id(202L)
                .format(SubscriberExportFormat.XLSX)
                .build();
        when(subscriberExportService.getLyceumSubscribersExportStatus(1L, 202L))
                .thenReturn(exportResponse);

        ResponseEntity<SubscriberExportJobResponse> response =
                lyceumSubscriberController.getLyceumSubscribersExportStatus(1L, 202L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(exportResponse);
        verify(subscriberExportService).getLyceumSubscribersExportStatus(1L, 202L);
    }

    @Test
    void downloadLyceumSubscribersExportReturnsResponseFromService() {
        when(subscriberExportService.downloadLyceumSubscribersExport(1L, 203L))
                .thenReturn(new SubscriberExportService.ExportFile(
                        new PathResource(Path.of("subscribers.xlsx")),
                        "subscribers.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ));

        ResponseEntity<Resource> response = lyceumSubscriberController.downloadLyceumSubscribersExport(1L, 203L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(subscriberExportService).downloadLyceumSubscribersExport(1L, 203L);
    }
}
