package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.exceptions.common.NoSuchElementException;
import com.dev.education_nearby_server.exceptions.common.UnauthorizedException;
import com.dev.education_nearby_server.exceptions.common.ValidationException;
import com.dev.education_nearby_server.models.dto.auth.ChangePasswordRequest;
import com.dev.education_nearby_server.models.dto.response.UserResponse;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;

/**
 * User-facing operations for account maintenance.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository repository;

    /**
     * Returns a view of every user in the system. Intended for administrative dashboards.
     *
     * @return all users with public fields only
     */
    public List<UserResponse> getAllUsers() {
        return repository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Fetches a single user by id.
     *
     * @param userId user identifier
     * @return user with the given id
     */
    public UserResponse getUserById(Long userId) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User with id " + userId + " not found."));
        return mapToResponse(user);
    }

    /**
     * Returns the currently authenticated user.
     *
     * @param connectedUser authenticated principal
     * @return authenticated user representation
     */
    public UserResponse getAuthenticatedUser(Principal connectedUser) {
        User user = resolveUser(connectedUser);
        return mapToResponse(user);
    }

    /**
     * Changes the authenticated user's password after validating the current password
     * and new/confirmation match.
     *
     * @param request password change payload
     * @param connectedUser authenticated principal requesting the change
     */
    public void changePassword(ChangePasswordRequest request, Principal connectedUser) {
        User user = resolveUser(connectedUser);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ValidationException("Wrong password");
        }

        if (!request.getNewPassword().equals(request.getConfirmationPassword())) {
            throw new ValidationException("Password are not the same");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        repository.save(user);
    }

    private User resolveUser(Principal connectedUser) {
        if (!(connectedUser instanceof UsernamePasswordAuthenticationToken authToken)) {
            throw new UnauthorizedException("You must be authenticated to perform this action.");
        }
        Object principal = authToken.getPrincipal();
        if (!(principal instanceof User user)) {
            throw new UnauthorizedException("You must be authenticated to perform this action.");
        }
        return repository.findById(user.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found."));
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .administratedLyceumId(user.getAdministratedLyceum() != null ? user.getAdministratedLyceum().getId() : null)
                .enabled(user.isEnabled())
                .build();
    }
}
