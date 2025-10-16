package com.dev.education_nearby_server.integration.services;

import com.dev.education_nearby_server.enums.Role;
import com.dev.education_nearby_server.models.dto.auth.ChangePasswordRequest;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.repositories.UserRepository;
import com.dev.education_nearby_server.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserServiceIT {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void changePasswordUpdatesStoredHash() {
        User user = User.builder()
                .firstname("Dale")
                .lastname("Cooper")
                .email("dale.cooper@example.com")
                .username("dale.cooper@example.com")
                .password(passwordEncoder.encode("OldPassword123"))
                .role(Role.USER)
                .enabled(true)
                .build();
        User saved = userRepository.save(user);
        Principal principal = new UsernamePasswordAuthenticationToken(saved, null, saved.getAuthorities());

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123")
                .newPassword("NewPassword456")
                .confirmationPassword("NewPassword456")
                .build();

        userService.changePassword(request, principal);

        User reloaded = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NewPassword456", reloaded.getPassword())).isTrue();
    }
}
