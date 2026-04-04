package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.enums.SubscriberExportFormat;
import com.dev.education_nearby_server.models.dto.response.SubscriberExportJobResponse;
import com.dev.education_nearby_server.models.dto.response.UserResponse;
import com.dev.education_nearby_server.services.LyceumService;
import com.dev.education_nearby_server.services.SubscriberExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints for lyceum subscriptions, subscribers listing, and subscribers exports.
 */
@RestController
@RequestMapping("/api/v1/lyceums")
@RequiredArgsConstructor
public class LyceumSubscriberController {

    private final LyceumService lyceumService;
    private final SubscriberExportService subscriberExportService;

    /**
     * Subscribes the authenticated user to a lyceum.
     *
     * @param lyceumId lyceum identifier
     * @return empty 204 on success
     */
    @PostMapping("/{lyceumId}/subscribe")
    public ResponseEntity<Void> subscribeToLyceum(@PathVariable Long lyceumId) {
        lyceumService.subscribeToLyceum(lyceumId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Unsubscribes the authenticated user from a lyceum.
     *
     * @param lyceumId lyceum identifier
     * @return empty 204 on success
     */
    @DeleteMapping("/{lyceumId}/subscribe")
    public ResponseEntity<Void> unsubscribeFromLyceum(@PathVariable Long lyceumId) {
        lyceumService.unsubscribeFromLyceum(lyceumId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lists users subscribed to a lyceum.
     *
     * @param lyceumId lyceum identifier
     * @return subscribers associated with the lyceum
     */
    @GetMapping("/{lyceumId}/subscribers")
    public ResponseEntity<List<UserResponse>> getLyceumSubscribers(@PathVariable Long lyceumId) {
        return ResponseEntity.ok(lyceumService.getLyceumSubscribers(lyceumId));
    }

    /**
     * Starts asynchronous export generation for lyceum subscribers.
     *
     * @param lyceumId lyceum identifier
     * @param format export format (CSV or XLSX)
     * @return export job metadata
     */
    @PostMapping("/{lyceumId}/subscribers/export")
    public ResponseEntity<SubscriberExportJobResponse> exportLyceumSubscribers(
            @PathVariable Long lyceumId,
            @RequestParam SubscriberExportFormat format
    ) {
        SubscriberExportJobResponse response = subscriberExportService.createLyceumSubscribersExport(lyceumId, format);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Returns status for a previously requested lyceum subscriber export.
     *
     * @param lyceumId lyceum identifier
     * @param exportId export job identifier
     * @return export job metadata
     */
    @GetMapping("/{lyceumId}/subscribers/export/{exportId}")
    public ResponseEntity<SubscriberExportJobResponse> getLyceumSubscribersExportStatus(
            @PathVariable Long lyceumId,
            @PathVariable Long exportId
    ) {
        return ResponseEntity.ok(subscriberExportService.getLyceumSubscribersExportStatus(lyceumId, exportId));
    }

    /**
     * Returns a presigned S3 URL for the generated lyceum subscribers export file.
     *
     * @param lyceumId lyceum identifier
     * @param exportId export job identifier
     * @return payload containing the download URL and expiration metadata
     */
    @GetMapping("/{lyceumId}/subscribers/export/{exportId}/download-url")
    public ResponseEntity<SubscriberExportService.ExportDownload> downloadLyceumSubscribersExport(
            @PathVariable Long lyceumId,
            @PathVariable Long exportId
    ) {
        SubscriberExportService.ExportDownload download = subscriberExportService.downloadLyceumSubscribersExport(lyceumId, exportId);
        return ResponseEntity.ok(download);
    }
}
