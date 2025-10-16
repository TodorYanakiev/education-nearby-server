package com.dev.education_nearby_server.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dev.education_nearby_server.enums.Permission.ADMIN_CREATE;
import static com.dev.education_nearby_server.enums.Permission.ADMIN_DELETE;
import static com.dev.education_nearby_server.enums.Permission.ADMIN_READ;
import static com.dev.education_nearby_server.enums.Permission.ADMIN_UPDATE;
import static com.dev.education_nearby_server.enums.Permission.USER_READ;

@Getter
@RequiredArgsConstructor
public enum Role {

    USER(Set.of(USER_READ)),
    ADMIN(
            Set.of(
                    ADMIN_READ,
                    ADMIN_UPDATE,
                    ADMIN_DELETE,
                    ADMIN_CREATE
            )
    );

    private final Set<Permission> permissions;

    public List<SimpleGrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = getPermissions()
                .stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getType()))
                .collect(Collectors.toList());

        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));
        return authorities;
    }
}
