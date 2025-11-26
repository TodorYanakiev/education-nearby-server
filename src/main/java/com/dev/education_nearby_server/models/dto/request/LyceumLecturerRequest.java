package com.dev.education_nearby_server.models.dto.request;

import jakarta.validation.constraints.NotNull;
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
public class LyceumLecturerRequest {

    @NotNull(message = "User id must not be null.")
    private Long userId;

    private Long lyceumId;
}
