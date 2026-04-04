package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.config.ExportProperties;
import com.dev.education_nearby_server.config.S3Properties;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Coordinates asynchronous subscriber export requests, status, and file access.
 */
@Service
@RequiredArgsConstructor
public class SubscriberExportService {

    private final SubscriberExportJobRepository exportJobRepository;
    private final UserRepository userRepository;
    private final CourseService courseService;
    private final LyceumService lyceumService;
    private final SubscriberExportProcessor exportProcessor;
    private final S3Properties s3Properties;
    private final ExportProperties exportProperties;
    private final S3Presigner s3Presigner;

    public SubscriberExportJobResponse createCourseSubscribersExport(Long courseId, SubscriberExportFormat format) {
        courseService.ensureCurrentUserCanAccessCourseSubscribers(courseId);
        return createExport(SubscriberExportScope.COURSE, courseId, format);
    }

    public SubscriberExportJobResponse createLyceumSubscribersExport(Long lyceumId, SubscriberExportFormat format) {
        lyceumService.ensureCurrentUserCanAccessLyceumSubscribers(lyceumId);
        return createExport(SubscriberExportScope.LYCEUM, lyceumId, format);
    }

    @Transactional(readOnly = true)
    public SubscriberExportJobResponse getCourseSubscribersExportStatus(Long courseId, Long exportId) {
        courseService.ensureCurrentUserCanAccessCourseSubscribers(courseId);
        SubscriberExportJob job = requireJob(exportId, SubscriberExportScope.COURSE, courseId);
        return mapToResponse(job);
    }

    @Transactional(readOnly = true)
    public SubscriberExportJobResponse getLyceumSubscribersExportStatus(Long lyceumId, Long exportId) {
        lyceumService.ensureCurrentUserCanAccessLyceumSubscribers(lyceumId);
        SubscriberExportJob job = requireJob(exportId, SubscriberExportScope.LYCEUM, lyceumId);
        return mapToResponse(job);
    }

    @Transactional(readOnly = true)
    public ExportDownload downloadCourseSubscribersExport(Long courseId, Long exportId) {
        courseService.ensureCurrentUserCanAccessCourseSubscribers(courseId);
        SubscriberExportJob job = requireJob(exportId, SubscriberExportScope.COURSE, courseId);
        return buildDownloadLink(job);
    }

    @Transactional(readOnly = true)
    public ExportDownload downloadLyceumSubscribersExport(Long lyceumId, Long exportId) {
        lyceumService.ensureCurrentUserCanAccessLyceumSubscribers(lyceumId);
        SubscriberExportJob job = requireJob(exportId, SubscriberExportScope.LYCEUM, lyceumId);
        return buildDownloadLink(job);
    }

    private SubscriberExportJobResponse createExport(
            SubscriberExportScope scope,
            Long targetId,
            SubscriberExportFormat format
    ) {
        if (format == null) {
            throw new BadRequestException("Export format must be provided.");
        }
        User requester = getManagedCurrentUser();
        SubscriberExportJob job = SubscriberExportJob.builder()
                .scope(scope)
                .targetId(targetId)
                .format(format)
                .status(SubscriberExportStatus.PENDING)
                .requestedByUserId(requester.getId())
                .build();
        SubscriberExportJob saved = exportJobRepository.save(job);
        exportProcessor.processExportJob(saved.getId());
        return mapToResponse(saved);
    }

    private SubscriberExportJob requireJob(Long exportId, SubscriberExportScope scope, Long targetId) {
        return exportJobRepository.findByIdAndScopeAndTargetId(exportId, scope, targetId)
                .orElseThrow(() -> new NoSuchElementException("Export job with id " + exportId + " not found."));
    }

    private ExportDownload buildDownloadLink(SubscriberExportJob job) {
        if (job.getStatus() == SubscriberExportStatus.FAILED) {
            String message = job.getErrorMessage() == null ? "Export generation failed." : job.getErrorMessage();
            throw new BadRequestException(message);
        }
        if (job.getStatus() != SubscriberExportStatus.COMPLETED) {
            throw new ConflictException("Export file is not ready yet.");
        }
        if (job.getFilePath() == null || job.getFilePath().isBlank()) {
            throw new NoSuchElementException("Export file not found.");
        }

        String bucketName = requiredBucketName();
        String objectKey = job.getFilePath().trim();
        String contentType = job.getContentType() != null
                ? job.getContentType()
                : "application/octet-stream";
        String fileName = (job.getFileName() != null && !job.getFileName().isBlank())
                ? job.getFileName()
                : objectKey.substring(objectKey.lastIndexOf('/') + 1);
        Duration presignedDuration = resolvePresignedDuration();
        Instant expiresAt = Instant.now().plus(presignedDuration);

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .responseContentType(contentType)
                    .responseContentDisposition("attachment; filename=\"" + fileName + "\"")
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(presignedDuration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return new ExportDownload(
                    URI.create(presignedRequest.url().toString()),
                    fileName,
                    expiresAt
            );
        } catch (Exception exception) {
            throw new InternalServerErrorException("Could not generate export download URL.");
        }
    }

    private SubscriberExportJobResponse mapToResponse(SubscriberExportJob job) {
        return SubscriberExportJobResponse.builder()
                .id(job.getId())
                .scope(job.getScope())
                .targetId(job.getTargetId())
                .format(job.getFormat())
                .status(job.getStatus())
                .fileName(job.getFileName())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .completedAt(job.getCompletedAt())
                .build();
    }

    private User getManagedCurrentUser() {
        User currentUser = getCurrentUser()
                .orElseThrow(() -> new UnauthorizedException("You must be authenticated to perform this action."));
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found."));
    }

    private Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof User user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    private String requiredBucketName() {
        String bucketName = s3Properties.getBucketName();
        if (bucketName == null || bucketName.isBlank()) {
            throw new InternalServerErrorException("S3 bucket name is not configured.");
        }
        return bucketName.trim();
    }

    private Duration resolvePresignedDuration() {
        int minutes = exportProperties.getPresignedUrlMinutes();
        if (minutes <= 0) {
            minutes = 10;
        }
        return Duration.ofMinutes(minutes);
    }

    public record ExportDownload(URI url, String fileName, Instant expiresAt) {
        public ExportDownload(URI url) {
            this(url, null, null);
        }
    }
}
