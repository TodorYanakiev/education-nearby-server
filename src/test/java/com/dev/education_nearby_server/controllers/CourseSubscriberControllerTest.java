package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.enums.SubscriberExportFormat;
import com.dev.education_nearby_server.models.dto.response.SubscriberExportJobResponse;
import com.dev.education_nearby_server.models.dto.response.UserResponse;
import com.dev.education_nearby_server.services.CourseService;
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
class CourseSubscriberControllerTest {

    @Mock
    private CourseService courseService;
    @Mock
    private SubscriberExportService subscriberExportService;

    @InjectMocks
    private CourseSubscriberController courseSubscriberController;

    @Test
    void subscribeToCourseReturnsNoContent() {
        Long courseId = 21L;

        ResponseEntity<Void> result = courseSubscriberController.subscribeToCourse(courseId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.hasBody()).isFalse();
        verify(courseService).subscribeToCourse(courseId);
    }

    @Test
    void unsubscribeFromCourseReturnsNoContent() {
        Long courseId = 22L;

        ResponseEntity<Void> result = courseSubscriberController.unsubscribeFromCourse(courseId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.hasBody()).isFalse();
        verify(courseService).unsubscribeFromCourse(courseId);
    }

    @Test
    void getCourseSubscribersReturnsResponseFromService() {
        Long courseId = 8L;
        List<UserResponse> responses = List.of(
                UserResponse.builder().id(1L).firstname("Ivan").lastname("Ivanov").build(),
                UserResponse.builder().id(2L).firstname("Maria").lastname("Petrova").build()
        );
        when(courseService.getCourseSubscribers(courseId)).thenReturn(responses);

        ResponseEntity<List<UserResponse>> result = courseSubscriberController.getCourseSubscribers(courseId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(responses);
        verify(courseService).getCourseSubscribers(courseId);
    }

    @Test
    void exportCourseSubscribersReturnsAcceptedResponse() {
        Long courseId = 9L;
        SubscriberExportJobResponse exportResponse = SubscriberExportJobResponse.builder()
                .id(101L)
                .format(SubscriberExportFormat.CSV)
                .build();
        when(subscriberExportService.createCourseSubscribersExport(courseId, SubscriberExportFormat.CSV))
                .thenReturn(exportResponse);

        ResponseEntity<SubscriberExportJobResponse> result =
                courseSubscriberController.exportCourseSubscribers(courseId, SubscriberExportFormat.CSV);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(result.getBody()).isEqualTo(exportResponse);
        verify(subscriberExportService).createCourseSubscribersExport(courseId, SubscriberExportFormat.CSV);
    }

    @Test
    void getCourseSubscribersExportStatusReturnsResponseFromService() {
        Long courseId = 10L;
        Long exportId = 102L;
        SubscriberExportJobResponse exportResponse = SubscriberExportJobResponse.builder()
                .id(exportId)
                .format(SubscriberExportFormat.XLSX)
                .build();
        when(subscriberExportService.getCourseSubscribersExportStatus(courseId, exportId))
                .thenReturn(exportResponse);

        ResponseEntity<SubscriberExportJobResponse> result =
                courseSubscriberController.getCourseSubscribersExportStatus(courseId, exportId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(exportResponse);
        verify(subscriberExportService).getCourseSubscribersExportStatus(courseId, exportId);
    }

    @Test
    void downloadCourseSubscribersExportReturnsResponseFromService() {
        Long courseId = 11L;
        Long exportId = 103L;
        when(subscriberExportService.downloadCourseSubscribersExport(courseId, exportId))
                .thenReturn(new SubscriberExportService.ExportFile(
                        new PathResource(Path.of("subscribers.csv")),
                        "subscribers.csv",
                        "text/csv"
                ));

        ResponseEntity<Resource> result = courseSubscriberController.downloadCourseSubscribersExport(courseId, exportId);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(subscriberExportService).downloadCourseSubscribersExport(courseId, exportId);
    }
}
