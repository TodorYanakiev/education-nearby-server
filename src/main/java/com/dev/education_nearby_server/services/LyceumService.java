package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.enums.TokenType;
import com.dev.education_nearby_server.exceptions.common.UnauthorizedException;
import com.dev.education_nearby_server.models.dto.request.LyceumRightsRequest;
import com.dev.education_nearby_server.models.entity.Lyceum;
import com.dev.education_nearby_server.models.entity.Token;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.repositories.LyceumRepository;
import com.dev.education_nearby_server.repositories.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LyceumService {

    private final LyceumRepository lyceumRepository;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;

    public String requestRightsOverLyceum(LyceumRightsRequest request) {
        String normalizedName = normalize(request.getLyceumName());
        String normalizedTown = normalize(request.getTown());

        Optional<Lyceum> lyceumOpt = lyceumRepository.findFirstByNameIgnoreCaseAndTownIgnoreCase(
                normalizedName,
                normalizedTown
        );

        if (lyceumOpt.isEmpty()) {
            return "We are sorry, we could not find such lyceum. Please contact us.";
        }

        Lyceum lyceum = lyceumOpt.get();
        String lyceumEmail = lyceum.getEmail();
        if (lyceumEmail == null || lyceumEmail.isBlank()) {
            return "We could not reach the lyceum via email. Please contact us.";
        }

        User currentUser = getCurrentUser()
                .orElseThrow(() -> new UnauthorizedException("You must be authenticated to request rights."));

        String tokenValue = UUID.randomUUID().toString();
        Token token = Token.builder()
                .user(currentUser)
                .tokenValue(tokenValue)
                .tokenType(TokenType.VERIFICATION)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);

        emailService.sendLyceumVerificationEmail(lyceumEmail, normalizedName, normalizedTown, tokenValue);

        return "We have sent them an email at " + lyceumEmail + ". If the email is outdated, please contact us.";
    }

    private String normalize(String input) {
        if (input == null) return null;
        String cleaned = input
                .trim()
                .replaceAll("\\s+", " ");
        return cleaned;
    }

    private Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof User user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }
}
