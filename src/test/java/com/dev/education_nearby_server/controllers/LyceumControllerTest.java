package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.models.dto.request.LyceumLecturerInviteRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumLecturerRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumRightsRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumRightsVerificationRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumImageRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumRequest;
import com.dev.education_nearby_server.models.dto.response.CourseResponse;
import com.dev.education_nearby_server.models.dto.response.LyceumImageResponse;
import com.dev.education_nearby_server.models.dto.response.LyceumResponse;
import com.dev.education_nearby_server.models.dto.response.UserResponse;
import com.dev.education_nearby_server.services.LyceumService;
import com.dev.education_nearby_server.enums.ImageRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
    void getLyceumImagesReturnsServiceResponse() {
        List<LyceumImageResponse> images = List.of(
                LyceumImageResponse.builder().id(1L).lyceumId(1L).url("https://cdn/main.jpg").role(ImageRole.MAIN).build(),
                LyceumImageResponse.builder().id(2L).lyceumId(1L).url("https://cdn/gallery.jpg").role(ImageRole.GALLERY).build()
        );
        when(lyceumService.getLyceumImages(1L)).thenReturn(images);

        ResponseEntity<List<LyceumImageResponse>> response = lyceumController.getLyceumImages(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(images);
        verify(lyceumService).getLyceumImages(1L);
    }

    @Test
    void getLyceumCoursesReturnsServiceResponse() {
        CourseResponse course = CourseResponse.builder()
                .id(9L)
                .name("Course")
                .build();
        List<CourseResponse> courses = List.of(course);
        when(lyceumService.getLyceumCourses(1L)).thenReturn(courses);

        ResponseEntity<List<CourseResponse>> response = lyceumController.getLyceumCourses(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(courses);
        verify(lyceumService).getLyceumCourses(1L);
    }

    @Test
    void subscribeToLyceumReturnsNoContent() {
        ResponseEntity<Void> response = lyceumController.subscribeToLyceum(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(lyceumService).subscribeToLyceum(1L);
    }

    @Test
    void unsubscribeFromLyceumReturnsNoContent() {
        ResponseEntity<Void> response = lyceumController.unsubscribeFromLyceum(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(lyceumService).unsubscribeFromLyceum(1L);
    }

    @Test
    void getLyceumsByIdsReturnsServiceResponse() {
        List<Long> ids = List.of(1L, 2L);
        List<LyceumResponse> lyceums = List.of(lyceumResponse);
        when(lyceumService.getLyceumsByIds(ids)).thenReturn(lyceums);

        ResponseEntity<List<LyceumResponse>> response = lyceumController.getLyceumsByIds(ids);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(lyceums);
        verify(lyceumService).getLyceumsByIds(ids);
    }

    @Test
    void filterLyceumsReturnsServiceResponse() {
        Page<LyceumResponse> lyceums = new PageImpl<>(
                List.of(lyceumResponse),
                PageRequest.of(0, 9),
                1
        );
        when(lyceumService.filterLyceums("Varna", 42.5, 23.3, 0, 9)).thenReturn(lyceums);

        ResponseEntity<Page<LyceumResponse>> response =
                lyceumController.filterLyceums("Varna", 42.5, 23.3, 0, 9);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(lyceums);
        verify(lyceumService).filterLyceums("Varna", 42.5, 23.3, 0, 9);
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
        LyceumRequest request = LyceumRequest.builder()
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
    void addLyceumImageReturnsCreatedResponse() {
        LyceumImageRequest request = LyceumImageRequest.builder()
                .url("https://cdn/main.jpg")
                .role(ImageRole.MAIN)
                .build();
        LyceumImageResponse imageResponse = LyceumImageResponse.builder()
                .id(11L)
                .lyceumId(1L)
                .url("https://cdn/main.jpg")
                .role(ImageRole.MAIN)
                .build();
        when(lyceumService.addLyceumImage(1L, request)).thenReturn(imageResponse);

        ResponseEntity<LyceumImageResponse> response = lyceumController.addLyceumImage(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(imageResponse);
        verify(lyceumService).addLyceumImage(1L, request);
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
    void updateLyceumReturnsServiceResponse() {
        LyceumRequest request = LyceumRequest.builder()
                .name("Updated")
                .town("Varna")
                .build();
        when(lyceumService.updateLyceum(1L, request)).thenReturn(lyceumResponse);

        ResponseEntity<LyceumResponse> response = lyceumController.updateLyceum(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(lyceumResponse);
        verify(lyceumService).updateLyceum(1L, request);
    }

    @Test
    void assignAdministratorReturnsNoContent() {
        ResponseEntity<Void> response = lyceumController.assignAdministrator(2L, 5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(lyceumService).assignAdministrator(2L, 5L);
    }

    @Test
    void removeAdministratorReturnsNoContent() {
        ResponseEntity<Void> response = lyceumController.removeAdministrator(3L, 5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(lyceumService).removeAdministratorFromLyceum(3L, 5L);
    }

    @Test
    void addLecturerReturnsNoContent() {
        LyceumLecturerRequest request = LyceumLecturerRequest.builder()
                .userId(10L)
                .lyceumId(3L)
                .build();

        ResponseEntity<Void> response = lyceumController.addLecturer(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(lyceumService).addLecturerToLyceum(request);
    }

    @Test
    void inviteLecturerReturnsNoContent() {
        LyceumLecturerInviteRequest request = LyceumLecturerInviteRequest.builder()
                .email("invitee@example.com")
                .lyceumId(3L)
                .build();

        ResponseEntity<Void> response = lyceumController.inviteLecturer(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(lyceumService).inviteLecturerByEmail(request);
    }

    @Test
    void removeLecturerReturnsNoContent() {
        ResponseEntity<Void> response = lyceumController.removeLecturer(2L, 5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(lyceumService).removeLecturerFromLyceum(2L, 5L);
    }

    @Test
    void deleteLyceumImageReturnsNoContent() {
        ResponseEntity<Void> response = lyceumController.deleteLyceumImage(2L, 7L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(lyceumService).deleteLyceumImage(2L, 7L);
    }

    @Test
    void getLyceumLecturersReturnsServiceResponse() {
        UserResponse lecturer = UserResponse.builder()
                .id(4L)
                .firstname("Tanya")
                .lastname("Petrova")
                .build();
        List<UserResponse> lecturers = List.of(lecturer);
        when(lyceumService.getLyceumLecturers(1L)).thenReturn(lecturers);

        ResponseEntity<List<UserResponse>> response = lyceumController.getLyceumLecturers(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(lecturers);
        verify(lyceumService).getLyceumLecturers(1L);
    }

    @Test
    void getLyceumAdministratorsReturnsServiceResponse() {
        UserResponse administrator = UserResponse.builder()
                .id(9L)
                .firstname("Admin")
                .lastname("User")
                .build();
        List<UserResponse> administrators = List.of(administrator);
        when(lyceumService.getLyceumAdministrators(1L)).thenReturn(administrators);

        ResponseEntity<List<UserResponse>> response = lyceumController.getLyceumAdministrators(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(administrators);
        verify(lyceumService).getLyceumAdministrators(1L);
    }

    @Test
    void deleteLyceumReturnsNoContent() {
        ResponseEntity<Void> response = lyceumController.deleteLyceum(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(lyceumService).deleteLyceum(1L);
    }
}
