package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.config.ExportProperties;
import com.dev.education_nearby_server.config.S3Properties;
import com.dev.education_nearby_server.enums.Role;
import com.dev.education_nearby_server.enums.SubscriberExportFormat;
import com.dev.education_nearby_server.enums.SubscriberExportScope;
import com.dev.education_nearby_server.enums.SubscriberExportStatus;
import com.dev.education_nearby_server.models.entity.SubscriberExportJob;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.repositories.CourseRepository;
import com.dev.education_nearby_server.repositories.LyceumRepository;
import com.dev.education_nearby_server.repositories.SubscriberExportJobRepository;
import com.dev.education_nearby_server.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriberExportProcessorTest {

    @Mock
    private SubscriberExportJobRepository exportJobRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private LyceumRepository lyceumRepository;
    @Mock
    private ExportProperties exportProperties;
    @Mock
    private S3Properties s3Properties;
    @Mock
    private S3Client s3Client;

    @InjectMocks
    private SubscriberExportProcessor subscriberExportProcessor;

    @TempDir
    Path tempDir;

    @Test
    void processExportJobReturnsWhenJobMissing() {
        when(exportJobRepository.findById(1L)).thenReturn(Optional.empty());

        subscriberExportProcessor.processExportJob(1L);

        verify(exportJobRepository).findById(1L);
        verifyNoMoreInteractions(exportJobRepository);
    }

    @Test
    void processExportJobCompletesAndUploadsCsvForCourse() {
        SubscriberExportJob job = buildJob(2L, SubscriberExportScope.COURSE, 19L, SubscriberExportFormat.CSV);
        User subscriber = createUser(10L);
        when(exportJobRepository.findById(2L)).thenReturn(Optional.of(job));
        when(exportJobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(courseRepository.existsById(19L)).thenReturn(true);
        when(userRepository.findDistinctBySubscribedCourses_IdOrderByIdAsc(19L)).thenReturn(List.of(subscriber));
        when(exportProperties.getDirectory()).thenReturn(tempDir.toString());
        when(exportProperties.getS3Prefix()).thenReturn("exports/subscribers/");
        when(s3Properties.getBucketName()).thenReturn("education-nearby-demo-bucket");

        subscriberExportProcessor.processExportJob(2L);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<Path> pathCaptor = ArgumentCaptor.forClass(Path.class);
        verify(s3Client).putObject(requestCaptor.capture(), pathCaptor.capture());

        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo("education-nearby-demo-bucket");
        assertThat(request.key()).startsWith("exports/subscribers/course-19-subscribers-2-");
        assertThat(request.key()).endsWith(".csv");
        assertThat(request.contentType()).isEqualTo("text/csv");

        Path generatedLocalFile = pathCaptor.getValue();
        assertThat(Files.exists(generatedLocalFile)).isFalse();

        assertThat(job.getStatus()).isEqualTo(SubscriberExportStatus.COMPLETED);
        assertThat(job.getFilePath()).isEqualTo(request.key());
        assertThat(job.getFileName()).endsWith(".csv");
        assertThat(job.getContentType()).isEqualTo("text/csv");
        assertThat(job.getCompletedAt()).isNotNull();
    }

    @Test
    void processExportJobCompletesAndUploadsXlsxWhenFormatExcelAlias() {
        SubscriberExportJob job = buildJob(3L, SubscriberExportScope.LYCEUM, 29L, SubscriberExportFormat.EXCEL);
        when(exportJobRepository.findById(3L)).thenReturn(Optional.of(job));
        when(exportJobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(lyceumRepository.existsById(29L)).thenReturn(true);
        when(userRepository.findDistinctBySubscribedLyceums_IdOrderByIdAsc(29L)).thenReturn(List.of());
        when(exportProperties.getDirectory()).thenReturn(tempDir.toString());
        when(exportProperties.getS3Prefix()).thenReturn("exports/subscribers/");
        when(s3Properties.getBucketName()).thenReturn("education-nearby-demo-bucket");

        subscriberExportProcessor.processExportJob(3L);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(Path.class));

        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.key()).startsWith("exports/subscribers/lyceum-29-subscribers-3-");
        assertThat(request.key()).endsWith(".xlsx");
        assertThat(request.contentType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(job.getFileName()).endsWith(".xlsx");
        assertThat(job.getStatus()).isEqualTo(SubscriberExportStatus.COMPLETED);
    }

    @Test
    void processExportJobMarksFailedWhenTargetMissing() {
        SubscriberExportJob job = buildJob(4L, SubscriberExportScope.LYCEUM, 30L, SubscriberExportFormat.CSV);
        when(exportJobRepository.findById(4L)).thenReturn(Optional.of(job));
        when(exportJobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(lyceumRepository.existsById(30L)).thenReturn(false);

        subscriberExportProcessor.processExportJob(4L);

        assertThat(job.getStatus()).isEqualTo(SubscriberExportStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("Target resource no longer exists.");
        assertThat(job.getCompletedAt()).isNotNull();
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(Path.class));
    }

    @Test
    void processExportJobMarksFailedWhenBucketMissing() {
        SubscriberExportJob job = buildJob(5L, SubscriberExportScope.COURSE, 31L, SubscriberExportFormat.CSV);
        when(exportJobRepository.findById(5L)).thenReturn(Optional.of(job));
        when(exportJobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(courseRepository.existsById(31L)).thenReturn(true);
        when(userRepository.findDistinctBySubscribedCourses_IdOrderByIdAsc(31L)).thenReturn(List.of());
        when(exportProperties.getDirectory()).thenReturn(tempDir.toString());
        when(s3Properties.getBucketName()).thenReturn(" ");

        subscriberExportProcessor.processExportJob(5L);

        assertThat(job.getStatus()).isEqualTo(SubscriberExportStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("S3 bucket name is not configured.");
        assertThat(job.getCompletedAt()).isNotNull();
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(Path.class));
    }

    @Test
    void processExportJobWritesEscapedCsvData() {
        SubscriberExportJob job = buildJob(6L, SubscriberExportScope.COURSE, 32L, SubscriberExportFormat.CSV);
        User subscriber = User.builder()
                .id(77L)
                .firstname("Ivan, Jr.")
                .lastname("Quote \"inside\"")
                .email("ivan@example.com")
                .username("ivan,user")
                .role(Role.USER)
                .enabled(true)
                .build();
        AtomicReference<String> uploadedCsv = new AtomicReference<>();
        when(exportJobRepository.findById(6L)).thenReturn(Optional.of(job));
        when(exportJobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(courseRepository.existsById(32L)).thenReturn(true);
        when(userRepository.findDistinctBySubscribedCourses_IdOrderByIdAsc(32L)).thenReturn(List.of(subscriber));
        when(exportProperties.getDirectory()).thenReturn(tempDir.toString());
        when(exportProperties.getS3Prefix()).thenReturn("exports/subscribers/");
        when(s3Properties.getBucketName()).thenReturn("education-nearby-demo-bucket");
        when(s3Client.putObject(any(PutObjectRequest.class), any(Path.class))).thenAnswer(invocation -> {
            Path source = invocation.getArgument(1);
            uploadedCsv.set(Files.readString(source));
            return PutObjectResponse.builder().build();
        });

        subscriberExportProcessor.processExportJob(6L);

        assertThat(uploadedCsv.get()).contains("id,firstname,lastname,email,username,role,enabled");
        assertThat(uploadedCsv.get()).contains("\"Ivan, Jr.\"");
        assertThat(uploadedCsv.get()).contains("\"Quote \"\"inside\"\"\"");
        assertThat(uploadedCsv.get()).contains("\"ivan,user\"");
    }

    @Test
    void processExportJobNormalizesPrefixAndTrimsBucketName() {
        SubscriberExportJob job = buildJob(7L, SubscriberExportScope.COURSE, 33L, SubscriberExportFormat.CSV);
        when(exportJobRepository.findById(7L)).thenReturn(Optional.of(job));
        when(exportJobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(courseRepository.existsById(33L)).thenReturn(true);
        when(userRepository.findDistinctBySubscribedCourses_IdOrderByIdAsc(33L)).thenReturn(List.of());
        when(exportProperties.getDirectory()).thenReturn(tempDir.toString());
        when(exportProperties.getS3Prefix()).thenReturn("/exports\\subscribers");
        when(s3Properties.getBucketName()).thenReturn(" education-nearby-demo-bucket ");

        subscriberExportProcessor.processExportJob(7L);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(Path.class));
        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo("education-nearby-demo-bucket");
        assertThat(request.key()).startsWith("exports/subscribers/course-33-subscribers-7-");
    }

    @Test
    void processExportJobTruncatesLongFailureMessage() {
        SubscriberExportJob job = buildJob(8L, SubscriberExportScope.COURSE, 34L, SubscriberExportFormat.CSV);
        String longMessage = "x".repeat(1500);
        when(exportJobRepository.findById(8L)).thenReturn(Optional.of(job));
        when(exportJobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(courseRepository.existsById(34L)).thenReturn(true);
        when(userRepository.findDistinctBySubscribedCourses_IdOrderByIdAsc(34L)).thenReturn(List.of());
        when(exportProperties.getDirectory()).thenReturn(tempDir.toString());
        when(exportProperties.getS3Prefix()).thenReturn("");
        when(s3Properties.getBucketName()).thenReturn("education-nearby-demo-bucket");
        when(s3Client.putObject(any(PutObjectRequest.class), any(Path.class)))
                .thenThrow(new RuntimeException(longMessage));

        subscriberExportProcessor.processExportJob(8L);

        assertThat(job.getStatus()).isEqualTo(SubscriberExportStatus.FAILED);
        assertThat(job.getErrorMessage()).hasSize(1024);
        assertThat(job.getCompletedAt()).isNotNull();
    }

    private SubscriberExportJob buildJob(
            Long id,
            SubscriberExportScope scope,
            Long targetId,
            SubscriberExportFormat format
    ) {
        SubscriberExportJob job = new SubscriberExportJob();
        job.setId(id);
        job.setScope(scope);
        job.setTargetId(targetId);
        job.setFormat(format);
        job.setStatus(SubscriberExportStatus.PENDING);
        job.setRequestedByUserId(1L);
        return job;
    }

    private User createUser(Long id) {
        return User.builder()
                .id(id)
                .firstname("Jane")
                .lastname("Doe")
                .email("jane" + id + "@example.com")
                .username("jane" + id)
                .password("secret")
                .role(Role.USER)
                .enabled(true)
                .build();
    }
}
