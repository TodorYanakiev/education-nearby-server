package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.config.ExportProperties;
import com.dev.education_nearby_server.enums.SubscriberExportFormat;
import com.dev.education_nearby_server.enums.SubscriberExportScope;
import com.dev.education_nearby_server.enums.SubscriberExportStatus;
import com.dev.education_nearby_server.models.entity.SubscriberExportJob;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.repositories.CourseRepository;
import com.dev.education_nearby_server.repositories.LyceumRepository;
import com.dev.education_nearby_server.repositories.SubscriberExportJobRepository;
import com.dev.education_nearby_server.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Background processor responsible for generating subscriber export files.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriberExportProcessor {

    private static final String CSV_CONTENT_TYPE = "text/csv";
    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String[] HEADER = {"id", "firstname", "lastname", "email", "username", "role", "enabled"};

    private final SubscriberExportJobRepository exportJobRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final LyceumRepository lyceumRepository;
    private final ExportProperties exportProperties;

    @Async("exportTaskExecutor")
    public void processExportJob(Long exportJobId) {
        SubscriberExportJob job = exportJobRepository.findById(exportJobId).orElse(null);
        if (job == null) {
            log.warn("Export job {} disappeared before processing started.", exportJobId);
            return;
        }

        try {
            markInProgress(job);
            ensureTargetStillExists(job);

            List<User> subscribers = loadSubscribers(job.getScope(), job.getTargetId());
            Path outputPath = createOutputPath(job);
            Files.createDirectories(outputPath.getParent());

            GeneratedFile generatedFile = generateFile(outputPath, job.getFormat(), subscribers);
            job.setStatus(SubscriberExportStatus.COMPLETED);
            job.setFilePath(outputPath.toString());
            job.setFileName(generatedFile.fileName());
            job.setContentType(generatedFile.contentType());
            job.setCompletedAt(LocalDateTime.now());
            job.setErrorMessage(null);
            exportJobRepository.save(job);
            log.info("Completed subscriber export job {} scope={} targetId={} file={}",
                    job.getId(), job.getScope(), job.getTargetId(), generatedFile.fileName());
        } catch (Exception exception) {
            log.error("Failed subscriber export job {} scope={} targetId={}",
                    job.getId(), job.getScope(), job.getTargetId(), exception);
            markFailed(job, exception.getMessage());
        }
    }

    private void markInProgress(SubscriberExportJob job) {
        job.setStatus(SubscriberExportStatus.IN_PROGRESS);
        job.setErrorMessage(null);
        exportJobRepository.save(job);
    }

    private void markFailed(SubscriberExportJob job, String message) {
        job.setStatus(SubscriberExportStatus.FAILED);
        job.setErrorMessage(truncateError(message));
        job.setCompletedAt(LocalDateTime.now());
        exportJobRepository.save(job);
    }

    private void ensureTargetStillExists(SubscriberExportJob job) {
        boolean exists = switch (job.getScope()) {
            case COURSE -> courseRepository.existsById(job.getTargetId());
            case LYCEUM -> lyceumRepository.existsById(job.getTargetId());
        };
        if (!exists) {
            throw new IllegalStateException("Target resource no longer exists.");
        }
    }

    private List<User> loadSubscribers(SubscriberExportScope scope, Long targetId) {
        return switch (scope) {
            case COURSE -> userRepository.findDistinctBySubscribedCourses_IdOrderByIdAsc(targetId);
            case LYCEUM -> userRepository.findDistinctBySubscribedLyceums_IdOrderByIdAsc(targetId);
        };
    }

    private Path createOutputPath(SubscriberExportJob job) {
        String extension = job.getFormat() == SubscriberExportFormat.CSV ? "csv" : "xlsx";
        String prefix = job.getScope() == SubscriberExportScope.COURSE ? "course" : "lyceum";
        String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMAT);
        String fileName = prefix + "-" + job.getTargetId() + "-subscribers-" + job.getId() + "-" + timestamp + "." + extension;
        return Path.of(exportProperties.getDirectory()).toAbsolutePath().normalize().resolve(fileName);
    }

    private GeneratedFile generateFile(Path outputPath, SubscriberExportFormat format, List<User> subscribers) throws IOException {
        return switch (format) {
            case CSV -> writeCsv(outputPath, subscribers);
            case XLSX, EXCEL -> writeXlsx(outputPath, subscribers);
        };
    }

    private GeneratedFile writeCsv(Path outputPath, List<User> subscribers) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                Files.newOutputStream(outputPath), StandardCharsets.UTF_8))) {
            writer.write(String.join(",", HEADER));
            writer.newLine();
            for (User user : subscribers) {
                writer.write(csvRow(user));
                writer.newLine();
            }
        }
        return new GeneratedFile(outputPath.getFileName().toString(), CSV_CONTENT_TYPE);
    }

    private GeneratedFile writeXlsx(Path outputPath, List<User> subscribers) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             OutputStream outputStream = Files.newOutputStream(outputPath)) {
            SXSSFSheet sheet = workbook.createSheet("Subscribers");
            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < HEADER.length; index++) {
                headerRow.createCell(index).setCellValue(HEADER[index]);
            }

            int rowIndex = 1;
            for (User user : subscribers) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(user.getId() != null ? user.getId() : 0L);
                row.createCell(1).setCellValue(safe(user.getFirstname()));
                row.createCell(2).setCellValue(safe(user.getLastname()));
                row.createCell(3).setCellValue(safe(user.getEmail()));
                row.createCell(4).setCellValue(safe(user.getUsername()));
                row.createCell(5).setCellValue(user.getRole() != null ? user.getRole().name() : "");
                row.createCell(6).setCellValue(user.isEnabled());
            }

            workbook.write(outputStream);
            workbook.dispose();
        }
        return new GeneratedFile(outputPath.getFileName().toString(), XLSX_CONTENT_TYPE);
    }

    private String csvRow(User user) {
        return String.join(",",
                toCsvValue(user.getId() != null ? user.getId().toString() : ""),
                toCsvValue(user.getFirstname()),
                toCsvValue(user.getLastname()),
                toCsvValue(user.getEmail()),
                toCsvValue(user.getUsername()),
                toCsvValue(user.getRole() != null ? user.getRole().name() : ""),
                toCsvValue(Boolean.toString(user.isEnabled()))
        );
    }

    private String toCsvValue(String value) {
        String safeValue = safe(value);
        String escaped = safeValue.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String truncateError(String message) {
        if (message == null) {
            return "Unknown export failure.";
        }
        return message.length() > 1024 ? message.substring(0, 1024) : message;
    }

    private record GeneratedFile(String fileName, String contentType) {
    }
}
