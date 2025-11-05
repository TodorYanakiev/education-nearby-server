package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.enums.Role;
import com.dev.education_nearby_server.enums.TokenType;
import com.dev.education_nearby_server.enums.VerificationStatus;
import com.dev.education_nearby_server.exceptions.common.AccessDeniedException;
import com.dev.education_nearby_server.exceptions.common.BadRequestException;
import com.dev.education_nearby_server.exceptions.common.ConflictException;
import com.dev.education_nearby_server.exceptions.common.NoSuchElementException;
import com.dev.education_nearby_server.exceptions.common.UnauthorizedException;
import com.dev.education_nearby_server.models.dto.request.LyceumRightsRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumRightsVerificationRequest;
import com.dev.education_nearby_server.models.entity.Lyceum;
import com.dev.education_nearby_server.models.entity.Token;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.repositories.LyceumRepository;
import com.dev.education_nearby_server.repositories.TokenRepository;
import com.dev.education_nearby_server.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LyceumService {

    private final LyceumRepository lyceumRepository;
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public String requestRightsOverLyceum(LyceumRightsRequest request) {
        String normalizedName = normalize(request.getLyceumName());
        String normalizedTown = normalize(request.getTown());
        Optional<Lyceum> lyceumOpt =
                lyceumRepository.findFirstByNameIgnoreCaseAndTownIgnoreCase(normalizedName, normalizedTown);
        if (lyceumOpt.isEmpty()) {
            return "We are sorry, we could not find such lyceum. Please contact us.";
        }
        Lyceum lyceum = lyceumOpt.get();
        if (!hasReachableEmail(lyceum)) {
            return "We could not reach the lyceum via email. Please contact us.";
        }

        User currentUser = getManagedCurrentUser();
        String response = handleExistingAdministration(currentUser, lyceum);
        if (response != null) {
            return response;
        }
        invalidateExistingVerificationTokens(currentUser);
        String tokenValue = createVerificationToken(currentUser, lyceum);
        emailService.sendLyceumVerificationEmail(
                lyceum.getEmail(), normalizedName, normalizedTown, tokenValue);
        return "We have sent you an email at " + lyceum.getEmail() + " with a verification code.";
    }

    public String verifyRightsOverLyceum(LyceumRightsVerificationRequest request) {
        String code = extractVerificationCode(request);
        User currentUser = getManagedCurrentUser();
        Token token = getValidVerificationToken(code);
        ensureTokenBelongsToUser(token, currentUser);
        Lyceum lyceum = requireLyceum(token);
        ensureUserNotAdminOfOtherLyceum(currentUser, lyceum);
        assignLyceumAdministration(currentUser, lyceum);
        expireToken(token);
        return "You are now the administrator of " + lyceum.getName() + " in " + lyceum.getTown() + ".";
    }

    public List<Lyceum> getVerifiedLyceums() {
        return lyceumRepository.findAllByVerificationStatus(VerificationStatus.VERIFIED);
    }

    public List<Lyceum> getAllLyceums() {
        return lyceumRepository.findAll();
    }

    public Lyceum getLyceumById(Long id) {
        Lyceum lyceum = lyceumRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Lyceum with id " + id + " not found."));
        if (lyceum.getVerificationStatus() != VerificationStatus.VERIFIED) {
            User currentUser = getCurrentUser()
                    .orElseThrow(() -> new UnauthorizedException("You must be authenticated to access this lyceum."));
            if (currentUser.getRole() != Role.ADMIN) {
                throw new AccessDeniedException("You do not have permission to access this lyceum.");
            }
        }
        return lyceum;
    }

    private String normalize(String input) {
        if (input == null) return null;
        return input
                .trim()
                .replaceAll("\\s+", " ");
    }

    private boolean hasReachableEmail(Lyceum lyceum) {
        String email = lyceum.getEmail();
        return email != null && !email.isBlank();
    }

    private String handleExistingAdministration(User user, Lyceum lyceum) {
        Lyceum administrated = user.getAdministratedLyceum();
        if (administrated == null) {
            return null;
        }
        if (administrated.getId().equals(lyceum.getId())) {
            return "You already administrate this lyceum.";
        }
        throw new ConflictException("You are already an administrator of another lyceum.");
    }

    private String createVerificationToken(User user, Lyceum lyceum) {
        String tokenValue = UUID.randomUUID().toString();
        Token token = Token.builder()
                .user(user)
                .lyceum(lyceum)
                .tokenValue(tokenValue)
                .tokenType(TokenType.VERIFICATION)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
        return tokenValue;
    }

    private String extractVerificationCode(LyceumRightsVerificationRequest request) {
        String code = request.getVerificationCode();
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Verification code must be provided.");
        }
        return code.trim();
    }

    private Token getValidVerificationToken(String code) {
        Token token = tokenRepository.findByToken(code)
                .orElseThrow(() -> new BadRequestException("Invalid verification code."));
        if (token.getTokenType() != TokenType.VERIFICATION) {
            throw new BadRequestException("Invalid verification code.");
        }
        if (token.isExpired() || token.isRevoked()) {
            throw new BadRequestException("Verification code has already been used or expired.");
        }
        return token;
    }

    private void ensureTokenBelongsToUser(Token token, User user) {
        if (token.getUser() == null || !token.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not allowed to use this verification code.");
        }
    }

    private Lyceum requireLyceum(Token token) {
        Lyceum lyceum = token.getLyceum();
        if (lyceum == null) {
            throw new BadRequestException("Verification code is not associated with a lyceum.");
        }
        return lyceum;
    }

    private void ensureUserNotAdminOfOtherLyceum(User user, Lyceum lyceum) {
        Lyceum administrated = user.getAdministratedLyceum();
        if (administrated != null && !administrated.getId().equals(lyceum.getId())) {
            throw new ConflictException("You are already an administrator of another lyceum.");
        }
    }

    private void assignLyceumAdministration(User user, Lyceum lyceum) {
        user.setAdministratedLyceum(lyceum);
        lyceum.setVerificationStatus(VerificationStatus.VERIFIED);
        lyceumRepository.save(lyceum);
        userRepository.save(user);
    }

    private void expireToken(Token token) {
        token.setExpired(true);
        token.setRevoked(true);
        tokenRepository.save(token);
    }

    private User getManagedCurrentUser() {
        User currentUser = getCurrentUser()
                .orElseThrow(() -> new UnauthorizedException("You must be authenticated to perform this action."));
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found."));
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

    private void invalidateExistingVerificationTokens(User user) {
        List<Token> activeTokens = tokenRepository.findAllValidTokenByUser(user.getId())
                .stream()
                .filter(token -> token.getTokenType() == TokenType.VERIFICATION)
                .toList();

        if (activeTokens.isEmpty()) {
            return;
        }

        activeTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });

        tokenRepository.saveAll(activeTokens);
    }
}
