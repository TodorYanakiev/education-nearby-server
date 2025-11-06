package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.models.dto.request.LyceumCreateRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumRightsRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumRightsVerificationRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumUpdateRequest;
import com.dev.education_nearby_server.models.dto.response.LyceumResponse;
import com.dev.education_nearby_server.services.LyceumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lyceums")
@RequiredArgsConstructor
public class LyceumController {

    private final LyceumService lyceumService;

    @GetMapping
    public ResponseEntity<List<LyceumResponse>> getAllLyceums() {
        return ResponseEntity.ok(lyceumService.getAllLyceums());
    }

    @GetMapping("/verified")
    public ResponseEntity<List<LyceumResponse>> getVerifiedLyceums() {
        return ResponseEntity.ok(lyceumService.getVerifiedLyceums());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LyceumResponse> getLyceumById(@PathVariable Long id) {
        return ResponseEntity.ok(lyceumService.getLyceumById(id));
    }

    @PostMapping("/request-rights")
    public ResponseEntity<String> requestRightsOverLyceum(@Valid @RequestBody LyceumRightsRequest request) {
        return ResponseEntity.ok(lyceumService.requestRightsOverLyceum(request));
    }

    @PostMapping
    public ResponseEntity<LyceumResponse> createLyceum(@Valid @RequestBody LyceumCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lyceumService.createLyceum(request));
    }

    @PostMapping("/verify-rights")
    public ResponseEntity<String> verifyRightsOverLyceum(@Valid @RequestBody LyceumRightsVerificationRequest request) {
        return ResponseEntity.ok(lyceumService.verifyRightsOverLyceum(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LyceumResponse> updateLyceum(
            @PathVariable Long id,
            @Valid @RequestBody LyceumUpdateRequest request
    ) {
        return ResponseEntity.ok(lyceumService.updateLyceum(id, request));
    }

    @PutMapping("/{lyceumId}/administrators/{userId}")
    public ResponseEntity<Void> assignAdministrator(
            @PathVariable Long lyceumId,
            @PathVariable Long userId
    ) {
        lyceumService.assignAdministrator(lyceumId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteLyceum(@PathVariable Long id) {
        lyceumService.deleteLyceum(id);
        return ResponseEntity.noContent().build();
    }
}
