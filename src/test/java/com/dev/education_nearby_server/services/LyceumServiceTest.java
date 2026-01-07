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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LyceumServiceTest {

    @Mock
    private LyceumRepository lyceumRepository;
    @Mock
    private TokenRepository tokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private CourseService courseService;

    @InjectMocks
    private LyceumService lyceumService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestRightsReturnsMessageWhenLyceumMissing() {
        LyceumRightsRequest request = LyceumRightsRequest.builder()
                .lyceumName("Unknown")
                .town("Nowhere")
                .build();
        when(lyceumRepository.findFirstByNameIgnoreCaseAndTownIgnoreCase("Unknown", "Nowhere"))
                .thenReturn(Optional.empty());

        String result = lyceumService.requestRightsOverLyceum(request);

        assertThat(result).isEqualTo("We are sorry, we could not find such lyceum. Please contact us.");
        verify(emailService, never()).sendLyceumVerificationEmail(any(), any(), any(), any());
    }

    @Test
    void getVerifiedLyceumsReturnsRepositoryResult() {
        Lyceum verifiedLyceum = createLyceum(15L, "Verified Lyceum", "Sofia", "verified@example.com");
        verifiedLyceum.setVerificationStatus(VerificationStatus.VERIFIED);
        when(lyceumRepository.findAllByVerificationStatus(VerificationStatus.VERIFIED))
                .thenReturn(List.of(verifiedLyceum));

        List<LyceumResponse> result = lyceumService.getVerifiedLyceums();

        assertThat(result).hasSize(1);
        LyceumResponse response = result.getFirst();
        assertThat(response.getId()).isEqualTo(15L);
        assertThat(response.getName()).isEqualTo("Verified Lyceum");
        assertThat(response.getTown()).isEqualTo("Sofia");
        assertThat(response.getEmail()).isEqualTo("verified@example.com");
        assertThat(response.getLongitude()).isEqualTo(23.5);
        assertThat(response.getLatitude()).isEqualTo(42.7);
        verify(lyceumRepository).findAllByVerificationStatus(VerificationStatus.VERIFIED);
    }

    @Test
    void getAllLyceumsReturnsRepositoryResult() {
        Lyceum lyceum = createLyceum(3L, "Lyceum", "Varna", "contact@example.com");
        when(lyceumRepository.findAll()).thenReturn(List.of(lyceum));

        List<LyceumResponse> result = lyceumService.getAllLyceums();

        assertThat(result).hasSize(1);
        LyceumResponse response = result.getFirst();
        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getName()).isEqualTo("Lyceum");
        assertThat(response.getTown()).isEqualTo("Varna");
        assertThat(response.getEmail()).isEqualTo("contact@example.com");
        assertThat(response.getLongitude()).isEqualTo(23.5);
        assertThat(response.getLatitude()).isEqualTo(42.7);
        verify(lyceumRepository).findAll();
    }

    @Test
    void getLyceumCoursesReturnsServicePayload() {
        Lyceum lyceum = createLyceum(5L, "Lyceum", "Varna", "contact@example.com");
        when(lyceumRepository.findById(5L)).thenReturn(Optional.of(lyceum));
        CourseResponse course = CourseResponse.builder()
                .id(11L)
                .name("Course")
                .lyceumId(5L)
                .build();
        when(courseService.getCoursesByLyceumId(5L)).thenReturn(List.of(course));

        List<CourseResponse> result = lyceumService.getLyceumCourses(5L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(11L);
        verify(courseService).getCoursesByLyceumId(5L);
    }

    @Test
    void getLyceumCoursesThrowsWhenLyceumMissing() {
        when(lyceumRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> lyceumService.getLyceumCourses(9L));
        verifyNoInteractions(courseService);
    }

    @Test
    void getLyceumCoursesThrowsWhenLyceumIdMissing() {
        assertThrows(BadRequestException.class, () -> lyceumService.getLyceumCourses(null));
        verifyNoInteractions(courseService);
    }

    @Test
    void getLyceumsByIdsReturnsRepositoryResult() {
        Lyceum first = createLyceum(1L, "First", "Sofia", "first@example.com");
        Lyceum second = createLyceum(2L, "Second", "Varna", "second@example.com");
        List<Long> ids = List.of(1L, 2L);
        when(lyceumRepository.findAllById(ids)).thenReturn(List.of(first, second));

        List<LyceumResponse> result = lyceumService.getLyceumsByIds(ids);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
        verify(lyceumRepository).findAllById(ids);
    }

    @Test
    void getLyceumsByIdsReturnsEmptyWhenRepositoryEmpty() {
        List<Long> ids = List.of(3L, 4L);
        when(lyceumRepository.findAllById(ids)).thenReturn(List.of());

        List<LyceumResponse> result = lyceumService.getLyceumsByIds(ids);

        assertThat(result).isEmpty();
        verify(lyceumRepository).findAllById(ids);
    }

    @Test
    void getLyceumsByIdsThrowsWhenIdsMissing() {
        assertThrows(BadRequestException.class, () -> lyceumService.getLyceumsByIds(List.of()));
        verifyNoInteractions(lyceumRepository);
    }

    @Test
    void getLyceumsByIdsThrowsWhenIdsNull() {
        assertThrows(BadRequestException.class, () -> lyceumService.getLyceumsByIds(null));
        verifyNoInteractions(lyceumRepository);
    }

    @Test
    void getLyceumsByIdsThrowsWhenIdsContainNull() {
        assertThrows(BadRequestException.class, () -> lyceumService.getLyceumsByIds(Arrays.asList(1L, null)));
        verifyNoInteractions(lyceumRepository);
    }

    @Test
    void getLyceumLecturersMapsLecturedIds() {
        Lyceum lyceum = createLyceum(8L, "Lyceum", "Varna", "lyceum@example.com");
        Course course = new Course();
        course.setId(12L);
        User lecturer = createUser(20L);
        lecturer.setCoursesLectured(List.of(course));
        lecturer.setLecturedLyceums(List.of(lyceum));
        lyceum.setLecturers(new ArrayList<>(List.of(lecturer)));
        when(lyceumRepository.findWithLecturersById(8L)).thenReturn(Optional.of(lyceum));

        List<UserResponse> response = lyceumService.getLyceumLecturers(8L);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getLecturedCourseIds()).containsExactly(12L);
        assertThat(response.getFirst().getLecturedLyceumIds()).containsExactly(8L);
        verify(lyceumRepository).findWithLecturersById(8L);
    }

    @Test
    void getLyceumLecturersReturnsEmptyWhenListNull() {
        Lyceum lyceum = createLyceum(10L, "Lyceum", "Varna", "lyceum@example.com");
        lyceum.setLecturers(null);
        when(lyceumRepository.findWithLecturersById(10L)).thenReturn(Optional.of(lyceum));

        List<UserResponse> response = lyceumService.getLyceumLecturers(10L);

        assertThat(response).isEmpty();
        verify(lyceumRepository).findWithLecturersById(10L);
    }

    @Test
    void getLyceumLecturersReturnsEmptyWhenListEmpty() {
        Lyceum lyceum = createLyceum(11L, "Lyceum", "Varna", "lyceum@example.com");
        lyceum.setLecturers(new ArrayList<>());
        when(lyceumRepository.findWithLecturersById(11L)).thenReturn(Optional.of(lyceum));

        List<UserResponse> response = lyceumService.getLyceumLecturers(11L);

        assertThat(response).isEmpty();
        verify(lyceumRepository).findWithLecturersById(11L);
    }

    @Test
    void filterLyceumsDelegatesToRepositoryWithPagination() {
        Lyceum lyceum = createLyceum(30L, "Central", "Varna", "central@example.com");
        lyceum.setVerificationStatus(VerificationStatus.VERIFIED);
        when(lyceumRepository.filterLyceums(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(lyceum));

        ArgumentCaptor<String> townCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Double> latCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> lonCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        List<LyceumResponse> result = lyceumService.filterLyceums("Varna", 42.5, 23.3, 2);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("Central");

        verify(lyceumRepository).filterLyceums(
                townCaptor.capture(),
                latCaptor.capture(),
                lonCaptor.capture(),
                statusCaptor.capture(),
                pageableCaptor.capture()
        );
        assertThat(townCaptor.getValue()).isEqualTo("Varna");
        assertThat(latCaptor.getValue()).isEqualTo(42.5);
        assertThat(lonCaptor.getValue()).isEqualTo(23.3);
        assertThat(statusCaptor.getValue()).isEqualTo(VerificationStatus.VERIFIED.name());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(2);
    }

    @Test
    void filterLyceumsTreatsBlankTownAsNullAndUnpaged() {
        when(lyceumRepository.filterLyceums(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        ArgumentCaptor<String> townCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Double> latCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> lonCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        lyceumService.filterLyceums("   ", null, null, null);

        verify(lyceumRepository).filterLyceums(
                townCaptor.capture(),
                latCaptor.capture(),
                lonCaptor.capture(),
                statusCaptor.capture(),
                pageableCaptor.capture()
        );
        assertThat(townCaptor.getValue()).isNull();
        assertThat(latCaptor.getValue()).isNull();
        assertThat(lonCaptor.getValue()).isNull();
        assertThat(statusCaptor.getValue()).isEqualTo(VerificationStatus.VERIFIED.name());
        assertThat(pageableCaptor.getValue().isPaged()).isFalse();
    }

    @Test
    void filterLyceumsThrowsWhenCoordinatesIncomplete() {
        assertThrows(BadRequestException.class, () -> lyceumService.filterLyceums("Varna", 42.5, null, 3));
        verify(lyceumRepository, never()).filterLyceums(any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void filterLyceumsThrowsWhenLimitNonPositive() {
        assertThrows(BadRequestException.class, () -> lyceumService.filterLyceums("Varna", null, null, 0));
        verify(lyceumRepository, never()).filterLyceums(any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void updateLyceumThrowsWhenRequestNull() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> lyceumService.updateLyceum(7L, null));

        assertThat(ex.getMessage()).isEqualTo("Lyceum payload must not be null.");
        verifyNoInteractions(lyceumRepository, userRepository);
    }

    @Test
    void updateLyceumThrowsWhenUnauthenticated() {
        LyceumRequest request = LyceumRequest.builder()
                .name("Updated")
                .town("Varna")
                .build();
        Lyceum lyceum = createLyceum(7L, "Lyceum", "Varna", null);
        when(lyceumRepository.findById(7L)).thenReturn(Optional.of(lyceum));

        assertThrows(UnauthorizedException.class, () -> lyceumService.updateLyceum(7L, request));
        verify(lyceumRepository, never()).save(any());
    }

    @Test
    void updateLyceumAllowsAdminUser() {
        LyceumRequest request = LyceumRequest.builder()
                .name("Updated")
                .town("Varna")
                .build();
        Lyceum existing = createLyceum(7L, "Lyceum", "Varna", null);

        User admin = createUser(10L);
        admin.setRole(Role.ADMIN);
        mockAuthenticatedUser(admin);

        when(lyceumRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(lyceumRepository.findFirstByNameIgnoreCaseAndTownIgnoreCase("Updated", "Varna"))
                .thenReturn(Optional.empty());
        when(lyceumRepository.save(existing)).thenReturn(existing);

        LyceumResponse response = lyceumService.updateLyceum(7L, request);

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(existing.getName()).isEqualTo("Updated");
        verify(lyceumRepository).save(existing);
    }

    @Test
    void updateLyceumAllowsLyceumAdministrator() {
        LyceumRequest request = LyceumRequest.builder()
                .name("Updated")
                .town("Varna")
                .build();
        Lyceum existing = createLyceum(7L, "Lyceum", "Varna", null);

        User user = createUser(11L);
        user.setAdministratedLyceum(existing);
        mockAuthenticatedUser(user);

        when(lyceumRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(lyceumRepository.findFirstByNameIgnoreCaseAndTownIgnoreCase("Updated", "Varna"))
                .thenReturn(Optional.empty());
        when(lyceumRepository.save(existing)).thenReturn(existing);

        LyceumResponse response = lyceumService.updateLyceum(7L, request);

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(existing.getName()).isEqualTo("Updated");
        verify(lyceumRepository).save(existing);
    }

    @Test
    void updateLyceumUpdatesOptionalFieldsWhenProvided() {
        LyceumRequest request = LyceumRequest.builder()
                .name("Updated")
                .town("Varna")
                .email("  updated@example.com  ")
                .address("  Some   address  ")
                .chairman("  Chair   Man ")
                .bulstat("  987654321 ")
                .registrationNumber(77)
                .longitude(24.1)
                .latitude(42.9)
                .build();
        Lyceum existing = createLyceum(7L, "Lyceum", "Varna", "contact@example.com");

        User admin = createUser(13L);
        admin.setRole(Role.ADMIN);
        mockAuthenticatedUser(admin);

        when(lyceumRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(lyceumRepository.findFirstByNameIgnoreCaseAndTownIgnoreCase("Updated", "Varna"))
                .thenReturn(Optional.empty());
        when(lyceumRepository.save(existing)).thenReturn(existing);

        lyceumService.updateLyceum(7L, request);

        assertThat(existing.getEmail()).isEqualTo("updated@example.com");
        assertThat(existing.getAddress()).isEqualTo("Some address");
        assertThat(existing.getChairman()).isEqualTo("Chair Man");
        assertThat(existing.getBulstat()).isEqualTo("987654321");
        assertThat(existing.getRegistrationNumber()).isEqualTo(77);
        assertThat(existing.getLongitude()).isEqualTo(24.1);
        assertThat(existing.getLatitude()).isEqualTo(42.9);
    }

    @Test
    void updateLyceumPreservesOptionalFieldsWhenNotProvided() {
        LyceumRequest request = LyceumRequest.builder()
                .name("Updated")
                .town("Varna")
                .build();
        Lyceum existing = createLyceum(7L, "Lyceum", "Varna", "contact@example.com");

        User admin = createUser(13L);
        admin.setRole(Role.ADMIN);
        mockAuthenticatedUser(admin);

        when(lyceumRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(lyceumRepository.findFirstByNameIgnoreCaseAndTownIgnoreCase("Updated", "Varna"))
                .thenReturn(Optional.empty());
        when(lyceumRepository.save(existing)).thenReturn(existing);

        lyceumService.updateLyceum(7L, request);

        assertThat(existing.getBulstat()).isEqualTo("123456789");
        assertThat(existing.getEmail()).isEqualTo("contact@example.com");
        assertThat(existing.getLongitude()).isEqualTo(23.5);
        assertThat(existing.getLatitude()).isEqualTo(42.7);
    }

    @Test
    void updateLyceumThrowsWhenNameBlank() {
        LyceumRequest request = LyceumRequest.builder()
                .name("   ")
                .town("Varna")
                .build();
        Lyceum existing = createLyceum(7L, "Lyceum", "Varna", null);

        User admin = createUser(10L);
        admin.setRole(Role.ADMIN);
        mockAuthenticatedUser(admin);

        when(lyceumRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> lyceumService.updateLyceum(7L, request));

        assertThat(ex.getMessage()).isEqualTo("Lyceum name must not be blank.");
        verify(lyceumRepository, never()).findFirstByNameIgnoreCaseAndTownIgnoreCase(any(), any());
        verify(lyceumRepository, never()).save(any());
    }

    @Test
    void updateLyceumThrowsWhenDuplicateNameAndTownExists() {
        LyceumRequest request = LyceumRequest.builder()
                .name("Updated")
                .town("Varna")
                .build();
        Lyceum existing = createLyceum(7L, "Lyceum", "Varna", null);
        Lyceum duplicate = createLyceum(9L, "Updated", "Varna", null);

        User admin = createUser(14L);
        admin.setRole(Role.ADMIN);
        mockAuthenticatedUser(admin);

        when(lyceumRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(lyceumRepository.findFirstByNameIgnoreCaseAndTownIgnoreCase("Updated", "Varna"))
                .thenReturn(Optional.of(duplicate));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> lyceumService.updateLyceum(7L, request));

        assertThat(ex.getMessage()).isEqualTo("Lyceum with the same name and town already exists.");
        verify(lyceumRepository, never()).save(any());
    }

    @Test
    void updateLyceumThrowsWhenUserNotAdministrator() {
        LyceumRequest request = LyceumRequest.builder()
                .name("Updated")
                .town("Varna")
                .build();
        Lyceum existing = createLyceum(7L, "Lyceum", "Varna", null);

        User user = createUser(12L);
        mockAuthenticatedUser(user);

        when(lyceumRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThrows(AccessDeniedException.class, () -> lyceumService.updateLyceum(7L, request));
        verify(lyceumRepository, never()).save(any());
    }

    @Test
    void assignAdministratorThrowsWhenCurrentUserNotAuthorized() {
        Lyceum lyceum = createLyceum(7L, "Lyceum", "Varna", null);
        User current = createUser(1L);
        mockAuthenticatedUser(current);

        when(lyceumRepository.findById(7L)).thenReturn(Optional.of(lyceum));
        when(userRepository.findById(current.getId())).thenReturn(Optional.of(current));

        assertThrows(AccessDeniedException.class, () -> lyceumService.assignAdministrator(7L, 5L));
        verify(userRepository, never()).save(any());
    }

    @Test
    void assignAdministratorThrowsWhenTargetUserMissing() {
        Lyceum lyceum = createLyceum(7L, "Lyceum", "Varna", null);
        User admin = createUser(2L);
        admin.setRole(Role.ADMIN);
        mockAuthenticatedUser(admin);

        when(lyceumRepository.findById(7L)).thenReturn(Optional.of(lyceum));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> lyceumService.assignAdministrator(7L, 5L));
        verify(userRepository, never()).save(any());
    }

    @Test
    void assignAdministratorThrowsWhenTargetAdministratesAnotherLyceum() {
        Lyceum lyceum = createLyceum(7L, "Lyceum", "Varna", null);
        Lyceum otherLyceum = createLyceum(9L, "Other", "Sofia", null);
        User admin = createUser(2L);
        admin.setRole(Role.ADMIN);
        mockAuthenticatedUser(admin);

        User target = createUser(5L);
        target.setAdministratedLyceum(otherLyceum);

        when(lyceumRepository.findById(7L)).thenReturn(Optional.of(lyceum));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        assertThrows(ConflictException.class, () -> lyceumService.assignAdministrator(7L, target.getId()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void assignAdministratorAllowsAdminUser() {
        Lyceum lyceum = createLyceum(7L, "Lyceum", "Varna", null);
        User admin = createUser(2L);
        admin.setRole(Role.ADMIN);
        mockAuthenticatedUser(admin);

        User target = createUser(5L);

        when(lyceumRepository.findById(7L)).thenReturn(Optional.of(lyceum));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        lyceumService.assignAdministrator(7L, target.getId());

        assertThat(target.getAdministratedLyceum()).isEqualTo(lyceum);
        assertThat(lyceum.getAdministrators())
                .extracting(User::getId)
                .contains(target.getId());
        verify(userRepository).save(target);
        verify(lyceumRepository).save(lyceum);
    }

    @Test
    void createLyceumThrowsWhenRequestNull() {
        assertThrows(BadRequestException.class, () -> lyceumService.createLyceum(null));
        verify(lyceumRepository, never()).save(any(Lyceum.class));
    }

    @Test
    void createLyceumThrowsWhenNameBlank() {
        LyceumRequest request = LyceumRequest.builder()
                .name("  ")
                .town("Varna")
                .build();

        assertThrows(BadRequestException.class, () -> lyceumService.createLyceum(request));
        verify(lyceumRepository, never()).save(any(Lyceum.class));
    }

    @Test
    void createLyceumThrowsWhenTownBlank() {
        LyceumRequest request = LyceumRequest.builder()
                .name("Lyceum")
                .town(" ")
                .build();

        assertThrows(BadRequestException.class, () -> lyceumService.createLyceum(request));
        verify(lyceumRepository, never()).save(any(Lyceum.class));
    }

    @Test
    void createLyceumThrowsWhenDuplicateExists() {
        LyceumRequest request = LyceumRequest.builder()
                .name("Lyceum")
                .town("Varna")
                .build();
        when(lyceumRepository.findFirstByNameIgnoreCaseAndTownIgnoreCase("Lyceum", "Varna"))
                .thenReturn(Optional.of(new Lyceum()));

        assertThrows(ConflictException.class, () -> lyceumService.createLyceum(request));
        verify(lyceumRepository, never()).save(any(Lyceum.class));
    }

    @Test
    void createLyceumNormalizesAndSavesEntity() {
        LyceumRequest request = LyceumRequest.builder()
                .name("  New Lyceum ")
                .town("  Varna ")
                .chitalishtaUrl("  https://example.org  ")
                .status("  Active ")
                .bulstat(" 12345 ")
                .chairman(" John  Doe ")
                .secretary(" Jane  Doe ")
                .phone("  123 456 ")
                .email(" admin@example.org ")
                .region("  Region ")
                .municipality("  Municipality ")
                .address("  Address 1 ")
                .urlToLibrariesSite(" https://library.example.org ")
                .registrationNumber(42)
                .longitude(23.456)
                .latitude(43.21)
                .build();
        when(lyceumRepository.findFirstByNameIgnoreCaseAndTownIgnoreCase("New Lyceum", "Varna"))
                .thenReturn(Optional.empty());
        ArgumentCaptor<Lyceum> lyceumCaptor = ArgumentCaptor.forClass(Lyceum.class);
        Lyceum saved = createLyceum(20L, "New Lyceum", "Varna", "admin@example.org");
        saved.setLongitude(23.456);
        saved.setLatitude(43.21);
        when(lyceumRepository.save(any(Lyceum.class))).thenReturn(saved);

        LyceumResponse response = lyceumService.createLyceum(request);

        verify(lyceumRepository).save(lyceumCaptor.capture());
        Lyceum persisted = lyceumCaptor.getValue();
        assertThat(persisted.getId()).isNull();
        assertThat(persisted.getName()).isEqualTo("New Lyceum");
        assertThat(persisted.getTown()).isEqualTo("Varna");
        assertThat(persisted.getChitalishtaUrl()).isEqualTo("https://example.org");
        assertThat(persisted.getStatus()).isEqualTo("Active");
        assertThat(persisted.getBulstat()).isEqualTo("12345");
        assertThat(persisted.getChairman()).isEqualTo("John Doe");
        assertThat(persisted.getSecretary()).isEqualTo("Jane Doe");
        assertThat(persisted.getPhone()).isEqualTo("123 456");
        assertThat(persisted.getEmail()).isEqualTo("admin@example.org");
        assertThat(persisted.getRegion()).isEqualTo("Region");
        assertThat(persisted.getMunicipality()).isEqualTo("Municipality");
        assertThat(persisted.getAddress()).isEqualTo("Address 1");
        assertThat(persisted.getUrlToLibrariesSite()).isEqualTo("https://library.example.org");
        assertThat(persisted.getRegistrationNumber()).isEqualTo(42);
        assertThat(persisted.getLongitude()).isEqualTo(23.456);
        assertThat(persisted.getLatitude()).isEqualTo(43.21);
        assertThat(persisted.getVerificationStatus()).isEqualTo(VerificationStatus.NOT_VERIFIED);

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getName()).isEqualTo("New Lyceum");
        assertThat(response.getTown()).isEqualTo("Varna");
        assertThat(response.getEmail()).isEqualTo("admin@example.org");
        assertThat(response.getLongitude()).isEqualTo(23.456);
        assertThat(response.getLatitude()).isEqualTo(43.21);
        assertThat(response.getVerificationStatus()).isEqualTo(VerificationStatus.NOT_VERIFIED);
    }

    @Test
    void getLyceumByIdReturnsResponseWhenVerified() {
        Lyceum lyceum = createLyceum(5L, "Lyceum", "Varna", "admin@example.com");
        lyceum.setVerificationStatus(VerificationStatus.VERIFIED);
        when(lyceumRepository.findById(5L)).thenReturn(Optional.of(lyceum));

        LyceumResponse response = lyceumService.getLyceumById(5L);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        verify(lyceumRepository).findById(5L);
    }

    @Test
    void getLyceumByIdThrowsUnauthorizedWhenNotVerifiedAndAnonymous() {
        Lyceum lyceum = createLyceum(6L, "Lyceum", "Varna", "mail@example.com");
        lyceum.setVerificationStatus(VerificationStatus.NOT_VERIFIED);
        when(lyceumRepository.findById(6L)).thenReturn(Optional.of(lyceum));
        SecurityContextHolder.clearContext();

        assertThrows(UnauthorizedException.class, () -> lyceumService.getLyceumById(6L));
    }

    @Test
    void getLyceumByIdThrowsAccessDeniedWhenNotVerifiedAndUserNotAdmin() {
        Lyceum lyceum = createLyceum(7L, "Lyceum", "Varna", "mail@example.com");
        lyceum.setVerificationStatus(VerificationStatus.NOT_VERIFIED);
        when(lyceumRepository.findById(7L)).thenReturn(Optional.of(lyceum));
        User user = createUser(100L);
        mockAuthenticatedUser(user);

        assertThrows(AccessDeniedException.class, () -> lyceumService.getLyceumById(7L));
    }

    @Test
    void getLyceumByIdReturnsResponseWhenAdminRequestsNonVerifiedLyceum() {
        Lyceum lyceum = createLyceum(8L, "Lyceum", "Varna", "mail@example.com");
        lyceum.setVerificationStatus(VerificationStatus.NOT_VERIFIED);
        when(lyceumRepository.findById(8L)).thenReturn(Optional.of(lyceum));
        User admin = createUser(200L);
        admin.setRole(Role.ADMIN);
        mockAuthenticatedUser(admin);

        LyceumResponse response = lyceumService.getLyceumById(8L);

        assertThat(response.getId()).isEqualTo(8L);
        assertThat(response.getVerificationStatus()).isEqualTo(VerificationStatus.NOT_VERIFIED);
        verify(lyceumRepository).findById(8L);
    }

    @Test
    void requestRightsReturnsMessageWhenLyceumEmailMissing() {
        Lyceum lyceum = createLyceum(10L, "Test Lyceum", "Sofia", null);
        LyceumRightsRequest request = LyceumRightsRequest.builder()
                .lyceumName("Test Lyceum")
                .town("Sofia")
                .build();
        when(lyceumRepository.findFirstByNameIgnoreCaseAndTownIgnoreCase("Test Lyceum", "Sofia"))
                .thenReturn(Optional.of(lyceum));

        String result = lyceumService.requestRightsOverLyceum(request);

        assertThat(result).isEqualTo("We could not reach the lyceum via email. Please contact us.");
        verify(emailService, never()).sendLyceumVerificationEmail(any(), any(), any(), any());
    }

    @Test
    void requestRightsReturnsMessageWhenUserAlreadyAdminOfSameLyceum() {
        Lyceum lyceum = createLyceum(5L, "Lyceum", "Varna", "school@example.com");
        User user = createUser(1L);
        user.setAdministratedLyceum(lyceum);

        LyceumRightsRequest request = LyceumRightsRequest.builder()
                .lyceumName("Lyceum")
                .town("Varna")
                .build();

        when(lyceumRepository.findFirstByNameIgnoreCaseAndTownIgnoreCase("Lyceum", "Varna"))
                .thenReturn(Optional.of(lyceum));
        mockAuthenticatedUser(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        String result = lyceumService.requestRightsOverLyceum(request);

        assertThat(result).isEqualTo("You already administrate this lyceum.");
        verify(tokenRepository, never()).save(any(Token.class));
        verify(emailService, never()).sendLyceumVerificationEmail(any(), any(), any(), any());
    }

    @Test
    void requestRightsThrowsWhenUserAlreadyAdminOfDifferentLyceum() {
        Lyceum target = createLyceum(5L, "Lyceum", "Varna", "school@example.com");
        Lyceum other = createLyceum(11L, "Other", "Varna", "other@example.com");
        User user = createUser(1L);
        user.setAdministratedLyceum(other);

        LyceumRightsRequest request = LyceumRightsRequest.builder()
                .lyceumName("Lyceum")
                .town("Varna")
                .build();

        when(lyceumRepository.findFirstByNameIgnoreCaseAndTownIgnoreCase("Lyceum", "Varna"))
                .thenReturn(Optional.of(target));
        mockAuthenticatedUser(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThrows(ConflictException.class, () -> lyceumService.requestRightsOverLyceum(request));
        verify(emailService, never()).sendLyceumVerificationEmail(any(), any(), any(), any());
    }

    @Test
    void requestRightsThrowsWhenUserNotAuthenticated() {
        Lyceum lyceum = createLyceum(7L, "Lyceum", "Varna", "school@example.com");

        LyceumRightsRequest request = LyceumRightsRequest.builder()
                .lyceumName("Lyceum")
                .town("Varna")
                .build();

        when(lyceumRepository.findFirstByNameIgnoreCaseAndTownIgnoreCase("Lyceum", "Varna"))
                .thenReturn(Optional.of(lyceum));

        assertThrows(UnauthorizedException.class, () -> lyceumService.requestRightsOverLyceum(request));
        verify(tokenRepository, never()).save(any(Token.class));
        verify(emailService, never()).sendLyceumVerificationEmail(any(), any(), any(), any());
    }

    @Test
    void requestRightsNormalizesInputBeforeLookupAndNotification() {
        Lyceum lyceum = createLyceum(8L, "Lyceum", "Varna", "school@example.com");
        User user = createUser(10L);

        LyceumRightsRequest request = LyceumRightsRequest.builder()
                .lyceumName("   Lyceum   ")
                .town("  Varna  ")
                .build();

        when(lyceumRepository.findFirstByNameIgnoreCaseAndTownIgnoreCase("Lyceum", "Varna"))
                .thenReturn(Optional.of(lyceum));
        when(tokenRepository.findAllValidTokenByUser(user.getId())).thenReturn(List.of());
        mockAuthenticatedUser(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        lyceumService.requestRightsOverLyceum(request);

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> townCaptor = ArgumentCaptor.forClass(String.class);
        verify(lyceumRepository).findFirstByNameIgnoreCaseAndTownIgnoreCase(nameCaptor.capture(), townCaptor.capture());
        assertThat(nameCaptor.getValue()).isEqualTo("Lyceum");
        assertThat(townCaptor.getValue()).isEqualTo("Varna");

        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> normalizedNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> normalizedTownCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendLyceumVerificationEmail(
                toCaptor.capture(),
                normalizedNameCaptor.capture(),
                normalizedTownCaptor.capture(),
                tokenCaptor.capture());

        assertThat(toCaptor.getValue()).isEqualTo("school@example.com");
        assertThat(normalizedNameCaptor.getValue()).isEqualTo("Lyceum");
        assertThat(normalizedTownCaptor.getValue()).isEqualTo("Varna");
        assertThat(tokenCaptor.getValue()).isNotBlank();
    }

    @Test
    void requestRightsCreatesTokenInvalidatesPreviousAndSendsEmail() {
        Lyceum lyceum = createLyceum(5L, "Lyceum", "Varna", "school@example.com");
        User user = createUser(1L);
        User managedUser = createUser(1L);
        Token previousToken = Token.builder()
                .tokenType(TokenType.VERIFICATION)
                .expired(false)
                .revoked(false)
                .user(managedUser)
                .build();

        LyceumRightsRequest request = LyceumRightsRequest.builder()
                .lyceumName("Lyceum")
                .town("Varna")
                .build();

        when(lyceumRepository.findFirstByNameIgnoreCaseAndTownIgnoreCase("Lyceum", "Varna"))
                .thenReturn(Optional.of(lyceum));
        when(tokenRepository.findAllValidTokenByUser(managedUser.getId()))
                .thenReturn(List.of(previousToken));
        mockAuthenticatedUser(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(managedUser));

        ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);

        String result = lyceumService.requestRightsOverLyceum(request);

        assertThat(result).isEqualTo("We have sent you an email at school@example.com with a verification code.");
        assertThat(previousToken.isExpired()).isTrue();
        assertThat(previousToken.isRevoked()).isTrue();

        verify(tokenRepository).saveAll(List.of(previousToken));
        verify(tokenRepository).save(tokenCaptor.capture());
        Token savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser()).isEqualTo(managedUser);
        assertThat(savedToken.getLyceum()).isEqualTo(lyceum);
        assertThat(savedToken.getTokenType()).isEqualTo(TokenType.VERIFICATION);
        assertThat(savedToken.isExpired()).isFalse();
        assertThat(savedToken.isRevoked()).isFalse();

        verify(emailService).sendLyceumVerificationEmail(
                "school@example.com", "Lyceum", "Varna", savedToken.getTokenValue());
    }

    @Test
    void verifyRightsThrowsWhenCodeMissing() {
        LyceumRightsVerificationRequest request = LyceumRightsVerificationRequest.builder()
                .verificationCode(" ")
                .build();

        assertThrows(BadRequestException.class, () -> lyceumService.verifyRightsOverLyceum(request));
    }

    @Test
    void verifyRightsThrowsWhenTokenMissing() {
        User user = createUser(1L);
        mockAuthenticatedUser(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(tokenRepository.findByToken("code")).thenReturn(Optional.empty());

        LyceumRightsVerificationRequest request = LyceumRightsVerificationRequest.builder()
                .verificationCode("code")
                .build();

        assertThrows(BadRequestException.class, () -> lyceumService.verifyRightsOverLyceum(request));
    }

    @Test
    void verifyRightsTrimsVerificationCodeBeforeLookup() {
        User user = createUser(1L);
        User managedUser = createUser(1L);
        Lyceum lyceum = createLyceum(3L, "Lyceum", "Varna", "mail@example.com");
        Token token = Token.builder()
                .tokenValue("trimmed-code")
                .tokenType(TokenType.VERIFICATION)
                .expired(false)
                .revoked(false)
                .user(managedUser)
                .lyceum(lyceum)
                .build();

        mockAuthenticatedUser(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(managedUser));
        when(tokenRepository.findByToken("trimmed-code")).thenReturn(Optional.of(token));

        LyceumRightsVerificationRequest request = LyceumRightsVerificationRequest.builder()
                .verificationCode("  trimmed-code  ")
                .build();

        String message = lyceumService.verifyRightsOverLyceum(request);

        assertThat(message).isEqualTo("You are now the administrator of Lyceum in Varna.");
    }

    @Test
    void verifyRightsThrowsWhenTokenHasDifferentType() {
        User user = createUser(1L);
        User managedUser = createUser(1L);
        Lyceum lyceum = createLyceum(3L, "Lyceum", "Varna", "mail@example.com");
        Token token = Token.builder()
                .tokenValue("code")
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .user(managedUser)
                .lyceum(lyceum)
                .build();

        mockAuthenticatedUser(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(managedUser));
        when(tokenRepository.findByToken("code")).thenReturn(Optional.of(token));

        LyceumRightsVerificationRequest request = LyceumRightsVerificationRequest.builder()
                .verificationCode("code")
                .build();

        assertThrows(BadRequestException.class, () -> lyceumService.verifyRightsOverLyceum(request));
    }

    @Test
    void verifyRightsThrowsWhenTokenExpiredOrRevoked() {
        User user = createUser(1L);
        User managedUser = createUser(1L);
        Lyceum lyceum = createLyceum(3L, "Lyceum", "Varna", "mail@example.com");
        Token token = Token.builder()
                .tokenValue("code")
                .tokenType(TokenType.VERIFICATION)
                .expired(true)
                .revoked(true)
                .user(managedUser)
                .lyceum(lyceum)
                .build();

        mockAuthenticatedUser(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(managedUser));
        when(tokenRepository.findByToken("code")).thenReturn(Optional.of(token));

        LyceumRightsVerificationRequest request = LyceumRightsVerificationRequest.builder()
                .verificationCode("code")
                .build();

        assertThrows(BadRequestException.class, () -> lyceumService.verifyRightsOverLyceum(request));
    }

    @Test
    void verifyRightsThrowsWhenTokenBelongsToDifferentUser() {
        User user = createUser(1L);
        User otherUser = createUser(2L);
        Lyceum lyceum = createLyceum(3L, "Lyceum", "Varna", "mail@example.com");
        Token token = Token.builder()
                .tokenValue("code")
                .tokenType(TokenType.VERIFICATION)
                .expired(false)
                .revoked(false)
                .user(otherUser)
                .lyceum(lyceum)
                .build();

        mockAuthenticatedUser(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(tokenRepository.findByToken("code")).thenReturn(Optional.of(token));

        LyceumRightsVerificationRequest request = LyceumRightsVerificationRequest.builder()
                .verificationCode("code")
                .build();

        assertThrows(UnauthorizedException.class, () -> lyceumService.verifyRightsOverLyceum(request));
    }

    @Test
    void verifyRightsThrowsWhenUserAdministratesDifferentLyceum() {
        User user = createUser(1L);
        Lyceum current = createLyceum(100L, "Current", "Varna", "curr@example.com");
        user.setAdministratedLyceum(current);

        Lyceum target = createLyceum(3L, "Lyceum", "Varna", "mail@example.com");
        Token token = Token.builder()
                .tokenValue("code")
                .tokenType(TokenType.VERIFICATION)
                .expired(false)
                .revoked(false)
                .user(user)
                .lyceum(target)
                .build();

        mockAuthenticatedUser(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(tokenRepository.findByToken("code")).thenReturn(Optional.of(token));

        LyceumRightsVerificationRequest request = LyceumRightsVerificationRequest.builder()
                .verificationCode("code")
                .build();

        assertThrows(ConflictException.class, () -> lyceumService.verifyRightsOverLyceum(request));
    }

    @Test
    void verifyRightsAssignsLyceumAndExpiresToken() {
        User user = createUser(1L);
        User managedUser = createUser(1L);
        Lyceum lyceum = createLyceum(3L, "Lyceum", "Varna", "mail@example.com");
        Token token = Token.builder()
                .tokenValue("code")
                .tokenType(TokenType.VERIFICATION)
                .expired(false)
                .revoked(false)
                .user(managedUser)
                .lyceum(lyceum)
                .build();

        mockAuthenticatedUser(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(managedUser));
        when(tokenRepository.findByToken("code")).thenReturn(Optional.of(token));

        LyceumRightsVerificationRequest request = LyceumRightsVerificationRequest.builder()
                .verificationCode("code")
                .build();

        String message = lyceumService.verifyRightsOverLyceum(request);

        assertThat(message).isEqualTo("You are now the administrator of Lyceum in Varna.");
        assertThat(managedUser.getAdministratedLyceum()).isEqualTo(lyceum);
        assertThat(lyceum.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(token.isExpired()).isTrue();
        assertThat(token.isRevoked()).isTrue();

        verify(lyceumRepository).save(lyceum);
        verify(userRepository).save(managedUser);

        ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        Token savedToken = tokenCaptor.getValue();
        assertEquals(token, savedToken);
    }

    @Test
    void deleteLyceumRemovesTokensAndUnlinksAdministrators() {
        Lyceum lyceum = createLyceum(5L, "Lyceum", "Varna", "mail@example.com");
        User admin = createUser(10L);
        admin.setAdministratedLyceum(lyceum);

        when(lyceumRepository.findById(5L)).thenReturn(Optional.of(lyceum));
        when(userRepository.findAllByAdministratedLyceum_Id(5L)).thenReturn(List.of(admin));

        lyceumService.deleteLyceum(5L);

        ArgumentCaptor<List<User>> usersCaptor = ArgumentCaptor.forClass(List.class);
        verify(userRepository).saveAll(usersCaptor.capture());
        List<User> savedUsers = usersCaptor.getValue();
        assertThat(savedUsers).containsExactly(admin);
        assertThat(savedUsers.getFirst().getAdministratedLyceum()).isNull();

        verify(tokenRepository).deleteAllByLyceum_Id(5L);
        verify(lyceumRepository).delete(lyceum);
    }

    @Test
    void removeAdministratorRequiresAdminRole() {
        User user = createUser(1L);

        mockAuthenticatedUser(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> lyceumService.removeAdministratorFromLyceum(3L, 11L));

        assertThat(ex.getMessage()).isEqualTo("You do not have permission to modify this lyceum.");
        verify(userRepository).findById(user.getId());
        verify(userRepository, never()).findById(11L);
        verifyNoInteractions(lyceumRepository);
    }

    @Test
    void removeAdministratorThrowsWhenUserIdNull() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> lyceumService.removeAdministratorFromLyceum(3L, null));

        assertThat(ex.getMessage()).isEqualTo("User id must be provided.");
        verifyNoInteractions(userRepository, lyceumRepository);
    }

    @Test
    void removeAdministratorThrowsWhenLyceumIdNull() {
        User admin = createUser(1L);
        admin.setRole(Role.ADMIN);

        mockAuthenticatedUser(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> lyceumService.removeAdministratorFromLyceum(null, 5L));

        assertThat(ex.getMessage()).isEqualTo("Lyceum id must be provided.");
        verify(userRepository).findById(admin.getId());
        verify(userRepository, never()).findById(5L);
        verifyNoInteractions(lyceumRepository);
    }

    @Test
    void removeAdministratorThrowsWhenLyceumMissing() {
        User admin = createUser(1L);
        admin.setRole(Role.ADMIN);

        mockAuthenticatedUser(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(lyceumRepository.findById(33L)).thenReturn(Optional.empty());

        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> lyceumService.removeAdministratorFromLyceum(33L, 5L));

        assertThat(ex.getMessage()).isEqualTo("Lyceum with id 33 not found.");
        verify(userRepository).findById(admin.getId());
        verify(lyceumRepository).findById(33L);
        verify(userRepository, never()).findById(5L);
    }

    @Test
    void removeAdministratorThrowsWhenUserMissing() {
        User admin = createUser(1L);
        admin.setRole(Role.ADMIN);
        Lyceum lyceum = createLyceum(3L, "Lyceum", "Varna", "mail@example.com");

        mockAuthenticatedUser(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(lyceumRepository.findById(3L)).thenReturn(Optional.of(lyceum));
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> lyceumService.removeAdministratorFromLyceum(3L, 9L));

        assertThat(ex.getMessage()).isEqualTo("User with id 9 not found.");
        verify(userRepository).findById(admin.getId());
        verify(lyceumRepository).findById(3L);
        verify(userRepository).findById(9L);
    }

    @Test
    void removeAdministratorUnlinksUserFromLyceum() {
        User admin = createUser(1L);
        admin.setRole(Role.ADMIN);
        User target = createUser(5L);
        Lyceum lyceum = createLyceum(3L, "Lyceum", "Varna", "mail@example.com");
        target.setAdministratedLyceum(lyceum);
        lyceum.setAdministrators(new ArrayList<>(List.of(target)));

        mockAuthenticatedUser(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        when(lyceumRepository.findById(3L)).thenReturn(Optional.of(lyceum));

        lyceumService.removeAdministratorFromLyceum(3L, target.getId());

        assertThat(target.getAdministratedLyceum()).isNull();
        assertThat(lyceum.getAdministrators()).doesNotContain(target);
        verify(userRepository).save(target);
        verify(lyceumRepository).save(lyceum);
    }

    @Test
    void removeAdministratorThrowsWhenUserHasNoLyceum() {
        User admin = createUser(1L);
        admin.setRole(Role.ADMIN);
        User target = createUser(5L);

        mockAuthenticatedUser(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));

        when(lyceumRepository.findById(3L))
                .thenReturn(Optional.of(createLyceum(3L, "Lyceum", "Varna", "mail@example.com")));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> lyceumService.removeAdministratorFromLyceum(3L, target.getId()));

        assertThat(ex.getMessage()).isEqualTo("User is not an administrator of this lyceum.");
        verify(userRepository, never()).save(any(User.class));
        verify(lyceumRepository, never()).save(any(Lyceum.class));
    }

    @Test
    void removeAdministratorThrowsWhenUserAdministratesDifferentLyceum() {
        User admin = createUser(1L);
        admin.setRole(Role.ADMIN);
        User target = createUser(5L);
        Lyceum lyceum = createLyceum(3L, "Lyceum", "Varna", "mail@example.com");
        Lyceum otherLyceum = createLyceum(9L, "Other", "Sofia", "mail@example.com");
        target.setAdministratedLyceum(otherLyceum);

        mockAuthenticatedUser(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(lyceumRepository.findById(3L)).thenReturn(Optional.of(lyceum));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> lyceumService.removeAdministratorFromLyceum(3L, target.getId()));

        assertThat(ex.getMessage()).isEqualTo("User is not an administrator of this lyceum.");
        verify(userRepository, never()).save(any(User.class));
        verify(lyceumRepository, never()).save(any(Lyceum.class));
    }

    @Test
    void removeAdministratorClearsUserWhenAdministratorsListNull() {
        User admin = createUser(1L);
        admin.setRole(Role.ADMIN);
        User target = createUser(5L);
        Lyceum lyceum = createLyceum(3L, "Lyceum", "Varna", "mail@example.com");
        target.setAdministratedLyceum(lyceum);
        lyceum.setAdministrators(null);

        mockAuthenticatedUser(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(lyceumRepository.findById(3L)).thenReturn(Optional.of(lyceum));

        lyceumService.removeAdministratorFromLyceum(3L, target.getId());

        assertThat(target.getAdministratedLyceum()).isNull();
        assertThat(lyceum.getAdministrators()).isNull();
        verify(userRepository).save(target);
        verify(lyceumRepository).save(lyceum);
    }

    @Test
    void addLecturerToLyceumAsAdminRequiresLyceumId() {
        User admin = createUser(1L);
        admin.setRole(Role.ADMIN);

        mockAuthenticatedUser(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> lyceumService.addLecturerToLyceum(LyceumLecturerRequest.builder()
                        .userId(5L)
                        .build()));

        assertThat(ex.getMessage()).isEqualTo("Lyceum id must be provided when assigning as admin.");
        verify(userRepository).findById(admin.getId());
        verifyNoInteractions(lyceumRepository);
    }

    @Test
    void addLecturerToLyceumThrowsWhenUserIdMissing() {
        User admin = createUser(1L);
        admin.setRole(Role.ADMIN);

        mockAuthenticatedUser(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> lyceumService.addLecturerToLyceum(LyceumLecturerRequest.builder()
                        .lyceumId(3L)
                        .build()));

        assertThat(ex.getMessage()).isEqualTo("User id must be provided.");
        verify(userRepository).findById(admin.getId());
        verifyNoInteractions(lyceumRepository);
    }

    @Test
    void addLecturerToLyceumThrowsWhenUserHasNoAdministratedLyceum() {
        User user = createUser(2L);

        mockAuthenticatedUser(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> lyceumService.addLecturerToLyceum(LyceumLecturerRequest.builder()
                        .userId(5L)
                        .build()));

        assertThat(ex.getMessage()).isEqualTo("You do not have permission to modify this lyceum.");
        verify(userRepository, never()).findById(5L);
        verifyNoInteractions(lyceumRepository);
    }

    @Test
    void addLecturerToLyceumAsAdminLinksBothSides() {
        User admin = createUser(1L);
        admin.setRole(Role.ADMIN);
        User lecturer = createUser(5L);
        Lyceum lyceum = createLyceum(3L, "Lyceum", "Varna", "mail@example.com");

        mockAuthenticatedUser(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(lyceumRepository.findById(3L)).thenReturn(Optional.of(lyceum));
        when(userRepository.findById(lecturer.getId())).thenReturn(Optional.of(lecturer));

        lyceumService.addLecturerToLyceum(LyceumLecturerRequest.builder()
                .userId(lecturer.getId())
                .lyceumId(3L)
                .build());

        assertThat(lyceum.getLecturers()).containsExactly(lecturer);
        assertThat(lecturer.getLecturedLyceums()).containsExactly(lyceum);
        verify(lyceumRepository).save(lyceum);
        verify(userRepository).save(lecturer);
    }

    @Test
    void addLecturerToLyceumAsLyceumAdminAllowsMatchingLyceumId() {
        Lyceum lyceum = createLyceum(7L, "Lyceum", "Varna", "mail@example.com");
        User lyceumAdmin = createUser(2L);
        lyceumAdmin.setAdministratedLyceum(lyceum);
        User lecturer = createUser(8L);

        mockAuthenticatedUser(lyceumAdmin);
        when(userRepository.findById(lyceumAdmin.getId())).thenReturn(Optional.of(lyceumAdmin));
        when(userRepository.findById(lecturer.getId())).thenReturn(Optional.of(lecturer));

        lyceumService.addLecturerToLyceum(LyceumLecturerRequest.builder()
                .userId(lecturer.getId())
                .lyceumId(7L)
                .build());

        assertThat(lyceum.getLecturers()).containsExactly(lecturer);
        assertThat(lecturer.getLecturedLyceums()).containsExactly(lyceum);
        verify(lyceumRepository).save(lyceum);
        verify(userRepository).save(lecturer);
    }

    @Test
    void addLecturerToLyceumAsLyceumAdminUsesOwnLyceumWhenIdOmitted() {
        Lyceum lyceum = createLyceum(7L, "Lyceum", "Varna", "mail@example.com");
        User lyceumAdmin = createUser(2L);
        lyceumAdmin.setAdministratedLyceum(lyceum);
        User lecturer = createUser(8L);

        mockAuthenticatedUser(lyceumAdmin);
        when(userRepository.findById(lyceumAdmin.getId())).thenReturn(Optional.of(lyceumAdmin));
        when(userRepository.findById(lecturer.getId())).thenReturn(Optional.of(lecturer));

        lyceumService.addLecturerToLyceum(LyceumLecturerRequest.builder()
                .userId(lecturer.getId())
                .build());

        assertThat(lyceum.getLecturers()).containsExactly(lecturer);
        assertThat(lecturer.getLecturedLyceums()).containsExactly(lyceum);
        verify(lyceumRepository).save(lyceum);
        verify(userRepository).save(lecturer);
    }

    @Test
    void addLecturerToLyceumThrowsWhenLyceumAdminTargetsOtherLyceum() {
        Lyceum lyceum = createLyceum(7L, "Lyceum", "Varna", "mail@example.com");
        User lyceumAdmin = createUser(2L);
        lyceumAdmin.setAdministratedLyceum(lyceum);

        mockAuthenticatedUser(lyceumAdmin);
        when(userRepository.findById(lyceumAdmin.getId())).thenReturn(Optional.of(lyceumAdmin));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> lyceumService.addLecturerToLyceum(LyceumLecturerRequest.builder()
                        .userId(9L)
                        .lyceumId(99L)
                        .build()));

        assertThat(ex.getMessage()).isEqualTo("You do not have permission to modify this lyceum.");
        verifyNoInteractions(lyceumRepository);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void addLecturerToLyceumThrowsWhenLecturerAlreadyAssigned() {
        User admin = createUser(1L);
        admin.setRole(Role.ADMIN);
        User lecturer = createUser(5L);
        Lyceum lyceum = createLyceum(3L, "Lyceum", "Varna", "mail@example.com");
        lyceum.setLecturers(new ArrayList<>(List.of(lecturer)));

        mockAuthenticatedUser(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(lyceumRepository.findById(3L)).thenReturn(Optional.of(lyceum));
        when(userRepository.findById(lecturer.getId())).thenReturn(Optional.of(lecturer));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> lyceumService.addLecturerToLyceum(LyceumLecturerRequest.builder()
                        .userId(lecturer.getId())
                        .lyceumId(3L)
                        .build()));

        assertThat(ex.getMessage()).isEqualTo("User is already a lecturer for this lyceum.");
        verify(lyceumRepository, never()).save(any(Lyceum.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void removeLecturerFromLyceumThrowsWhenUserIdNull() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> lyceumService.removeLecturerFromLyceum(3L, null));

        assertThat(ex.getMessage()).isEqualTo("User id must be provided.");
        verifyNoInteractions(lyceumRepository, userRepository);
    }

    @Test
    void removeLecturerFromLyceumThrowsWhenLyceumIdNull() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> lyceumService.removeLecturerFromLyceum(null, 3L));

        assertThat(ex.getMessage()).isEqualTo("Lyceum id must be provided.");
        verifyNoInteractions(lyceumRepository, userRepository);
    }

    @Test
    void removeLecturerFromLyceumThrowsWhenLyceumMissing() {
        when(lyceumRepository.findWithLecturersById(3L)).thenReturn(Optional.empty());

        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> lyceumService.removeLecturerFromLyceum(3L, 5L));

        assertThat(ex.getMessage()).isEqualTo("Lyceum with id 3 not found.");
        verify(lyceumRepository).findWithLecturersById(3L);
        verifyNoInteractions(userRepository);
        verify(lyceumRepository, never()).save(any(Lyceum.class));
    }

    @Test
    void removeLecturerFromLyceumThrowsWhenUserNotAuthorized() {
        Lyceum lyceum = createLyceum(3L, "Lyceum", "Varna", "mail@example.com");
        User user = createUser(1L);

        mockAuthenticatedUser(user);
        when(lyceumRepository.findWithLecturersById(3L)).thenReturn(Optional.of(lyceum));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> lyceumService.removeLecturerFromLyceum(3L, 5L));

        assertThat(ex.getMessage()).isEqualTo("You do not have permission to modify this lyceum.");
        verify(userRepository, never()).findById(5L);
        verify(lyceumRepository, never()).save(any(Lyceum.class));
    }

    @Test
    void removeLecturerFromLyceumThrowsWhenLecturerMissing() {
        User admin = createUser(1L);
        admin.setRole(Role.ADMIN);
        Lyceum lyceum = createLyceum(3L, "Lyceum", "Varna", "mail@example.com");

        mockAuthenticatedUser(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(lyceumRepository.findWithLecturersById(3L)).thenReturn(Optional.of(lyceum));
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> lyceumService.removeLecturerFromLyceum(3L, 5L));

        assertThat(ex.getMessage()).isEqualTo("User with id 5 not found.");
        verify(lyceumRepository, never()).save(any(Lyceum.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void removeLecturerFromLyceumThrowsWhenUserNotLecturer() {
        User admin = createUser(1L);
        admin.setRole(Role.ADMIN);
        User lecturer = createUser(5L);
        Lyceum lyceum = createLyceum(3L, "Lyceum", "Varna", "mail@example.com");
        lyceum.setLecturers(new ArrayList<>());
        lecturer.setLecturedLyceums(new ArrayList<>());

        mockAuthenticatedUser(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(lyceumRepository.findWithLecturersById(3L)).thenReturn(Optional.of(lyceum));
        when(userRepository.findById(lecturer.getId())).thenReturn(Optional.of(lecturer));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> lyceumService.removeLecturerFromLyceum(3L, lecturer.getId()));

        assertThat(ex.getMessage()).isEqualTo("User is not a lecturer for this lyceum.");
        verify(lyceumRepository, never()).save(any(Lyceum.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void removeLecturerFromLyceumAllowsLyceumAdministrator() {
        Lyceum lyceum = createLyceum(3L, "Lyceum", "Varna", "mail@example.com");
        User lyceumAdmin = createUser(1L);
        lyceumAdmin.setAdministratedLyceum(lyceum);
        User lecturer = createUser(5L);
        lyceum.setLecturers(new ArrayList<>(List.of(lecturer)));
        lecturer.setLecturedLyceums(new ArrayList<>(List.of(lyceum)));

        mockAuthenticatedUser(lyceumAdmin);
        when(userRepository.findById(lyceumAdmin.getId())).thenReturn(Optional.of(lyceumAdmin));
        when(lyceumRepository.findWithLecturersById(3L)).thenReturn(Optional.of(lyceum));
        when(userRepository.findById(lecturer.getId())).thenReturn(Optional.of(lecturer));

        lyceumService.removeLecturerFromLyceum(3L, lecturer.getId());

        assertThat(lyceum.getLecturers()).doesNotContain(lecturer);
        assertThat(lecturer.getLecturedLyceums()).doesNotContain(lyceum);
        verify(lyceumRepository).save(lyceum);
        verify(userRepository).save(lecturer);
    }

    @Test
    void removeLecturerFromLyceumUnlinksBothSides() {
        User admin = createUser(1L);
        admin.setRole(Role.ADMIN);
        User lecturer = createUser(5L);
        Lyceum lyceum = createLyceum(3L, "Lyceum", "Varna", "mail@example.com");
        lyceum.setLecturers(new ArrayList<>(List.of(lecturer)));
        lecturer.setLecturedLyceums(new ArrayList<>(List.of(lyceum)));

        mockAuthenticatedUser(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(lyceumRepository.findWithLecturersById(3L)).thenReturn(Optional.of(lyceum));
        when(userRepository.findById(lecturer.getId())).thenReturn(Optional.of(lecturer));

        lyceumService.removeLecturerFromLyceum(3L, lecturer.getId());

        assertThat(lyceum.getLecturers()).doesNotContain(lecturer);
        assertThat(lecturer.getLecturedLyceums()).doesNotContain(lyceum);
        verify(lyceumRepository).save(lyceum);
        verify(userRepository).save(lecturer);
    }

    @Test
    void removeLecturerFromLyceumAllowsRemovalWhenOnlyLyceumSidePresent() {
        User admin = createUser(1L);
        admin.setRole(Role.ADMIN);
        User lecturer = createUser(5L);
        Lyceum lyceum = createLyceum(3L, "Lyceum", "Varna", "mail@example.com");
        lyceum.setLecturers(new ArrayList<>(List.of(lecturer)));
        lecturer.setLecturedLyceums(null);

        mockAuthenticatedUser(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(lyceumRepository.findWithLecturersById(3L)).thenReturn(Optional.of(lyceum));
        when(userRepository.findById(lecturer.getId())).thenReturn(Optional.of(lecturer));

        lyceumService.removeLecturerFromLyceum(3L, lecturer.getId());

        assertThat(lyceum.getLecturers()).doesNotContain(lecturer);
        assertThat(lecturer.getLecturedLyceums()).isNull();
        verify(lyceumRepository).save(lyceum);
        verify(userRepository).save(lecturer);
    }

    @Test
    void removeLecturerFromLyceumAllowsRemovalWhenOnlyUserSidePresent() {
        User admin = createUser(1L);
        admin.setRole(Role.ADMIN);
        User lecturer = createUser(5L);
        Lyceum lyceum = createLyceum(3L, "Lyceum", "Varna", "mail@example.com");
        lyceum.setLecturers(null);
        lecturer.setLecturedLyceums(new ArrayList<>(List.of(lyceum)));

        mockAuthenticatedUser(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(lyceumRepository.findWithLecturersById(3L)).thenReturn(Optional.of(lyceum));
        when(userRepository.findById(lecturer.getId())).thenReturn(Optional.of(lecturer));

        lyceumService.removeLecturerFromLyceum(3L, lecturer.getId());

        assertThat(lecturer.getLecturedLyceums()).doesNotContain(lyceum);
        verify(lyceumRepository).save(lyceum);
        verify(userRepository).save(lecturer);
    }

    private void mockAuthenticatedUser(User user) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User createUser(Long id) {
        return User.builder()
                .id(id)
                .firstname("John")
                .lastname("Doe")
                .email("john" + id + "@example.com")
                .username("john" + id)
                .password("password123")
                .role(Role.USER)
                .enabled(true)
                .build();
    }

    private Lyceum createLyceum(Long id, String name, String town, String email) {
        Lyceum lyceum = new Lyceum();
        lyceum.setId(id);
        lyceum.setName(name);
        lyceum.setTown(town);
        lyceum.setEmail(email);
        lyceum.setChitalishtaUrl("http://example.com");
        lyceum.setStatus("Active");
        lyceum.setBulstat("123456789");
        lyceum.setChairman("Chairman");
        lyceum.setSecretary("Secretary");
        lyceum.setPhone("+359123456");
        lyceum.setRegion("Region");
        lyceum.setMunicipality("Municipality");
        lyceum.setAddress("Some address");
        lyceum.setUrlToLibrariesSite("http://library.example.com");
        lyceum.setRegistrationNumber(42);
        lyceum.setLongitude(23.5);
        lyceum.setLatitude(42.7);
        lyceum.setVerificationStatus(VerificationStatus.NOT_VERIFIED);
        return lyceum;
    }
}
