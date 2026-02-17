package com.dev.education_nearby_server.models.dto.request;

import com.dev.education_nearby_server.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload for updating a user's global role.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleUpdateRequest {

    @NotNull(message = "Role must not be null.")
    private Role role;
}
