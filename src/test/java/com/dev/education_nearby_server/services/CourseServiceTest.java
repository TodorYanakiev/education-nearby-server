package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.config.S3Properties;
import com.dev.education_nearby_server.enums.AgeGroup;
import com.dev.education_nearby_server.enums.CourseType;
import com.dev.education_nearby_server.enums.ImageRole;
import com.dev.education_nearby_server.enums.Role;
import com.dev.education_nearby_server.exceptions.common.AccessDeniedException;
import com.dev.education_nearby_server.exceptions.common.BadRequestException;
import com.dev.education_nearby_server.exceptions.common.ConflictException;
import com.dev.education_nearby_server.exceptions.common.NoSuchElementException;
import com.dev.education_nearby_server.exceptions.common.ValidationException;
import com.dev.education_nearby_server.models.dto.request.CourseFilterRequest;
import com.dev.education_nearby_server.models.dto.request.CourseImageRequest;
import com.dev.education_nearby_server.models.dto.request.CourseRequest;
import com.dev.education_nearby_server.models.dto.request.CourseUpdateRequest;
import com.dev.education_nearby_server.models.dto.response.CourseImageResponse;
import com.dev.education_nearby_server.models.dto.response.CourseResponse;
import com.dev.education_nearby_server.models.entity.Course;
import com.dev.education_nearby_server.models.entity.CourseSchedule;
import com.dev.education_nearby_server.models.entity.CourseImage;
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

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
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
    void getAllCoursesReturnsMappedResponses() {
        Course first = createCourseEntity(1L);
        first.getLecturers().add(createUser(5L, Role.USER));
        Course second = createCourseEntity(2L);
        second.setLecturers(new ArrayList<>());
        when(courseRepository.findAll()).thenReturn(List.of(first, second));

        List<CourseResponse> responses = courseService.getAllCourses();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
        assertThat(responses.get(0).getImages()).isEmpty();
        assertThat(responses.get(1).getId()).isEqualTo(2L);
        verify(courseRepository).findAll();
    }

    @Test
    void getCoursesByLyceumIdReturnsMappedResponses() {
        Course course = createCourseEntity(3L);
        Lyceum lyceum = new Lyceum();
        lyceum.setId(7L);
        course.setLyceum(lyceum);
        when(courseRepository.findAllByLyceum_Id(7L)).thenReturn(List.of(course));

        List<CourseResponse> responses = courseService.getCoursesByLyceumId(7L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getId()).isEqualTo(3L);
        assertThat(responses.getFirst().getLyceumId()).isEqualTo(7L);
        verify(courseRepository).findAllByLyceum_Id(7L);
    }

    @Test
    void getCoursesByLyceumIdReturnsEmptyWhenRepositoryEmpty() {
        when(courseRepository.findAllByLyceum_Id(8L)).thenReturn(List.of());

        List<CourseResponse> responses = courseService.getCoursesByLyceumId(8L);

        assertThat(responses).isEmpty();
        verify(courseRepository).findAllByLyceum_Id(8L);
    }

    @Test
    void getCoursesByLyceumIdThrowsWhenIdMissing() {
        assertThrows(BadRequestException.class, () -> courseService.getCoursesByLyceumId(null));
        verifyNoInteractions(courseRepository);
    }

    @Test
    void filterCoursesUsesDefaultsWhenRequestNull() {
        Course course = createCourseEntity(1L);
        when(courseRepository.filterCourses(anyList(), anyBoolean(), anyList(), anyBoolean(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(course));

        List<CourseResponse> responses = courseService.filterCourses(null);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getId()).isEqualTo(1L);
        verify(courseRepository).filterCourses(
                List.of(),
                false,
                List.of(),
                false,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    @Test
    void filterCoursesSanitizesNullFilters() {
        List<CourseType> courseTypes = new ArrayList<>();
        courseTypes.add(null);
        List<AgeGroup> ageGroups = new ArrayList<>();
        ageGroups.add(AgeGroup.TEEN);
        ageGroups.add(null);
        CourseFilterRequest request = CourseFilterRequest.builder()
                .courseTypes(courseTypes)
                .ageGroups(ageGroups)
                .minPrice(10f)
                .maxPrice(20f)
                .startTimeFrom(LocalTime.of(9, 0))
                .startTimeTo(LocalTime.of(11, 0))
                .build();
        when(courseRepository.filterCourses(anyList(), anyBoolean(), anyList(), anyBoolean(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(createCourseEntity(2L)));

        courseService.filterCourses(request);

        verify(courseRepository).filterCourses(
                List.of(),
                false,
                List.of(AgeGroup.TEEN),
                true,
                10f,
                20f,
                null,
                null,
                LocalTime.of(9, 0),
                LocalTime.of(11, 0)
        );
    }

    @Test
    void filterCoursesThrowsWhenMinPriceExceedsMaxPrice() {
        CourseFilterRequest request = CourseFilterRequest.builder()
                .minPrice(100f)
                .maxPrice(50f)
                .build();

        assertThrows(BadRequestException.class, () -> courseService.filterCourses(request));
        verifyNoInteractions(courseRepository);
    }

    @Test
    void filterCoursesThrowsWhenStartTimeRangeInvalid() {
        CourseFilterRequest request = CourseFilterRequest.builder()
                .startTimeFrom(LocalTime.of(12, 0))
                .startTimeTo(LocalTime.of(10, 0))
                .build();

        assertThrows(BadRequestException.class, () -> courseService.filterCourses(request));
        verifyNoInteractions(courseRepository);
    }

    @Test
    void getCourseByIdReturnsResponse() {
        Course course = createCourseEntity(9L);
        CourseImage gallery = buildCourseImage(1L, course, "courses/9/gallery.jpg", "https://cdn/gallery.jpg", ImageRole.GALLERY, 0);
        CourseImage main = buildCourseImage(2L, course, "courses/9/main.jpg", "https://cdn/main.jpg", ImageRole.MAIN, 1);
        course.getImages().add(main);
        course.getImages().add(gallery);
        when(courseRepository.findDetailedById(9L)).thenReturn(Optional.of(course));

        CourseResponse response = courseService.getCourseById(9L);

        assertThat(response.getId()).isEqualTo(9L);
        assertThat(response.getName()).isEqualTo(course.getName());
        assertThat(response.getImages()).hasSize(2);
        assertThat(response.getImages().getFirst().getId()).isEqualTo(1L);
        assertThat(response.getImages().get(1).getRole()).isEqualTo(ImageRole.MAIN);
        verify(courseRepository).findDetailedById(9L);
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

    @Test
    void getCourseImagesReturnsMappedResponses() {
        Course course = createCourseEntity(15L);
        when(courseRepository.findById(15L)).thenReturn(Optional.of(course));
        CourseImage first = buildCourseImage(1L, course, "courses/15/main.jpg", "https://cdn/images/15/main.jpg", ImageRole.MAIN, 0);
        CourseImage second = buildCourseImage(2L, course, "courses/15/gallery.jpg", "https://cdn/images/15/gallery.jpg", ImageRole.GALLERY, 1);
        when(courseImageRepository.findAllByCourseIdOrderByOrderIndexAscIdAsc(15L))
                .thenReturn(List.of(first, second));

        List<CourseImageResponse> responses = courseService.getCourseImages(15L);

        assertThat(responses).hasSize(2);
        assertThat(responses.getFirst().getId()).isEqualTo(1L);
        assertThat(responses.get(1).getRole()).isEqualTo(ImageRole.GALLERY);
        verify(courseImageRepository).findAllByCourseIdOrderByOrderIndexAscIdAsc(15L);
    }

    @Test
    void addCourseImageAsAdminPersistsImage() {
        Course course = createCourseEntity(5L);
        when(courseRepository.findDetailedById(5L)).thenReturn(Optional.of(course));
        User admin = createUser(11L, Role.ADMIN);
        authenticate(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        mockS3Properties("courses/", null, "https://cdn.example.com");
        when(courseImageRepository.findByS3Key("courses/5/main.png")).thenReturn(Optional.empty());
        ArgumentCaptor<CourseImage> imageCaptor = ArgumentCaptor.forClass(CourseImage.class);
        when(courseImageRepository.save(any())).thenAnswer(invocation -> {
            CourseImage image = invocation.getArgument(0);
            image.setId(77L);
            return image;
        });

        CourseImageRequest request = CourseImageRequest.builder()
                .s3Key("courses/5/main.png")
                .role(ImageRole.MAIN)
                .altText("  Cover  ")
                .width(400)
                .height(250)
                .mimeType(" image/png ")
                .orderIndex(2)
                .build();

        CourseImageResponse response = courseService.addCourseImage(5L, request);

        verify(courseImageRepository).save(imageCaptor.capture());
        CourseImage persisted = imageCaptor.getValue();
        assertThat(persisted.getS3Key()).isEqualTo("courses/5/main.png");
        assertThat(persisted.getUrl()).isEqualTo("https://cdn.example.com/courses/5/main.png");
        assertThat(persisted.getAltText()).isEqualTo("Cover");
        assertThat(persisted.getOrderIndex()).isEqualTo(2);
        assertThat(response.getId()).isEqualTo(77L);
        assertThat(course.getImages()).hasSize(1);
    }

    @Test
    void addCourseImageThrowsWhenRoleAlreadyExists() {
        Course course = createCourseEntity(6L);
        course.getImages().add(buildCourseImage(1L, course, "courses/6/main.png", "https://cdn/main.png", ImageRole.MAIN, 0));
        when(courseRepository.findDetailedById(6L)).thenReturn(Optional.of(course));
        User admin = createUser(10L, Role.ADMIN);
        authenticate(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        mockS3Properties("courses/", null, "https://cdn.example.com");
        when(courseImageRepository.findByS3Key("courses/6/new.png")).thenReturn(Optional.empty());

        CourseImageRequest request = CourseImageRequest.builder()
                .s3Key("courses/6/new.png")
                .role(ImageRole.MAIN)
                .build();

        assertThrows(ConflictException.class, () -> courseService.addCourseImage(6L, request));
        verify(courseImageRepository, never()).save(any());
    }

    @Test
    void addCourseImageThrowsWhenS3KeyAlreadyUsed() {
        Course course = createCourseEntity(7L);
        when(courseRepository.findDetailedById(7L)).thenReturn(Optional.of(course));
        User admin = createUser(20L, Role.ADMIN);
        authenticate(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        mockS3Properties("courses/", null, "https://cdn.example.com");
        when(courseImageRepository.findByS3Key("courses/7/dup.png")).thenReturn(Optional.of(new CourseImage()));

        CourseImageRequest request = CourseImageRequest.builder()
                .s3Key("courses/7/dup.png")
                .role(ImageRole.LOGO)
                .build();

        assertThrows(ConflictException.class, () -> courseService.addCourseImage(7L, request));
        verify(courseImageRepository, never()).save(any());
    }

    @Test
    void addCourseImageThrowsWhenKeyAndUrlMissing() {
        Course course = createCourseEntity(30L);
        when(courseRepository.findDetailedById(30L)).thenReturn(Optional.of(course));
        User admin = createUser(60L, Role.ADMIN);
        authenticate(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        CourseImageRequest request = CourseImageRequest.builder()
                .role(ImageRole.MAIN)
                .build();

        assertThrows(ValidationException.class, () -> courseService.addCourseImage(30L, request));
        verify(courseImageRepository, never()).findByS3Key(any());
    }

    @Test
    void addCourseImageThrowsWhenUrlMalformed() {
        Course course = createCourseEntity(31L);
        when(courseRepository.findDetailedById(31L)).thenReturn(Optional.of(course));
        User admin = createUser(61L, Role.ADMIN);
        authenticate(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        CourseImageRequest request = CourseImageRequest.builder()
                .url("ht@tp://invalid")
                .role(ImageRole.MAIN)
                .build();

        assertThrows(ValidationException.class, () -> courseService.addCourseImage(31L, request));
        verify(courseImageRepository, never()).findByS3Key(any());
    }

    @Test
    void addCourseImageAllowsMultipleGalleryImages() {
        Course course = createCourseEntity(32L);
        course.getImages().add(buildCourseImage(10L, course, "courses/32/gallery-1.png", "https://cdn/gallery-1.png", ImageRole.GALLERY, 0));
        when(courseRepository.findDetailedById(32L)).thenReturn(Optional.of(course));
        User admin = createUser(62L, Role.ADMIN);
        authenticate(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        mockS3Properties("courses/", null, "https://cdn.example.com");
        when(courseImageRepository.findByS3Key("courses/32/gallery-2.png")).thenReturn(Optional.empty());
        when(courseImageRepository.save(any())).thenAnswer(invocation -> {
            CourseImage image = invocation.getArgument(0);
            image.setId(320L);
            return image;
        });

        CourseImageRequest request = CourseImageRequest.builder()
                .s3Key("courses/32/gallery-2.png")
                .role(ImageRole.GALLERY)
                .orderIndex(2)
                .build();

        CourseImageResponse response = courseService.addCourseImage(32L, request);

        assertThat(response.getId()).isEqualTo(320L);
        assertThat(course.getImages()).hasSize(2);
        assertThat(course.getImages().getLast().getRole()).isEqualTo(ImageRole.GALLERY);
    }

    @Test
    void addCourseImageBuildsUrlUsingBucketWhenNoPublicBaseConfigured() {
        Course course = createCourseEntity(70L);
        when(courseRepository.findDetailedById(70L)).thenReturn(Optional.of(course));
        User admin = createUser(23L, Role.ADMIN);
        authenticate(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        mockS3Properties("courses/", "education-bucket", null);
        when(courseImageRepository.findByS3Key("courses/70/logo.png")).thenReturn(Optional.empty());
        when(courseImageRepository.save(any())).thenAnswer(invocation -> {
            CourseImage image = invocation.getArgument(0);
            image.setId(700L);
            return image;
        });

        CourseImageRequest request = CourseImageRequest.builder()
                .s3Key("courses/70/logo.png")
                .role(ImageRole.LOGO)
                .build();

        CourseImageResponse response = courseService.addCourseImage(70L, request);

        assertThat(response.getUrl()).isEqualTo("https://education-bucket.s3.amazonaws.com/courses/70/logo.png");
        assertThat(course.getImages()).hasSize(1);
        assertThat(course.getImages().getFirst().getUrl()).isEqualTo(response.getUrl());
    }

    @Test
    void addCourseImageExtractsKeyFromUrlWithBucketPrefix() {
        Course course = createCourseEntity(71L);
        when(courseRepository.findDetailedById(71L)).thenReturn(Optional.of(course));
        User admin = createUser(24L, Role.ADMIN);
        authenticate(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        mockS3Properties("courses/", "education-bucket", "https://education-bucket.s3.amazonaws.com");
        when(courseImageRepository.findByS3Key("courses/71/gallery/img.png")).thenReturn(Optional.empty());
        when(courseImageRepository.save(any())).thenAnswer(invocation -> {
            CourseImage image = invocation.getArgument(0);
            image.setId(710L);
            return image;
        });

        String url = "https://education-bucket.s3.amazonaws.com/education-bucket/courses/71/gallery/img.png";
        CourseImageRequest request = CourseImageRequest.builder()
                .url(url)
                .role(ImageRole.GALLERY)
                .orderIndex(5)
                .build();

        CourseImageResponse response = courseService.addCourseImage(71L, request);

        assertThat(response.getId()).isEqualTo(710L);
        assertThat(response.getS3Key()).isEqualTo("courses/71/gallery/img.png");
        assertThat(response.getUrl()).isEqualTo(url);
        assertThat(course.getImages()).hasSize(1);
        assertThat(course.getImages().getFirst().getS3Key()).isEqualTo("courses/71/gallery/img.png");
    }

    @Test
    void addCourseImageThrowsWhenKeyOutsideAllowedPrefix() {
        Course course = createCourseEntity(8L);
        when(courseRepository.findDetailedById(8L)).thenReturn(Optional.of(course));
        User admin = createUser(21L, Role.ADMIN);
        authenticate(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        mockS3Properties("courses/", null, "https://cdn.example.com");

        CourseImageRequest request = CourseImageRequest.builder()
                .s3Key("invalid/main.png")
                .role(ImageRole.MAIN)
                .build();

        assertThrows(ValidationException.class, () -> courseService.addCourseImage(8L, request));
        verify(courseImageRepository, never()).save(any());
    }

    @Test
    void addCourseImageThrowsWhenBucketDoesNotMatchUrl() {
        Course course = createCourseEntity(9L);
        when(courseRepository.findDetailedById(9L)).thenReturn(Optional.of(course));
        User admin = createUser(22L, Role.ADMIN);
        authenticate(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        mockS3Properties("courses/", "expected-bucket", "https://expected-bucket.s3.amazonaws.com");

        CourseImageRequest request = CourseImageRequest.builder()
                .url("https://another-bucket.s3.amazonaws.com/courses/9/image.png")
                .role(ImageRole.MAIN)
                .build();

        assertThrows(ValidationException.class, () -> courseService.addCourseImage(9L, request));
        verify(courseImageRepository, never()).save(any());
    }

    @Test
    void addCourseImageThrowsWhenUserCannotModifyCourse() {
        Course course = createCourseEntity(12L);
        when(courseRepository.findDetailedById(12L)).thenReturn(Optional.of(course));
        User user = createUser(30L, Role.USER);
        authenticate(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        CourseImageRequest request = CourseImageRequest.builder()
                .s3Key("courses/12/main.png")
                .role(ImageRole.MAIN)
                .build();

        assertThrows(AccessDeniedException.class, () -> courseService.addCourseImage(12L, request));
        verify(courseImageRepository, never()).findByS3Key(any());
    }

    @Test
    void deleteCourseImageRemovesEntity() {
        Course course = createCourseEntity(13L);
        CourseImage courseImage = buildCourseImage(3L, course, "courses/13/main.png", "https://cdn/main.png", ImageRole.MAIN, 0);
        course.getImages().add(courseImage);
        when(courseRepository.findDetailedById(13L)).thenReturn(Optional.of(course));
        User admin = createUser(40L, Role.ADMIN);
        authenticate(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(courseImageRepository.findById(3L)).thenReturn(Optional.of(courseImage));

        courseService.deleteCourseImage(13L, 3L);

        verify(courseImageRepository).delete(courseImage);
        assertThat(course.getImages()).isEmpty();
    }

    @Test
    void updateCourseUpdatesProvidedFields() {
        Course course = createCourseEntity(40L);
        Lyceum existingLyceum = new Lyceum();
        existingLyceum.setId(5L);
        existingLyceum.setLecturers(new ArrayList<>());
        course.setLyceum(existingLyceum);
        CourseSchedule newSchedule = new CourseSchedule();

        when(courseRepository.findDetailedById(40L)).thenReturn(Optional.of(course));
        User admin = createUser(70L, Role.ADMIN);
        authenticate(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        Lyceum newLyceum = new Lyceum();
        newLyceum.setId(11L);
        newLyceum.setLecturers(new ArrayList<>());
        when(lyceumRepository.findWithLecturersById(11L)).thenReturn(Optional.of(newLyceum));
        when(courseRepository.save(course)).thenAnswer(invocation -> invocation.getArgument(0));

        CourseUpdateRequest request = CourseUpdateRequest.builder()
                .name("Updated name")
                .description("Updated description")
                .type(CourseType.SPORT)
                .ageGroupList(List.of(AgeGroup.TEEN))
                .schedule(newSchedule)
                .address("  Some address  ")
                .price(150.5f)
                .facebookLink("fb-link")
                .websiteLink("site-link")
                .achievements("  Awards ")
                .lyceumId(11L)
                .build();

        CourseResponse response = courseService.updateCourse(40L, request);

        assertThat(response.getName()).isEqualTo("Updated name");
        assertThat(course.getDescription()).isEqualTo("Updated description");
        assertThat(course.getType()).isEqualTo(CourseType.SPORT);
        assertThat(course.getAgeGroupList()).containsExactly(AgeGroup.TEEN);
        assertThat(course.getSchedule()).isEqualTo(newSchedule);
        assertThat(course.getAddress()).isEqualTo("Some address");
        assertThat(course.getPrice()).isEqualTo(150.5f);
        assertThat(course.getFacebookLink()).isEqualTo("fb-link");
        assertThat(course.getWebsiteLink()).isEqualTo("site-link");
        assertThat(course.getAchievements()).isEqualTo("Awards");
        assertThat(course.getLyceum()).isEqualTo(newLyceum);
        verify(courseRepository).save(course);
    }

    @Test
    void updateCourseThrowsWhenRequestNull() {
        assertThrows(BadRequestException.class, () -> courseService.updateCourse(1L, null));
        verify(courseRepository, never()).findDetailedById(any());
    }

    @Test
    void updateCourseThrowsWhenNoFieldsProvided() {
        CourseUpdateRequest request = CourseUpdateRequest.builder().build();

        assertThrows(BadRequestException.class, () -> courseService.updateCourse(2L, request));
        verify(courseRepository, never()).findDetailedById(any());
    }

    @Test
    void updateCourseThrowsWhenNameBlank() {
        Course course = createCourseEntity(44L);
        when(courseRepository.findDetailedById(44L)).thenReturn(Optional.of(course));
        User admin = createUser(71L, Role.ADMIN);
        authenticate(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        CourseUpdateRequest request = CourseUpdateRequest.builder()
                .name("   ")
                .build();

        assertThrows(ValidationException.class, () -> courseService.updateCourse(44L, request));
        verify(courseRepository, never()).save(any());
    }

    @Test
    void updateCourseThrowsWhenAgeGroupsEmpty() {
        Course course = createCourseEntity(45L);
        when(courseRepository.findDetailedById(45L)).thenReturn(Optional.of(course));
        User admin = createUser(72L, Role.ADMIN);
        authenticate(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        CourseUpdateRequest request = CourseUpdateRequest.builder()
                .ageGroupList(List.of())
                .build();

        assertThrows(ValidationException.class, () -> courseService.updateCourse(45L, request));
        verify(courseRepository, never()).save(any());
    }

    @Test
    void updateCourseThrowsWhenLecturersNotFound() {
        Course course = createCourseEntity(46L);
        when(courseRepository.findDetailedById(46L)).thenReturn(Optional.of(course));
        User admin = createUser(73L, Role.ADMIN);
        authenticate(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.findAllById(any())).thenReturn(List.of());

        CourseUpdateRequest request = CourseUpdateRequest.builder()
                .lecturerIds(List.of(100L))
                .build();

        assertThrows(NoSuchElementException.class, () -> courseService.updateCourse(46L, request));
        verify(courseRepository, never()).save(any());
    }

    @Test
    void updateCoursePreventsLyceumChangeWhenUserLacksPermission() {
        Course course = createCourseEntity(47L);
        User lecturer = createUser(80L, Role.USER);
        course.setLecturers(new ArrayList<>(List.of(lecturer)));
        when(courseRepository.findDetailedById(47L)).thenReturn(Optional.of(course));
        authenticate(lecturer);
        when(userRepository.findById(lecturer.getId())).thenReturn(Optional.of(lecturer));

        Lyceum newLyceum = new Lyceum();
        newLyceum.setId(99L);
        newLyceum.setLecturers(new ArrayList<>());
        when(lyceumRepository.findWithLecturersById(99L)).thenReturn(Optional.of(newLyceum));

        CourseUpdateRequest request = CourseUpdateRequest.builder()
                .lyceumId(99L)
                .build();

        assertThrows(AccessDeniedException.class, () -> courseService.updateCourse(47L, request));
        verify(courseRepository, never()).save(any());
    }

    @Test
    void updateCourseSupportsIncrementalLecturerChanges() {
        Course course = createCourseEntity(41L);
        User existingLecturer = createUser(5L, Role.USER);
        course.setLecturers(new ArrayList<>(List.of(existingLecturer)));
        when(courseRepository.findDetailedById(41L)).thenReturn(Optional.of(course));

        User admin = createUser(71L, Role.ADMIN);
        authenticate(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        User newLecturer = createUser(9L, Role.USER);
        when(userRepository.findAllById(any())).thenReturn(List.of(newLecturer));
        when(courseRepository.save(course)).thenAnswer(invocation -> invocation.getArgument(0));

        CourseUpdateRequest request = CourseUpdateRequest.builder()
                .lecturerIdsToAdd(List.of(9L))
                .lecturerIdsToRemove(List.of(5L))
                .build();

        CourseResponse response = courseService.updateCourse(41L, request);

        assertThat(response.getLecturerIds()).containsExactly(9L);
        assertThat(course.getLecturers()).hasSize(1);
        assertThat(course.getLecturers().get(0).getId()).isEqualTo(9L);
        verify(userRepository).findAllById(any());
        verify(courseRepository).save(course);
    }

    @Test
    void deleteCourseImageThrowsWhenImageBelongsToDifferentCourse() {
        Course requested = createCourseEntity(14L);
        Course other = createCourseEntity(99L);
        CourseImage foreignImage = buildCourseImage(4L, other, "courses/99/main.png", "url", ImageRole.MAIN, 0);
        requested.getImages().add(new CourseImage());
        when(courseRepository.findDetailedById(14L)).thenReturn(Optional.of(requested));
        User admin = createUser(41L, Role.ADMIN);
        authenticate(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(courseImageRepository.findById(4L)).thenReturn(Optional.of(foreignImage));

        assertThrows(BadRequestException.class, () -> courseService.deleteCourseImage(14L, 4L));
        verify(courseImageRepository, never()).delete(any());
    }

    @Test
    void deleteCourseImageThrowsWhenUserCannotModifyCourse() {
        Course course = createCourseEntity(18L);
        when(courseRepository.findDetailedById(18L)).thenReturn(Optional.of(course));
        User user = createUser(50L, Role.USER);
        authenticate(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThrows(AccessDeniedException.class, () -> courseService.deleteCourseImage(18L, 2L));
        verify(courseImageRepository, never()).findById(any());
    }

    @Test
    void deleteCourseImageThrowsWhenImageCourseMissing() {
        Course course = createCourseEntity(19L);
        when(courseRepository.findDetailedById(19L)).thenReturn(Optional.of(course));
        User admin = createUser(51L, Role.ADMIN);
        authenticate(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        CourseImage orphan = new CourseImage();
        orphan.setId(900L);
        when(courseImageRepository.findById(900L)).thenReturn(Optional.of(orphan));

        assertThrows(BadRequestException.class, () -> courseService.deleteCourseImage(19L, 900L));
        verify(courseImageRepository, never()).delete(any());
    }

    @Test
    void deleteCourseImageThrowsWhenNotFound() {
        Course course = createCourseEntity(16L);
        when(courseRepository.findDetailedById(16L)).thenReturn(Optional.of(course));
        User admin = createUser(42L, Role.ADMIN);
        authenticate(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(courseImageRepository.findById(55L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> courseService.deleteCourseImage(16L, 55L));
        verify(courseImageRepository, never()).delete(any());
    }

    @Test
    void deleteCourseRemovesOnlyCourseEntity() {
        Course course = createCourseEntity(25L);
        Lyceum lyceum = new Lyceum();
        lyceum.setId(9L);
        course.setLyceum(lyceum);
        User lecturer = createUser(60L, Role.USER);
        course.getLecturers().add(lecturer);
        when(courseRepository.findDetailedById(25L)).thenReturn(Optional.of(course));
        User admin = createUser(70L, Role.ADMIN);
        authenticate(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        courseService.deleteCourse(25L);

        verify(courseRepository).delete(course);
        verify(lyceumRepository, never()).delete(any());
        verify(userRepository, never()).delete(any());
        assertThat(course.getLecturers()).containsExactly(lecturer);
        assertThat(course.getLyceum()).isEqualTo(lyceum);
    }

    @Test
    void deleteCourseThrowsWhenUserCannotModify() {
        Course course = createCourseEntity(26L);
        when(courseRepository.findDetailedById(26L)).thenReturn(Optional.of(course));
        User user = createUser(80L, Role.USER);
        authenticate(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThrows(AccessDeniedException.class, () -> courseService.deleteCourse(26L));
        verify(courseRepository, never()).delete(any());
    }

    @Test
    void deleteCourseThrowsWhenNotFound() {
        when(courseRepository.findDetailedById(31L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> courseService.deleteCourse(31L));
        verify(courseRepository, never()).delete(any());
    }

    @Test
    void getCourseImagesThrowsWhenCourseNotFound() {
        when(courseRepository.findById(200L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> courseService.getCourseImages(200L));
        verify(courseImageRepository, never()).findAllByCourseIdOrderByOrderIndexAscIdAsc(any());
    }

    private Course createCourseEntity(Long id) {
        Course course = new Course();
        course.setId(id);
        course.setName("Course " + id);
        course.setDescription("Description");
        course.setType(CourseType.MUSIC);
        course.setAgeGroupList(new ArrayList<>(List.of(AgeGroup.ADULT)));
        course.setImages(new ArrayList<>());
        course.setLecturers(new ArrayList<>());
        return course;
    }

    private CourseImage buildCourseImage(Long id, Course course, String key, String url, ImageRole role, Integer order) {
        CourseImage image = new CourseImage();
        image.setId(id);
        image.setCourse(course);
        image.setS3Key(key);
        image.setUrl(url);
        image.setRole(role);
        image.setOrderIndex(order);
        return image;
    }

    private void mockS3Properties(String prefix, String bucket, String publicBaseUrl) {
        lenient().when(s3Properties.getAllowedPrefix()).thenReturn(prefix);
        lenient().when(s3Properties.getBucketName()).thenReturn(bucket);
        lenient().when(s3Properties.getPublicBaseUrl()).thenReturn(publicBaseUrl);
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
