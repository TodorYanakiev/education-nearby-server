package com.dev.education_nearby_server.models.dto.request;

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
public class LyceumRightsRequest {

    @NotBlank(message = "Lyceum name must not be blank!")
    private String lyceumName;

    @NotBlank(message = "Town must not be blank!")
    private String town;
}
