package com.dev.education_nearby_server.controllers;

import com.dev.education_nearby_server.models.dto.request.LyceumRightsRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumRightsVerificationRequest;
import com.dev.education_nearby_server.models.entity.Lyceum;
import com.dev.education_nearby_server.services.LyceumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    public ResponseEntity<List<Lyceum>> getAllLyceums() {
        return ResponseEntity.ok(lyceumService.getAllLyceums());
    }

    @GetMapping("/verified")
    public ResponseEntity<List<Lyceum>> getVerifiedLyceums() {
        return ResponseEntity.ok(lyceumService.getVerifiedLyceums());
    }

    @PostMapping("/request-rights")
    public ResponseEntity<String> requestRightsOverLyceum(@Valid @RequestBody LyceumRightsRequest request) {
        return ResponseEntity.ok(lyceumService.requestRightsOverLyceum(request));
    }

    @PostMapping("/verify-rights")
    public ResponseEntity<String> verifyRightsOverLyceum(@Valid @RequestBody LyceumRightsVerificationRequest request) {
        return ResponseEntity.ok(lyceumService.verifyRightsOverLyceum(request));
    }
}
