package com.dev.education_nearby_server.models.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class LyceumUpdateRequest {

    @NotBlank(message = "Lyceum name must not be blank.")
    private String name;

    private String chitalishtaUrl;

    private String status;

    private String bulstat;

    private String chairman;

    private String secretary;

    private String phone;

    @Email(message = "Invalid email format.")
    private String email;

    private String region;

    private String municipality;

    @NotBlank(message = "Town must not be blank.")
    private String town;

    private String address;

    private String urlToLibrariesSite;

    private Integer registrationNumber;

    private Double longitude;

    private Double latitude;
}
