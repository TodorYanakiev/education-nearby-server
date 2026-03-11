package com.dev.education_nearby_server.models.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Course representation used for filtering responses with lyceum location details.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class CourseFilterResponse extends CourseResponse {
    private String lyceumTown;
    private String lyceumAddress;
}
