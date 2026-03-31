package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.enums.SubscriberExportFormat;
import com.dev.education_nearby_server.enums.SubscriberExportScope;
import com.dev.education_nearby_server.enums.SubscriberExportStatus;
import com.dev.education_nearby_server.exceptions.common.BadRequestException;
import com.dev.education_nearby_server.exceptions.common.ConflictException;
import com.dev.education_nearby_server.exceptions.common.NoSuchElementException;
import com.dev.education_nearby_server.exceptions.common.UnauthorizedException;
import com.dev.education_nearby_server.models.dto.response.SubscriberExportJobResponse;
import com.dev.education_nearby_server.models.entity.SubscriberExportJob;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.repositories.SubscriberExportJobRepository;
import com.dev.education_nearby_server.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.PathResource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
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
    public ExportFile downloadCourseSubscribersExport(Long courseId, Long exportId) {
        courseService.ensureCurrentUserCanAccessCourseSubscribers(courseId);
        SubscriberExportJob job = requireJob(exportId, SubscriberExportScope.COURSE, courseId);
        return buildExportFile(job);
    }

    @Transactional(readOnly = true)
    public ExportFile downloadLyceumSubscribersExport(Long lyceumId, Long exportId) {
        lyceumService.ensureCurrentUserCanAccessLyceumSubscribers(lyceumId);
        SubscriberExportJob job = requireJob(exportId, SubscriberExportScope.LYCEUM, lyceumId);
        return buildExportFile(job);
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

    private ExportFile buildExportFile(SubscriberExportJob job) {
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

        Path path = Path.of(job.getFilePath()).toAbsolutePath().normalize();
        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new NoSuchElementException("Export file not found.");
        }

        String contentType = job.getContentType() != null
                ? job.getContentType()
                : "application/octet-stream";
        String fileName = (job.getFileName() != null && !job.getFileName().isBlank())
                ? job.getFileName()
                : path.getFileName().toString();

        return new ExportFile(new PathResource(path), fileName, contentType);
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

    public record ExportFile(PathResource resource, String fileName, String contentType) {
    }
}
