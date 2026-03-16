package com.dev.education_nearby_server.config;

import com.dev.education_nearby_server.models.entity.User;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Supplies the current authenticated user id for Spring Data auditing fields.
 */
public class ApplicationAuditAware implements AuditorAware<Long> {
    /**
     * Returns the authenticated user's id when present, otherwise empty for anonymous contexts.
     */
    @Override
    public Optional<Long> getCurrentAuditor() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();
        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken
        ) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User userPrincipal) {
            return Optional.ofNullable(userPrincipal.getId());
        }
        return Optional.empty();
    }
}
