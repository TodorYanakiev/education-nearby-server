package com.dev.education_nearby_server.models.dto.response;

import com.dev.education_nearby_server.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LyceumResponse {

    private Long id;
    private String name;
    private String chitalishtaUrl;
    private String status;
    private String bulstat;
    private String chairman;
    private String secretary;
    private String phone;
    private String email;
    private String region;
    private String municipality;
    private String town;
    private String address;
    private String urlToLibrariesSite;
    private Integer registrationNumber;
    private VerificationStatus verificationStatus;
}
