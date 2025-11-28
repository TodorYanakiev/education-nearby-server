package com.dev.education_nearby_server.models.dto.response;

import com.dev.education_nearby_server.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representation of user details exposed by the public API.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String firstname;
    private String lastname;
    private String email;
    private String username;
    private Role role;
    private Long administratedLyceumId;
    private boolean enabled;
}
