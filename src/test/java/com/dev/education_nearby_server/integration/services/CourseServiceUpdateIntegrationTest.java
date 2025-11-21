package com.dev.education_nearby_server.integration.services;

import com.dev.education_nearby_server.enums.AgeGroup;
import com.dev.education_nearby_server.enums.CourseType;
import com.dev.education_nearby_server.enums.Role;
import com.dev.education_nearby_server.models.dto.request.CourseUpdateRequest;
import com.dev.education_nearby_server.models.dto.response.CourseResponse;
import com.dev.education_nearby_server.models.entity.Course;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.repositories.CourseRepository;
import com.dev.education_nearby_server.repositories.UserRepository;
import com.dev.education_nearby_server.services.CourseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CourseServiceUpdateIntegrationTest {

    @Autowired
    private CourseService courseService;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        courseRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void updateCourseUpdatesAgeGroups() {
        User admin = persistUser(Role.ADMIN);
        Course course = persistCourse();
        authenticate(admin);

        CourseUpdateRequest request = CourseUpdateRequest.builder()
                .ageGroupList(List.of(
                        AgeGroup.TEEN,
                        AgeGroup.YOUNG_ADULT,
                        AgeGroup.PRE_TEEN
                ))
                .build();

        CourseResponse response = courseService.updateCourse(course.getId(), request);

        assertThat(response.getAgeGroupList())
                .containsExactly(AgeGroup.TEEN, AgeGroup.YOUNG_ADULT, AgeGroup.PRE_TEEN);
        List<AgeGroup> storedAgeGroups = loadPersistedAgeGroups(course.getId());
        assertThat(storedAgeGroups)
                .containsExactly(AgeGroup.TEEN, AgeGroup.YOUNG_ADULT, AgeGroup.PRE_TEEN);
    }

    private Course persistCourse() {
        Course course = new Course();
        course.setName("Course " + UUID.randomUUID());
        course.setDescription("Description");
        course.setType(CourseType.MUSIC);
        course.setAgeGroupList(new ArrayList<>(List.of(AgeGroup.ADULT)));
        course.setLecturers(new ArrayList<>());
        course.setImages(new ArrayList<>());
        return courseRepository.save(course);
    }

    private User persistUser(Role role) {
        User user = new User();
        user.setFirstname("Test");
        user.setLastname("User");
        user.setEmail("user-" + UUID.randomUUID() + "@example.com");
        user.setUsername("user-" + UUID.randomUUID());
        user.setPassword("Password123!");
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private void authenticate(User user) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private List<AgeGroup> loadPersistedAgeGroups(Long courseId) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        return template.execute(status -> {
            Course managed = courseRepository.findById(courseId).orElseThrow();
            return new ArrayList<>(managed.getAgeGroupList());
        });
    }
}
