package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.config.ExportProperties;
import com.dev.education_nearby_server.config.S3Properties;
import com.dev.education_nearby_server.enums.Role;
import com.dev.education_nearby_server.enums.SubscriberExportFormat;
import com.dev.education_nearby_server.enums.SubscriberExportScope;
import com.dev.education_nearby_server.enums.SubscriberExportStatus;
import com.dev.education_nearby_server.exceptions.common.BadRequestException;
import com.dev.education_nearby_server.exceptions.common.ConflictException;
import com.dev.education_nearby_server.exceptions.common.InternalServerErrorException;
import com.dev.education_nearby_server.exceptions.common.NoSuchElementException;
import com.dev.education_nearby_server.exceptions.common.UnauthorizedException;
import com.dev.education_nearby_server.models.dto.response.SubscriberExportJobResponse;
import com.dev.education_nearby_server.models.entity.SubscriberExportJob;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.repositories.SubscriberExportJobRepository;
import com.dev.education_nearby_server.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriberExportServiceTest {

    @Mock
    private SubscriberExportJobRepository exportJobRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseService courseService;
    @Mock
    private LyceumService lyceumService;
    @Mock
    private SubscriberExportProcessor exportProcessor;
    @Mock
    private S3Properties s3Properties;
    @Mock
    private ExportProperties exportProperties;
    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private SubscriberExportService subscriberExportService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createCourseSubscribersExportStartsAsyncProcessing() {
        User authenticated = createUser(11L);
        authenticate(authenticated);
        when(userRepository.findById(11L)).thenReturn(Optional.of(authenticated));
        when(exportJobRepository.save(any())).thenAnswer(invocation -> {
            SubscriberExportJob job = invocation.getArgument(0);
            job.setId(99L);
            job.setCreatedAt(LocalDateTime.now());
            job.setUpdatedAt(LocalDateTime.now());
            return job;
        });

        SubscriberExportJobResponse response =
                subscriberExportService.createCourseSubscribersExport(5L, SubscriberExportFormat.CSV);

        ArgumentCaptor<SubscriberExportJob> captor = ArgumentCaptor.forClass(SubscriberExportJob.class);
        verify(exportJobRepository).save(captor.capture());
        SubscriberExportJob savedJob = captor.getValue();
        assertThat(savedJob.getScope()).isEqualTo(SubscriberExportScope.COURSE);
        assertThat(savedJob.getTargetId()).isEqualTo(5L);
        assertThat(savedJob.getFormat()).isEqualTo(SubscriberExportFormat.CSV);
        assertThat(savedJob.getStatus()).isEqualTo(SubscriberExportStatus.PENDING);
        assertThat(savedJob.getRequestedByUserId()).isEqualTo(11L);

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getScope()).isEqualTo(SubscriberExportScope.COURSE);
        assertThat(response.getTargetId()).isEqualTo(5L);
        verify(courseService).ensureCurrentUserCanAccessCourseSubscribers(5L);
        verify(exportProcessor).processExportJob(99L);
    }

    @Test
    void createLyceumSubscribersExportStartsAsyncProcessing() {
        User authenticated = createUser(15L);
        authenticate(authenticated);
        when(userRepository.findById(15L)).thenReturn(Optional.of(authenticated));
        when(exportJobRepository.save(any())).thenAnswer(invocation -> {
            SubscriberExportJob job = invocation.getArgument(0);
            job.setId(120L);
            return job;
        });

        SubscriberExportJobResponse response =
                subscriberExportService.createLyceumSubscribersExport(21L, SubscriberExportFormat.XLSX);

        verify(lyceumService).ensureCurrentUserCanAccessLyceumSubscribers(21L);
        verify(exportProcessor).processExportJob(120L);
        assertThat(response.getId()).isEqualTo(120L);
        assertThat(response.getScope()).isEqualTo(SubscriberExportScope.LYCEUM);
        assertThat(response.getTargetId()).isEqualTo(21L);
        assertThat(response.getFormat()).isEqualTo(SubscriberExportFormat.XLSX);
    }

    @Test
    void createCourseSubscribersExportThrowsWhenFormatMissing() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> subscriberExportService.createCourseSubscribersExport(5L, null));

        assertThat(exception.getMessage()).isEqualTo("Export format must be provided.");
        verify(courseService).ensureCurrentUserCanAccessCourseSubscribers(5L);
        verifyNoInteractions(exportJobRepository, exportProcessor, userRepository);
    }

    @Test
    void createCourseSubscribersExportThrowsWhenUnauthenticated() {
        assertThrows(UnauthorizedException.class,
                () -> subscriberExportService.createCourseSubscribersExport(5L, SubscriberExportFormat.CSV));

        verify(courseService).ensureCurrentUserCanAccessCourseSubscribers(5L);
        verifyNoInteractions(exportJobRepository, exportProcessor);
    }

    @Test
    void createCourseSubscribersExportThrowsWhenPrincipalIsNotDomainUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("plain-user", null)
        );

        assertThrows(UnauthorizedException.class,
                () -> subscriberExportService.createCourseSubscribersExport(5L, SubscriberExportFormat.CSV));

        verify(courseService).ensureCurrentUserCanAccessCourseSubscribers(5L);
        verifyNoInteractions(exportJobRepository, exportProcessor, userRepository);
    }

    @Test
    void createCourseSubscribersExportThrowsWhenUserMissingInRepository() {
        User authenticated = createUser(12L);
        authenticate(authenticated);
        when(userRepository.findById(12L)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> subscriberExportService.createCourseSubscribersExport(6L, SubscriberExportFormat.XLSX));

        verify(courseService).ensureCurrentUserCanAccessCourseSubscribers(6L);
        verify(userRepository).findById(12L);
        verifyNoInteractions(exportJobRepository, exportProcessor);
    }

    @Test
    void getCourseSubscribersExportStatusReturnsMappedResponse() {
        SubscriberExportJob job = buildJob(200L, SubscriberExportScope.COURSE, 7L, SubscriberExportStatus.COMPLETED);
        when(exportJobRepository.findByIdAndScopeAndTargetId(200L, SubscriberExportScope.COURSE, 7L))
                .thenReturn(Optional.of(job));

        SubscriberExportJobResponse response = subscriberExportService.getCourseSubscribersExportStatus(7L, 200L);

        assertThat(response.getId()).isEqualTo(200L);
        assertThat(response.getScope()).isEqualTo(SubscriberExportScope.COURSE);
        assertThat(response.getStatus()).isEqualTo(SubscriberExportStatus.COMPLETED);
        verify(courseService).ensureCurrentUserCanAccessCourseSubscribers(7L);
    }

    @Test
    void getLyceumSubscribersExportStatusReturnsMappedResponse() {
        SubscriberExportJob job = buildJob(210L, SubscriberExportScope.LYCEUM, 17L, SubscriberExportStatus.IN_PROGRESS);
        when(exportJobRepository.findByIdAndScopeAndTargetId(210L, SubscriberExportScope.LYCEUM, 17L))
                .thenReturn(Optional.of(job));

        SubscriberExportJobResponse response = subscriberExportService.getLyceumSubscribersExportStatus(17L, 210L);

        assertThat(response.getId()).isEqualTo(210L);
        assertThat(response.getScope()).isEqualTo(SubscriberExportScope.LYCEUM);
        assertThat(response.getStatus()).isEqualTo(SubscriberExportStatus.IN_PROGRESS);
        verify(lyceumService).ensureCurrentUserCanAccessLyceumSubscribers(17L);
    }

    @Test
    void getCourseSubscribersExportStatusThrowsWhenJobMissing() {
        when(exportJobRepository.findByIdAndScopeAndTargetId(201L, SubscriberExportScope.COURSE, 8L))
                .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> subscriberExportService.getCourseSubscribersExportStatus(8L, 201L));

        verify(courseService).ensureCurrentUserCanAccessCourseSubscribers(8L);
    }

    @Test
    void downloadCourseSubscribersExportThrowsWhenJobFailed() {
        SubscriberExportJob job = buildJob(300L, SubscriberExportScope.COURSE, 9L, SubscriberExportStatus.FAILED);
        job.setErrorMessage("Generation failed");
        when(exportJobRepository.findByIdAndScopeAndTargetId(300L, SubscriberExportScope.COURSE, 9L))
                .thenReturn(Optional.of(job));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> subscriberExportService.downloadCourseSubscribersExport(9L, 300L));

        assertThat(exception.getMessage()).isEqualTo("Generation failed");
        verify(courseService).ensureCurrentUserCanAccessCourseSubscribers(9L);
    }

    @Test
    void downloadCourseSubscribersExportThrowsDefaultMessageWhenJobFailedWithoutMessage() {
        SubscriberExportJob job = buildJob(306L, SubscriberExportScope.COURSE, 14L, SubscriberExportStatus.FAILED);
        job.setErrorMessage(null);
        when(exportJobRepository.findByIdAndScopeAndTargetId(306L, SubscriberExportScope.COURSE, 14L))
                .thenReturn(Optional.of(job));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> subscriberExportService.downloadCourseSubscribersExport(14L, 306L));

        assertThat(exception.getMessage()).isEqualTo("Export generation failed.");
    }

    @Test
    void downloadCourseSubscribersExportThrowsWhenJobNotReady() {
        SubscriberExportJob job = buildJob(301L, SubscriberExportScope.COURSE, 9L, SubscriberExportStatus.IN_PROGRESS);
        when(exportJobRepository.findByIdAndScopeAndTargetId(301L, SubscriberExportScope.COURSE, 9L))
                .thenReturn(Optional.of(job));

        assertThrows(ConflictException.class,
                () -> subscriberExportService.downloadCourseSubscribersExport(9L, 301L));
    }

    @Test
    void downloadCourseSubscribersExportThrowsWhenFilePathMissing() {
        SubscriberExportJob job = buildJob(302L, SubscriberExportScope.COURSE, 10L, SubscriberExportStatus.COMPLETED);
        job.setFilePath("  ");
        when(exportJobRepository.findByIdAndScopeAndTargetId(302L, SubscriberExportScope.COURSE, 10L))
                .thenReturn(Optional.of(job));

        assertThrows(NoSuchElementException.class,
                () -> subscriberExportService.downloadCourseSubscribersExport(10L, 302L));
    }

    @Test
    void downloadCourseSubscribersExportReturnsPresignedUrl() throws Exception {
        SubscriberExportJob job = buildJob(303L, SubscriberExportScope.COURSE, 11L, SubscriberExportStatus.COMPLETED);
        job.setFilePath("exports/subscribers/export-303.csv");
        job.setFileName("export-303.csv");
        job.setContentType("text/csv");
        when(exportJobRepository.findByIdAndScopeAndTargetId(303L, SubscriberExportScope.COURSE, 11L))
                .thenReturn(Optional.of(job));
        when(s3Properties.getBucketName()).thenReturn("education-nearby-demo-bucket");
        when(exportProperties.getPresignedUrlMinutes()).thenReturn(7);
        PresignedGetObjectRequest presignedGetObjectRequest = org.mockito.Mockito.mock(PresignedGetObjectRequest.class);
        when(presignedGetObjectRequest.url()).thenReturn(new URL("https://download.example.com/export-303.csv"));
        when(s3Presigner.presignGetObject(any())).thenReturn(presignedGetObjectRequest);

        SubscriberExportService.ExportDownload response =
                subscriberExportService.downloadCourseSubscribersExport(11L, 303L);

        assertThat(response.url()).isEqualTo(URI.create("https://download.example.com/export-303.csv"));
        verify(s3Presigner).presignGetObject(any());
    }

    @Test
    void downloadCourseSubscribersExportUsesDefaultDownloadMetadataWhenMissing() throws Exception {
        SubscriberExportJob job = buildJob(333L, SubscriberExportScope.COURSE, 15L, SubscriberExportStatus.COMPLETED);
        job.setFilePath("exports/subscribers/s3-object-name.xlsx");
        job.setFileName(" ");
        job.setContentType(null);
        when(exportJobRepository.findByIdAndScopeAndTargetId(333L, SubscriberExportScope.COURSE, 15L))
                .thenReturn(Optional.of(job));
        when(s3Properties.getBucketName()).thenReturn("education-nearby-demo-bucket");
        when(exportProperties.getPresignedUrlMinutes()).thenReturn(0);
        PresignedGetObjectRequest presignedGetObjectRequest = org.mockito.Mockito.mock(PresignedGetObjectRequest.class);
        when(presignedGetObjectRequest.url()).thenReturn(new URL("https://download.example.com/defaults"));
        when(s3Presigner.presignGetObject(any())).thenReturn(presignedGetObjectRequest);

        SubscriberExportService.ExportDownload response =
                subscriberExportService.downloadCourseSubscribersExport(15L, 333L);

        ArgumentCaptor<GetObjectPresignRequest> presignCaptor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(presignCaptor.capture());
        GetObjectPresignRequest presignRequest = presignCaptor.getValue();
        GetObjectRequest getObjectRequest = presignRequest.getObjectRequest();

        assertThat(response.url()).isEqualTo(URI.create("https://download.example.com/defaults"));
        assertThat(presignRequest.signatureDuration()).isEqualTo(Duration.ofMinutes(10));
        assertThat(getObjectRequest.responseContentType()).isEqualTo("application/octet-stream");
        assertThat(getObjectRequest.responseContentDisposition())
                .isEqualTo("attachment; filename=\"s3-object-name.xlsx\"");
    }

    @Test
    void downloadCourseSubscribersExportThrowsWhenBucketNotConfigured() {
        SubscriberExportJob job = buildJob(304L, SubscriberExportScope.COURSE, 12L, SubscriberExportStatus.COMPLETED);
        job.setFilePath("exports/subscribers/export-304.csv");
        when(exportJobRepository.findByIdAndScopeAndTargetId(304L, SubscriberExportScope.COURSE, 12L))
                .thenReturn(Optional.of(job));
        when(s3Properties.getBucketName()).thenReturn(" ");

        assertThrows(InternalServerErrorException.class,
                () -> subscriberExportService.downloadCourseSubscribersExport(12L, 304L));
    }

    @Test
    void downloadLyceumSubscribersExportUsesLyceumAuthorization() throws Exception {
        SubscriberExportJob job = buildJob(401L, SubscriberExportScope.LYCEUM, 20L, SubscriberExportStatus.COMPLETED);
        job.setFilePath("exports/subscribers/export-401.xlsx");
        job.setFileName("export-401.xlsx");
        job.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        when(exportJobRepository.findByIdAndScopeAndTargetId(401L, SubscriberExportScope.LYCEUM, 20L))
                .thenReturn(Optional.of(job));
        when(s3Properties.getBucketName()).thenReturn("education-nearby-demo-bucket");
        when(exportProperties.getPresignedUrlMinutes()).thenReturn(10);
        PresignedGetObjectRequest presignedGetObjectRequest = org.mockito.Mockito.mock(PresignedGetObjectRequest.class);
        when(presignedGetObjectRequest.url()).thenReturn(new URL("https://download.example.com/export-401.xlsx"));
        when(s3Presigner.presignGetObject(any())).thenReturn(presignedGetObjectRequest);

        SubscriberExportService.ExportDownload response =
                subscriberExportService.downloadLyceumSubscribersExport(20L, 401L);

        assertThat(response.url()).isEqualTo(URI.create("https://download.example.com/export-401.xlsx"));
        verify(lyceumService).ensureCurrentUserCanAccessLyceumSubscribers(20L);
        verify(exportJobRepository).findByIdAndScopeAndTargetId(401L, SubscriberExportScope.LYCEUM, 20L);
    }

    @Test
    void downloadCourseSubscribersExportThrowsWhenPresignerFails() {
        SubscriberExportJob job = buildJob(305L, SubscriberExportScope.COURSE, 13L, SubscriberExportStatus.COMPLETED);
        job.setFilePath("exports/subscribers/export-305.csv");
        when(exportJobRepository.findByIdAndScopeAndTargetId(305L, SubscriberExportScope.COURSE, 13L))
                .thenReturn(Optional.of(job));
        when(s3Properties.getBucketName()).thenReturn("education-nearby-demo-bucket");
        when(exportProperties.getPresignedUrlMinutes()).thenReturn(10);
        when(s3Presigner.presignGetObject(any())).thenThrow(new RuntimeException("boom"));

        InternalServerErrorException exception = assertThrows(InternalServerErrorException.class,
                () -> subscriberExportService.downloadCourseSubscribersExport(13L, 305L));

        assertThat(exception.getMessage()).isEqualTo("Could not generate export download URL.");
    }

    private SubscriberExportJob buildJob(Long id, SubscriberExportScope scope, Long targetId, SubscriberExportStatus status) {
        SubscriberExportJob job = new SubscriberExportJob();
        job.setId(id);
        job.setScope(scope);
        job.setTargetId(targetId);
        job.setFormat(SubscriberExportFormat.CSV);
        job.setStatus(status);
        job.setRequestedByUserId(1L);
        return job;
    }

    private User createUser(Long id) {
        return User.builder()
                .id(id)
                .firstname("John")
                .lastname("Doe")
                .email("john" + id + "@example.com")
                .username("john" + id)
                .password("secret")
                .role(Role.USER)
                .enabled(true)
                .build();
    }

    private void authenticate(User user) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
