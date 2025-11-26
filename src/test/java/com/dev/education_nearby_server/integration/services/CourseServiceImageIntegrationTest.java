package com.dev.education_nearby_server.integration.services;

import com.dev.education_nearby_server.enums.AgeGroup;
import com.dev.education_nearby_server.enums.CourseType;
import com.dev.education_nearby_server.enums.ImageRole;
import com.dev.education_nearby_server.enums.Role;
import com.dev.education_nearby_server.exceptions.common.AccessDeniedException;
import com.dev.education_nearby_server.exceptions.common.BadRequestException;
import com.dev.education_nearby_server.exceptions.common.ConflictException;
import com.dev.education_nearby_server.models.dto.request.CourseImageRequest;
import com.dev.education_nearby_server.models.dto.response.CourseImageResponse;
import com.dev.education_nearby_server.models.entity.Course;
import com.dev.education_nearby_server.models.entity.CourseImage;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.repositories.CourseImageRepository;
import com.dev.education_nearby_server.repositories.CourseRepository;
import com.dev.education_nearby_server.repositories.UserRepository;
import com.dev.education_nearby_server.services.CourseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "app.s3.bucket-name=education-test-bucket",
        "app.s3.public-base-url=https://education-test-bucket.s3.amazonaws.com",
        "app.s3.allowed-prefix=courses/"
})
class CourseServiceImageIntegrationTest {

    @Autowired
    private CourseService courseService;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private CourseImageRepository courseImageRepository;
    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCourseImagesReturnsSortedPayload() {
        Course course = persistCourse();
        CourseImage imageA = persistImage(course, "courses/" + course.getId() + "/gallery-2.png", ImageRole.GALLERY, 1);
        CourseImage imageB = persistImage(course, "courses/" + course.getId() + "/logo.png", ImageRole.LOGO, 0);
        courseImageRepository.saveAll(List.of(imageA, imageB));

        List<CourseImageResponse> responses = courseService.getCourseImages(course.getId());

        assertThat(responses).hasSize(2);
        assertThat(responses.getFirst().getRole()).isEqualTo(ImageRole.LOGO);
        assertThat(responses.get(1).getOrderIndex()).isEqualTo(1);
    }

    @Test
    void addCourseImageWithS3KeyPersistsImage() {
        User admin = persistUser(Role.ADMIN);
        Course course = persistCourse();
        authenticate(admin);

        CourseImageRequest request = CourseImageRequest.builder()
                .s3Key("courses/" + course.getId() + "/main.png")
                .role(ImageRole.MAIN)
                .altText("Cover")
                .width(640)
                .height(480)
                .mimeType("image/png")
                .orderIndex(0)
                .build();

        CourseImageResponse response = courseService.addCourseImage(course.getId(), request);

        CourseImage stored = courseImageRepository.findById(response.getId()).orElseThrow();
        assertThat(stored.getS3Key()).isEqualTo(request.getS3Key());
        assertThat(stored.getUrl()).isEqualTo("https://education-test-bucket.s3.amazonaws.com/" + request.getS3Key());
        assertThat(stored.getAltText()).isEqualTo("Cover");
    }

    @Test
    void addCourseImageExtractsKeyFromUrl() {
        User admin = persistUser(Role.ADMIN);
        Course course = persistCourse();
        authenticate(admin);

        CourseImageRequest request = CourseImageRequest.builder()
                .url("https://education-test-bucket.s3.amazonaws.com/courses/" + course.getId() + "/gallery/item.png")
                .role(ImageRole.GALLERY)
                .orderIndex(3)
                .build();

        CourseImageResponse response = courseService.addCourseImage(course.getId(), request);

        CourseImage stored = courseImageRepository.findById(response.getId()).orElseThrow();
        assertThat(stored.getS3Key()).isEqualTo("courses/" + course.getId() + "/gallery/item.png");
        assertThat(stored.getRole()).isEqualTo(ImageRole.GALLERY);
    }

    @Test
    void addCourseImagePreventsDuplicateRoles() {
        User admin = persistUser(Role.ADMIN);
        Course course = persistCourse();
        authenticate(admin);

        CourseImageRequest mainImage = CourseImageRequest.builder()
                .s3Key("courses/" + course.getId() + "/main.png")
                .role(ImageRole.MAIN)
                .build();
        courseService.addCourseImage(course.getId(), mainImage);

        CourseImageRequest duplicate = CourseImageRequest.builder()
                .s3Key("courses/" + course.getId() + "/main-2.png")
                .role(ImageRole.MAIN)
                .build();

        assertThrows(ConflictException.class, () -> courseService.addCourseImage(course.getId(), duplicate));
    }

    @Test
    void deleteCourseImageRemovesRecord() {
        User admin = persistUser(Role.ADMIN);
        Course course = persistCourse();
        authenticate(admin);
        CourseImageResponse response = courseService.addCourseImage(course.getId(), CourseImageRequest.builder()
                .s3Key("courses/" + course.getId() + "/logo.png")
                .role(ImageRole.LOGO)
                .build());
        assertThat(courseImageRepository.findAll()).hasSize(1);

        courseService.deleteCourseImage(course.getId(), response.getId());

        assertThat(courseImageRepository.findAll()).isEmpty();
    }

    @Test
    void deleteCourseImageRejectsDifferentCourse() {
        User admin = persistUser(Role.ADMIN);
        Course owner = persistCourse();
        Course other = persistCourse();
        authenticate(admin);
        CourseImageResponse response = courseService.addCourseImage(owner.getId(), CourseImageRequest.builder()
                .s3Key("courses/" + owner.getId() + "/logo.png")
                .role(ImageRole.LOGO)
                .build());

        assertThrows(BadRequestException.class, () -> courseService.deleteCourseImage(other.getId(), response.getId()));
    }

    @Test
    void addCourseImageRequiresPermission() {
        User user = persistUser(Role.USER);
        Course course = persistCourse();
        authenticate(user);

        CourseImageRequest request = CourseImageRequest.builder()
                .s3Key("courses/" + course.getId() + "/main.png")
                .role(ImageRole.MAIN)
                .build();

        assertThrows(AccessDeniedException.class, () -> courseService.addCourseImage(course.getId(), request));
    }

    private Course persistCourse() {
        Course course = new Course();
        course.setName("Course " + UUID.randomUUID());
        course.setDescription("Description");
        course.setType(CourseType.MUSIC);
        course.setAgeGroupList(new ArrayList<>(List.of(AgeGroup.ADULT)));
        course.setImages(new ArrayList<>());
        course.setLecturers(new ArrayList<>());
        return courseRepository.save(course);
    }

    private CourseImage persistImage(Course course, String key, ImageRole role, int orderIndex) {
        CourseImage image = new CourseImage();
        image.setCourse(course);
        image.setS3Key(key);
        image.setUrl("https://education-test-bucket.s3.amazonaws.com/" + key);
        image.setRole(role);
        image.setOrderIndex(orderIndex);
        return image;
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
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
