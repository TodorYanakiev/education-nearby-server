package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.enums.Role;
import com.dev.education_nearby_server.enums.TokenType;
import com.dev.education_nearby_server.enums.VerificationStatus;
import com.dev.education_nearby_server.exceptions.common.AccessDeniedException;
import com.dev.education_nearby_server.exceptions.common.BadRequestException;
import com.dev.education_nearby_server.exceptions.common.ConflictException;
import com.dev.education_nearby_server.exceptions.common.NoSuchElementException;
import com.dev.education_nearby_server.exceptions.common.UnauthorizedException;
import com.dev.education_nearby_server.models.dto.request.LyceumLecturerRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumRightsRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumRightsVerificationRequest;
import com.dev.education_nearby_server.models.dto.request.LyceumRequest;
import com.dev.education_nearby_server.models.dto.response.CourseResponse;
import com.dev.education_nearby_server.models.dto.response.LyceumResponse;
import com.dev.education_nearby_server.models.dto.response.UserResponse;
import com.dev.education_nearby_server.models.entity.Course;
import com.dev.education_nearby_server.models.entity.Lyceum;
import com.dev.education_nearby_server.models.entity.Token;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.repositories.LyceumRepository;
import com.dev.education_nearby_server.repositories.TokenRepository;
import com.dev.education_nearby_server.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Handles lyceum discovery, administration verification, and lecturer/administrator management.
 */
@Service
@RequiredArgsConstructor
public class LyceumService {

    private final LyceumRepository lyceumRepository;
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final CourseService courseService;
    private static final String LYCEUM_ID_MESSAGE = "Lyceum with id ";
    private static final String NOT_FOUND_MESSAGE = " not found.";
    private static final String USER_WITH_ID = "User with id ";

    /**
     * Starts the administrator verification flow for a lyceum by sending a token to the lyceum email.
     *
     * @param request request containing lyceum name/town and requester info
     * @return user-facing message describing the outcome
     */
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

    /**
     * Confirms administrator rights using a verification code and assigns the user as administrator.
     *
     * @param request verification payload containing the token
     * @return confirmation message once rights are granted
     */
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

