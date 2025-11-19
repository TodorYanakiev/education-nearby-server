package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.config.S3Properties;
import com.dev.education_nearby_server.enums.AgeGroup;
import com.dev.education_nearby_server.enums.CourseType;
import com.dev.education_nearby_server.enums.Role;
import com.dev.education_nearby_server.exceptions.common.AccessDeniedException;
import com.dev.education_nearby_server.exceptions.common.BadRequestException;
import com.dev.education_nearby_server.exceptions.common.NoSuchElementException;
import com.dev.education_nearby_server.models.dto.request.CourseRequest;
import com.dev.education_nearby_server.models.dto.response.CourseResponse;
import com.dev.education_nearby_server.models.entity.Course;
import com.dev.education_nearby_server.models.entity.Lyceum;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.repositories.CourseImageRepository;
import com.dev.education_nearby_server.repositories.CourseRepository;
import com.dev.education_nearby_server.repositories.LyceumRepository;
import com.dev.education_nearby_server.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseImageRepository courseImageRepository;
    @Mock
    private LyceumRepository lyceumRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private S3Properties s3Properties;

    @InjectMocks
    private CourseService courseService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createCourseFailsForNonAdminWithoutLyceum() {
        User user = createUser(1L, Role.USER);
        authenticate(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        CourseRequest request = CourseRequest.builder()
                .name("Course")
                .description("Description")
                .type(CourseType.MUSIC)
                .ageGroupList(List.of(AgeGroup.ADULT))
                .build();

        assertThrows(AccessDeniedException.class, () -> courseService.createCourse(request));

        verify(lyceumRepository, never()).findWithLecturersById(any());
        verify(courseRepository, never()).save(any());
    }

    @Test
    void createCourseAsAdminSavesCourse() {
        User admin = createUser(1L, Role.ADMIN);
        authenticate(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        Course saved = new Course();
        saved.setId(10L);
        when(courseRepository.save(any())).thenReturn(saved);

        CourseRequest request = CourseRequest.builder()
                .name("Course")
                .description("Description")
                .type(CourseType.MUSIC)
                .ageGroupList(List.of(AgeGroup.ADULT))
                .lecturerIds(List.of(2L, 3L))
                .build();

        when(userRepository.findAllById(any())).thenReturn(List.of(createUser(2L, Role.USER), createUser(3L, Role.USER)));

        CourseResponse response = courseService.createCourse(request);

        verify(courseRepository).save(courseCaptor.capture());
        Course persisted = courseCaptor.getValue();
        assertThat(persisted.getName()).isEqualTo("Course");
        assertThat(persisted.getType()).isEqualTo(CourseType.MUSIC);
        assertThat(persisted.getLecturers()).hasSize(2);

        assertEquals(10L, response.getId());
    }

    @Test
    void createCourseAsLyceumLecturerAutoAssignsLecturer() {
        Lyceum lyceum = new Lyceum();
        lyceum.setId(5L);
        User lecturer = createUser(7L, Role.USER);
        lyceum.setLecturers(new ArrayList<>(List.of(lecturer)));
        lecturer.setLecturedLyceums(new ArrayList<>(List.of(lyceum)));

        authenticate(lecturer);
        when(userRepository.findById(lecturer.getId())).thenReturn(Optional.of(lecturer));
        when(lyceumRepository.findWithLecturersById(5L)).thenReturn(Optional.of(lyceum));

        Course saved = new Course();
        saved.setId(20L);
        when(courseRepository.save(any())).thenReturn(saved);

        CourseRequest request = CourseRequest.builder()
                .name("Course")
                .description("Description")
                .type(CourseType.MUSIC)
                .ageGroupList(List.of(AgeGroup.ADULT))
                .lyceumId(5L)
                .build();

        when(userRepository.findAllById(any())).thenReturn(List.of(lecturer));

        CourseResponse response = courseService.createCourse(request);

        verify(courseRepository).save(any(Course.class));
        assertThat(response.getId()).isEqualTo(20L);
        verify(userRepository).findAllById(any());
    }

    @Test
    void createCourseThrowsWhenLecturerNotFound() {
        User admin = createUser(1L, Role.ADMIN);
        authenticate(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        CourseRequest request = CourseRequest.builder()
                .name("Course")
                .description("Description")
                .type(CourseType.MUSIC)
                .ageGroupList(List.of(AgeGroup.ADULT))
                .lecturerIds(List.of(5L))
                .build();

        when(userRepository.findAllById(any())).thenReturn(List.of());

        assertThrows(NoSuchElementException.class, () -> courseService.createCourse(request));
    }

    @Test
    void createCourseThrowsWhenRequestNull() {
        User admin = createUser(1L, Role.ADMIN);
        authenticate(admin);

        assertThrows(BadRequestException.class, () -> courseService.createCourse(null));
        verifyNoInteractions(userRepository);
    }

    private void authenticate(User user) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User createUser(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setFirstname("John");
        user.setLastname("Doe");
        user.setEmail("john" + id + "@example.com");
        user.setUsername("john" + id);
        user.setPassword("password");
        user.setRole(role);
        user.setEnabled(true);
        user.setCoursesLectured(new ArrayList<>());
        return user;
    }
}