    /**
     * Lists only lyceums that passed administrator verification.
     *
     * @return verified lyceums
     */
    public List<LyceumResponse> getVerifiedLyceums() {
        return lyceumRepository.findAllByVerificationStatus(VerificationStatus.VERIFIED)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Lists all lyceums regardless of verification status.
     *
     * @return every lyceum
     */
    public List<LyceumResponse> getAllLyceums() {
        return lyceumRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Filters lyceums by town and/or coordinates; only verified lyceums are returned.
     *
     * @param town optional town filter (case-insensitive)
     * @param latitude optional latitude used with longitude
     * @param longitude optional longitude used with latitude
     * @param limit optional max results to return
     * @return lyceums that match the provided filters
     */
    public List<LyceumResponse> filterLyceums(String town, Double latitude, Double longitude, Integer limit) {
        String normalizedTown = normalize(town);
        if (normalizedTown != null && normalizedTown.isBlank()) {
            normalizedTown = null;
        }
        if ((latitude == null) != (longitude == null)) {
            throw new BadRequestException("Both latitude and longitude must be provided to filter by location.");
        }
        if (limit != null && limit <= 0) {
            throw new BadRequestException("Limit must be greater than zero.");
        }

        Pageable pageable = limit != null ? PageRequest.of(0, limit) : Pageable.unpaged();
        List<Lyceum> lyceums = lyceumRepository.filterLyceums(
                normalizedTown,
                latitude,
                longitude,
                VerificationStatus.VERIFIED.name(),
                pageable
        );
        return lyceums.stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Creates a new lyceum record after validating required fields and uniqueness by name and town.
     *
     * @param request lyceum payload to persist
     * @return created lyceum response
     */
    public LyceumResponse createLyceum(LyceumRequest request) {
        if (request == null) {
            throw new BadRequestException("Lyceum payload must not be null.");
        }
        String name = normalize(request.getName());
        String town = normalize(request.getTown());
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Lyceum name must not be blank.");
        }
        if (town == null || town.isBlank()) {
            throw new BadRequestException("Lyceum town must not be blank.");
        }
        boolean exists = lyceumRepository.findFirstByNameIgnoreCaseAndTownIgnoreCase(name, town).isPresent();
        if (exists) {
            throw new ConflictException("Lyceum with the same name and town already exists.");
        }

        Lyceum lyceum = new Lyceum();
        lyceum.setName(name);
        lyceum.setTown(town);
        lyceum.setChitalishtaUrl(normalize(request.getChitalishtaUrl()));
        lyceum.setStatus(normalize(request.getStatus()));
        lyceum.setBulstat(normalize(request.getBulstat()));
        lyceum.setChairman(normalize(request.getChairman()));
        lyceum.setSecretary(normalize(request.getSecretary()));
        lyceum.setPhone(normalize(request.getPhone()));
        lyceum.setEmail(normalize(request.getEmail()));
        lyceum.setRegion(normalize(request.getRegion()));
        lyceum.setMunicipality(normalize(request.getMunicipality()));
        lyceum.setAddress(normalize(request.getAddress()));
        lyceum.setUrlToLibrariesSite(normalize(request.getUrlToLibrariesSite()));
        lyceum.setRegistrationNumber(request.getRegistrationNumber());
        lyceum.setLongitude(request.getLongitude());
        lyceum.setLatitude(request.getLatitude());
        lyceum.setVerificationStatus(VerificationStatus.NOT_VERIFIED);

        Lyceum savedLyceum = lyceumRepository.save(lyceum);
        return mapToResponse(savedLyceum);
    }

    /**
     * Updates lyceum fields while enforcing authorization and uniqueness constraints.
     *
     * @param id lyceum identifier
     * @param request updated lyceum data
     * @return updated lyceum response
     */
    public LyceumResponse updateLyceum(Long id, LyceumRequest request) {
        if (request == null) {
            throw new BadRequestException("Lyceum payload must not be null.");
        }

        Lyceum lyceum = lyceumRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(LYCEUM_ID_MESSAGE + id + NOT_FOUND_MESSAGE));

        User currentUser = getManagedCurrentUser();
        ensureUserCanModifyLyceum(currentUser, lyceum);

        String name = normalize(request.getName());
        String town = normalize(request.getTown());
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Lyceum name must not be blank.");
        }
        if (town == null || town.isBlank()) {
            throw new BadRequestException("Lyceum town must not be blank.");
        }

        lyceumRepository.findFirstByNameIgnoreCaseAndTownIgnoreCase(name, town)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("Lyceum with the same name and town already exists.");
                });

        lyceum.setName(name);
        lyceum.setTown(town);
        applyOptionalString(request.getChitalishtaUrl(), lyceum::setChitalishtaUrl);
        applyOptionalString(request.getStatus(), lyceum::setStatus);
        applyOptionalString(request.getBulstat(), lyceum::setBulstat);
        applyOptionalString(request.getChairman(), lyceum::setChairman);
        applyOptionalString(request.getSecretary(), lyceum::setSecretary);
        applyOptionalString(request.getPhone(), lyceum::setPhone);
        applyOptionalString(request.getEmail(), lyceum::setEmail);
        applyOptionalString(request.getRegion(), lyceum::setRegion);
        applyOptionalString(request.getMunicipality(), lyceum::setMunicipality);
        applyOptionalString(request.getAddress(), lyceum::setAddress);
        applyOptionalString(request.getUrlToLibrariesSite(), lyceum::setUrlToLibrariesSite);
        if (request.getRegistrationNumber() != null) {
            lyceum.setRegistrationNumber(request.getRegistrationNumber());
        }
        if (request.getLongitude() != null) {
            lyceum.setLongitude(request.getLongitude());
        }
        if (request.getLatitude() != null) {
            lyceum.setLatitude(request.getLatitude());
        }

        Lyceum updatedLyceum = lyceumRepository.save(lyceum);
        return mapToResponse(updatedLyceum);
    }

    /**
     * Assigns a user as administrator of a lyceum, ensuring permissions and uniqueness.
     *
     * @param lyceumId lyceum identifier
     * @param userId user to promote
     */
    @Transactional
    public void assignAdministrator(Long lyceumId, Long userId) {
        Lyceum lyceum = lyceumRepository.findById(lyceumId)
                .orElseThrow(() -> new NoSuchElementException(LYCEUM_ID_MESSAGE + lyceumId + NOT_FOUND_MESSAGE));

        User currentUser = getManagedCurrentUser();
        ensureUserCanModifyLyceum(currentUser, lyceum);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException(USER_WITH_ID + userId + NOT_FOUND_MESSAGE));

        Lyceum administrated = user.getAdministratedLyceum();
        if (administrated != null && !administrated.getId().equals(lyceumId)) {
            throw new ConflictException("User already administrates another lyceum.");
        }

        user.setAdministratedLyceum(lyceum);
        lyceum.setVerificationStatus(VerificationStatus.VERIFIED);
        syncAdministratorsCollection(lyceum, user);

        userRepository.save(user);
        lyceumRepository.save(lyceum);
    }

    /**
     * Removes a user from lyceum administrators; admin-only.
     *
     * @param lyceumId lyceum identifier
     * @param userId user identifier to demote
     */
    @Transactional
    public void removeAdministratorFromLyceum(Long lyceumId, Long userId) {
        if (userId == null) {
            throw new BadRequestException("User id must be provided.");
        }
        User currentUser = getManagedCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("You do not have permission to modify this lyceum.");
        }
        Lyceum lyceum = requireLyceum(lyceumId);

        User administrator = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException(USER_WITH_ID + userId + NOT_FOUND_MESSAGE));

        Lyceum administrated = administrator.getAdministratedLyceum();
        if (administrated == null || administrated.getId() == null || !administrated.getId().equals(lyceumId)) {
            throw new BadRequestException("User is not an administrator of this lyceum.");
        }

        if (lyceum.getAdministrators() != null) {
            lyceum.getAdministrators()
                    .removeIf(existing -> existing.getId() != null && existing.getId().equals(userId));
        }
        administrator.setAdministratedLyceum(null);

        userRepository.save(administrator);
        lyceumRepository.save(lyceum);
    }

    /**
     * Adds a lecturer to a lyceum, validating permissions and preventing duplicates.
     *
     * @param request lecturer assignment payload
     */
    @Transactional
    public void addLecturerToLyceum(LyceumLecturerRequest request) {
        User currentUser = getManagedCurrentUser();
        if (request.getUserId() == null) {
            throw new BadRequestException("User id must be provided.");
        }

        Lyceum lyceum = resolveLyceumForLecturerAssignment(currentUser, request);

        User lecturer = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NoSuchElementException(USER_WITH_ID + request.getUserId() + NOT_FOUND_MESSAGE));

        if (lyceum.getLecturers() == null) {
            lyceum.setLecturers(new ArrayList<>());
        }
        boolean alreadyLecturer = lyceum.getLecturers().stream()
                .anyMatch(existing -> existing.getId() != null && existing.getId().equals(lecturer.getId()));
        if (alreadyLecturer) {
            throw new ConflictException("User is already a lecturer for this lyceum.");
        }

        if (lecturer.getLecturedLyceums() == null) {
            lecturer.setLecturedLyceums(new ArrayList<>());
        }

        lyceum.getLecturers().add(lecturer);
        lecturer.getLecturedLyceums().add(lyceum);

        lyceumRepository.save(lyceum);
        userRepository.save(lecturer);
    }

    /**
     * Removes a lecturer from a lyceum after verifying permissions.
     *
     * @param lyceumId lyceum identifier
     * @param userId lecturer identifier
     */
    @Transactional
    public void removeLecturerFromLyceum(Long lyceumId, Long userId) {
        if (userId == null) {
            throw new BadRequestException("User id must be provided.");
        }
        Lyceum lyceum = requireLyceumWithLecturers(lyceumId);

        User currentUser = getManagedCurrentUser();
        ensureUserCanModifyLyceum(currentUser, lyceum);

        User lecturer = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException(USER_WITH_ID + userId + NOT_FOUND_MESSAGE));

        boolean removedFromLyceum = lyceum.getLecturers() != null
                && lyceum.getLecturers().removeIf(existing -> existing.getId() != null && existing.getId().equals(userId));
        boolean removedFromUser = lecturer.getLecturedLyceums() != null
                && lecturer.getLecturedLyceums()
                .removeIf(existing -> existing.getId() != null && existing.getId().equals(lyceum.getId()));
        if (!removedFromLyceum && !removedFromUser) {
            throw new BadRequestException("User is not a lecturer for this lyceum.");
        }

        lyceumRepository.save(lyceum);
        userRepository.save(lecturer);
    }

    private Lyceum resolveLyceumForLecturerAssignment(User currentUser, LyceumLecturerRequest request) {
        if (currentUser.getRole() == Role.ADMIN) {
            if (request.getLyceumId() == null) {
                throw new BadRequestException("Lyceum id must be provided when assigning as admin.");
            }
            return requireLyceum(request.getLyceumId());
        }

        Lyceum lyceum = currentUser.getAdministratedLyceum();
        if (lyceum == null) {
            throw new AccessDeniedException("You do not have permission to modify this lyceum.");
        }
        if (request.getLyceumId() != null && !lyceum.getId().equals(request.getLyceumId())) {
            throw new AccessDeniedException("You do not have permission to modify this lyceum.");
        }
        return lyceum;
    }

    /**
     * Deletes a lyceum and clears administrator relationships and tokens.
     *
     * @param id lyceum identifier
     */
    @Transactional
    public void deleteLyceum(Long id) {
        Lyceum lyceum = lyceumRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(LYCEUM_ID_MESSAGE + id + NOT_FOUND_MESSAGE));

        List<User> administrators = userRepository.findAllByAdministratedLyceum_Id(id);
        if (!administrators.isEmpty()) {
            administrators.forEach(user -> user.setAdministratedLyceum(null));
            userRepository.saveAll(administrators);
        }

        tokenRepository.deleteAllByLyceum_Id(id);
        lyceumRepository.delete(lyceum);
    }

    /**
     * Retrieves a lyceum by id, guarding access to non-verified lyceums for non-admins.
     *
     * @param id lyceum identifier
     * @return lyceum details
     */
    public LyceumResponse getLyceumById(Long id) {
        Lyceum lyceum = lyceumRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(LYCEUM_ID_MESSAGE + id + NOT_FOUND_MESSAGE));
        if (lyceum.getVerificationStatus() != VerificationStatus.VERIFIED) {
            User currentUser = getCurrentUser()
                    .orElseThrow(() -> new UnauthorizedException("You must be authenticated to access this lyceum."));
            if (currentUser.getRole() != Role.ADMIN) {
                throw new AccessDeniedException("You do not have permission to access this lyceum.");
            }
        }
        return mapToResponse(lyceum);
    }

    /**
     * Lists courses linked to a specific lyceum.
     *
     * @param lyceumId lyceum identifier
     * @return courses for the lyceum
     */
    public List<CourseResponse> getLyceumCourses(Long lyceumId) {
        requireLyceum(lyceumId);
        return courseService.getCoursesByLyceumId(lyceumId);
    }

    /**
     * Returns lyceums that match the provided ids.
     *
     * @param ids list of lyceum identifiers
     * @return matching lyceum responses
     */
    public List<LyceumResponse> getLyceumsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("Lyceum ids must be provided.");
        }
        if (ids.stream().anyMatch(id -> id == null)) {
            throw new BadRequestException("Lyceum ids must not contain null values.");
        }
        return lyceumRepository.findAllById(ids)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Lists lecturers assigned to a specific lyceum.
     *
     * @param lyceumId lyceum identifier
     * @return lecturers for the lyceum
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getLyceumLecturers(Long lyceumId) {
        Lyceum lyceum = requireLyceumWithLecturers(lyceumId);
        List<User> lecturers = lyceum.getLecturers();
        if (lecturers == null || lecturers.isEmpty()) {
            return List.of();
        }
        return lecturers.stream()
                .map(this::mapToUserResponse)
                .toList();
    }

    /**
     * Lists administrators assigned to a specific lyceum.
     *
     * @param lyceumId lyceum identifier
     * @return administrators for the lyceum
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getLyceumAdministrators(Long lyceumId) {
        Lyceum lyceum = requireLyceum(lyceumId);
        User currentUser = getManagedCurrentUser();
        ensureUserCanModifyLyceum(currentUser, lyceum);

        List<User> administrators = userRepository.findAllByAdministratedLyceum_Id(lyceumId);
        if (administrators == null || administrators.isEmpty()) {
            return List.of();
        }
        return administrators.stream()
                .map(this::mapToUserResponse)
                .toList();
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

    private void ensureUserCanModifyLyceum(User user, Lyceum lyceum) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        Lyceum administrated = user.getAdministratedLyceum();
        if (administrated == null || !administrated.getId().equals(lyceum.getId())) {
            throw new AccessDeniedException("You do not have permission to modify this lyceum.");
        }
    }

    private void syncAdministratorsCollection(Lyceum lyceum, User user) {
        if (lyceum.getAdministrators() == null) {
            lyceum.setAdministrators(new ArrayList<>());
        }
        boolean alreadyAdministrator = lyceum.getAdministrators().stream()
                .anyMatch(existing -> existing.getId() != null && existing.getId().equals(user.getId()));
        if (!alreadyAdministrator) {
            lyceum.getAdministrators().add(user);
        }
    }

    private Lyceum requireLyceum(Token token) {
        Lyceum lyceum = token.getLyceum();
        if (lyceum == null) {
            throw new BadRequestException("Verification code is not associated with a lyceum.");
        }
        return lyceum;
    }

    private Lyceum requireLyceum(Long lyceumId) {
        if (lyceumId == null) {
            throw new BadRequestException("Lyceum id must be provided.");
        }
        return lyceumRepository.findById(lyceumId)
                .orElseThrow(() -> new NoSuchElementException(LYCEUM_ID_MESSAGE + lyceumId + NOT_FOUND_MESSAGE));
    }

    private Lyceum requireLyceumWithLecturers(Long lyceumId) {
        if (lyceumId == null) {
            throw new BadRequestException("Lyceum id must be provided.");
        }
        return lyceumRepository.findWithLecturersById(lyceumId)
                .orElseThrow(() -> new NoSuchElementException(LYCEUM_ID_MESSAGE + lyceumId + NOT_FOUND_MESSAGE));
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
        syncAdministratorsCollection(lyceum, user);
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

    private void applyOptionalString(String value, Consumer<String> setter) {
        if (value != null) {
            setter.accept(normalize(value));
        }
    }

    private LyceumResponse mapToResponse(Lyceum lyceum) {
        if (lyceum == null) {
            return null;
        }
        return LyceumResponse.builder()
                .id(lyceum.getId())
                .name(lyceum.getName())
                .chitalishtaUrl(lyceum.getChitalishtaUrl())
                .status(lyceum.getStatus())
                .bulstat(lyceum.getBulstat())
                .chairman(lyceum.getChairman())
                .secretary(lyceum.getSecretary())
                .phone(lyceum.getPhone())
                .email(lyceum.getEmail())
                .region(lyceum.getRegion())
                .municipality(lyceum.getMunicipality())
                .town(lyceum.getTown())
                .address(lyceum.getAddress())
                .urlToLibrariesSite(lyceum.getUrlToLibrariesSite())
                .registrationNumber(lyceum.getRegistrationNumber())
                .longitude(lyceum.getLongitude())
                .latitude(lyceum.getLatitude())
                .verificationStatus(lyceum.getVerificationStatus())
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .administratedLyceumId(user.getAdministratedLyceum() != null ? user.getAdministratedLyceum().getId() : null)
                .lecturedCourseIds(extractLecturedCourseIds(user))
                .lecturedLyceumIds(extractLecturedLyceumIds(user))
                .enabled(user.isEnabled())
                .build();
    }

    private List<Long> extractLecturedCourseIds(User user) {
        if (user.getCoursesLectured() == null || user.getCoursesLectured().isEmpty()) {
            return List.of();
        }
        return user.getCoursesLectured().stream()
                .map(Course::getId)
                .toList();
    }

    private List<Long> extractLecturedLyceumIds(User user) {
        if (user.getLecturedLyceums() == null || user.getLecturedLyceums().isEmpty()) {
            return List.of();
        }
        return user.getLecturedLyceums().stream()
                .map(Lyceum::getId)
                .toList();
    }
}
