package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.config.S3Properties;
import com.dev.education_nearby_server.enums.ImageRole;
import com.dev.education_nearby_server.enums.Role;
import com.dev.education_nearby_server.exceptions.common.AccessDeniedException;
import com.dev.education_nearby_server.exceptions.common.BadRequestException;
import com.dev.education_nearby_server.exceptions.common.ConflictException;
import com.dev.education_nearby_server.exceptions.common.NoSuchElementException;
import com.dev.education_nearby_server.exceptions.common.UnauthorizedException;
import com.dev.education_nearby_server.exceptions.common.ValidationException;
import com.dev.education_nearby_server.models.dto.request.CourseImageRequest;
import com.dev.education_nearby_server.models.dto.request.CourseRequest;
import com.dev.education_nearby_server.models.dto.response.CourseImageResponse;
import com.dev.education_nearby_server.models.dto.response.CourseResponse;
import com.dev.education_nearby_server.models.entity.Course;
import com.dev.education_nearby_server.models.entity.CourseImage;
import com.dev.education_nearby_server.models.entity.Lyceum;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.repositories.CourseImageRepository;
import com.dev.education_nearby_server.repositories.CourseRepository;
import com.dev.education_nearby_server.repositories.LyceumRepository;
import com.dev.education_nearby_server.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseImageRepository courseImageRepository;
    private final LyceumRepository lyceumRepository;
    private final UserRepository userRepository;
    private final S3Properties s3Properties;
    private static final String NOT_FOUND = " not found.";

    @Transactional(readOnly = true)
    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourseById(Long courseId) {
        Course course = requireCourse(courseId, true);
        return mapToResponse(course);
    }

    public List<CourseImageResponse> getCourseImages(Long courseId) {
        Course course = requireCourse(courseId, false);
        List<CourseImage> images = courseImageRepository.findAllByCourseIdOrderByOrderIndexAscIdAsc(course.getId());
        return images.stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public CourseImageResponse addCourseImage(Long courseId, CourseImageRequest request) {
        Course course = requireCourse(courseId, true);
        ensureUserCanModifyCourse(course);

        String resolvedKey = resolveS3Key(request);
        String resolvedUrl = resolveUrl(request, resolvedKey);

        validateS3Location(resolvedUrl, resolvedKey);
        ensureUniqueKey(resolvedKey);
        ensureSingleRoleIfNeeded(course, request.getRole());

        CourseImage image = new CourseImage();
        image.setCourse(course);
        image.setS3Key(resolvedKey);
        image.setUrl(resolvedUrl);
        image.setRole(request.getRole());
        image.setAltText(trimToNull(request.getAltText()));
        image.setWidth(request.getWidth());
        image.setHeight(request.getHeight());
        image.setMimeType(trimToNull(request.getMimeType()));
        image.setOrderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0);

        course.getImages().add(image);
        CourseImage saved = courseImageRepository.save(image);

        return mapToResponse(saved);
    }

    @Transactional
    public void deleteCourseImage(Long courseId, Long imageId) {
        Course course = requireCourse(courseId, true);
        ensureUserCanModifyCourse(course);

        CourseImage image = courseImageRepository.findById(imageId)
                .orElseThrow(() -> new NoSuchElementException("Course image with id " + imageId + NOT_FOUND));
        if (image.getCourse() == null || !image.getCourse().getId().equals(course.getId())) {
            throw new BadRequestException("The provided image does not belong to this course.");
        }

        course.getImages().removeIf(existing -> existing.getId() != null && existing.getId().equals(imageId));
        courseImageRepository.delete(image);
    }

    @Transactional
    public CourseResponse createCourse(CourseRequest request) {
        if (request == null) {
            throw new BadRequestException("Course payload must not be null.");
        }
        User currentUser = getManagedCurrentUser();

        Lyceum lyceum = null;
        if (request.getLyceumId() != null) {
            lyceum = lyceumRepository.findWithLecturersById(request.getLyceumId())
                    .orElseThrow(() -> new NoSuchElementException("Lyceum with id " + request.getLyceumId() + NOT_FOUND));
        }

        ensureUserCanCreateCourse(currentUser, lyceum);

        Course course = new Course();
        course.setName(request.getName());
        course.setDescription(request.getDescription());
        course.setType(request.getType());
        course.setAgeGroupList(request.getAgeGroupList());
        course.setSchedule(request.getSchedule() != null ? request.getSchedule() : new com.dev.education_nearby_server.models.entity.CourseSchedule());
        course.setAddress(trimToNull(request.getAddress()));
        course.setPrice(request.getPrice());
        course.setFacebookLink(trimToNull(request.getFacebookLink()));
        course.setWebsiteLink(trimToNull(request.getWebsiteLink()));
        course.setAchievements(trimToNull(request.getAchievements()));
        course.setLyceum(lyceum);

        List<User> lecturers = resolveLecturers(request.getLecturerIds(), currentUser, lyceum);
        course.setLecturers(lecturers);

        Course saved = courseRepository.save(course);
        return mapToResponse(saved);
    }

    private void ensureSingleRoleIfNeeded(Course course, ImageRole role) {
        if (role == ImageRole.GALLERY) {
            return;
        }
        boolean alreadyExists = course.getImages().stream()
                .anyMatch(image -> image.getRole() == role);
        if (alreadyExists) {
            throw new ConflictException("Course already has an image with role " + role + ". Remove it before adding another.");
        }
    }

    private void ensureUniqueKey(String resolvedKey) {
        Optional<CourseImage> existing = courseImageRepository.findByS3Key(resolvedKey);
        if (existing.isPresent()) {
            throw new ConflictException("An image with the same S3 key is already registered.");
        }
    }

    private String resolveS3Key(CourseImageRequest request) {
        String explicitKey = trimToNull(request.getS3Key());
        String url = trimToNull(request.getUrl());

        if (StringUtils.hasText(explicitKey)) {
            return explicitKey;
        }
        if (StringUtils.hasText(url)) {
            return extractKeyFromUrl(url);
        }
        throw new ValidationException("Either url or s3Key must be provided.");
    }

    private String resolveUrl(CourseImageRequest request, String resolvedKey) {
        String url = trimToNull(request.getUrl());
        if (StringUtils.hasText(url)) {
            // Basic format validation
            parseUri(url);
            return url;
        }
        if (!StringUtils.hasText(resolvedKey)) {
            throw new ValidationException("Could not resolve an image URL without a valid S3 key.");
        }
        return buildUrlFromKey(resolvedKey);
    }

    private void validateS3Location(String url, String key) {
        if (!StringUtils.hasText(key)) {
            throw new ValidationException("S3 key cannot be empty.");
        }

        String prefix = trimToNull(s3Properties.getAllowedPrefix());
        if (StringUtils.hasText(prefix) && !key.startsWith(prefix)) {
            throw new ValidationException("S3 key must start with the configured prefix: " + prefix);
        }

        URI uri = parseUri(url);
        String bucketName = trimToNull(s3Properties.getBucketName());
        String normalizedPath = normalizePath(uri.getPath());

        if (!StringUtils.hasText(bucketName)) {
            if (StringUtils.hasText(normalizedPath) && !normalizedPath.equals(key)) {
                throw new ValidationException("Resolved S3 key does not match the key extracted from the URL.");
            }
            return;
        }

        String host = uri.getHost();
        String allowedHost = extractHost(trimToNull(s3Properties.getPublicBaseUrl()));
        boolean bucketInHost = host != null && host.contains(bucketName);
        boolean bucketInPath = pathStartsWithBucket(uri.getPath(), bucketName);
        boolean matchesAllowedHost = host != null && allowedHost != null && host.equalsIgnoreCase(allowedHost);

        if (!bucketInHost && !bucketInPath && !matchesAllowedHost) {
            throw new ValidationException("Image URL must point to bucket " + bucketName + ".");
        }

        String derivedKey = deriveKeyFromPath(uri.getPath(), bucketName);
        if (StringUtils.hasText(derivedKey) && !derivedKey.equals(key)) {
            throw new ValidationException("Resolved S3 key does not match the key extracted from the URL.");
        }
    }

    private String buildUrlFromKey(String key) {
        String baseUrl = trimToNull(s3Properties.getPublicBaseUrl());
        if (!StringUtils.hasText(baseUrl)) {
            String bucketName = trimToNull(s3Properties.getBucketName());
            if (!StringUtils.hasText(bucketName)) {
                throw new ValidationException("Provide a full image URL or configure app.s3.public-base-url.");
            }
            baseUrl = "https://" + bucketName + ".s3.amazonaws.com/";
        }
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        return baseUrl + key;
    }

    private String extractKeyFromUrl(String url) {
        URI uri = parseUri(url);
        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            throw new ValidationException("Image URL must contain an object path.");
        }
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        String bucketName = trimToNull(s3Properties.getBucketName());
        if (StringUtils.hasText(bucketName) && normalizedPath.startsWith(bucketName + "/")) {
            normalizedPath = normalizedPath.substring(bucketName.length() + 1);
        }
        if (!StringUtils.hasText(normalizedPath)) {
            throw new ValidationException("Could not extract an S3 key from the provided URL.");
        }
        return normalizedPath;
    }

    private URI parseUri(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new ValidationException("Provided URL is not valid.");
        }
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private boolean pathStartsWithBucket(String path, String bucketName) {
        return StringUtils.hasText(path) && StringUtils.hasText(bucketName) && path.startsWith("/" + bucketName + "/");
    }

    private String deriveKeyFromPath(String path, String bucketName) {
        String normalized = normalizePath(path);
        if (!StringUtils.hasText(normalized)) {
            return normalized;
        }
        if (StringUtils.hasText(bucketName) && normalized.startsWith(bucketName + "/")) {
            normalized = normalized.substring(bucketName.length() + 1);
        }
        return normalized;
    }

    private String extractHost(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        try {
            URI uri = new URI(url);
            return uri.getHost();
        } catch (URISyntaxException ignored) {
            return null;
        }
    }

    private Course requireCourse(Long courseId, boolean fetchDetails) {
        Optional<Course> courseOpt = fetchDetails
                ? courseRepository.findDetailedById(courseId)
                : courseRepository.findById(courseId);
        Course course = courseOpt.orElseThrow(() -> new NoSuchElementException("Course with id " + courseId + NOT_FOUND));
        if (course.getImages() == null) {
            course.setImages(new ArrayList<>());
        }
        return course;
    }

    private void ensureUserCanCreateCourse(User user, Lyceum lyceum) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (lyceum == null) {
            throw new AccessDeniedException("You do not have permission to create this course.");
        }
        Long administratedLyceumId = user.getAdministratedLyceum() == null ? null : user.getAdministratedLyceum().getId();
        if (administratedLyceumId != null && administratedLyceumId.equals(lyceum.getId())) {
            return;
        }
        boolean isLyceumLecturer = lyceum.getLecturers() != null
                && lyceum.getLecturers().stream()
                .anyMatch(lecturer -> lecturer.getId() != null && lecturer.getId().equals(user.getId()));
        if (isLyceumLecturer) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to create this course.");
    }

    private void ensureUserCanModifyCourse(Course course) {
        User user = getManagedCurrentUser();
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        Long administratedLyceumId = user.getAdministratedLyceum() == null ? null : user.getAdministratedLyceum().getId();
        boolean isLyceumAdministrator = course.getLyceum() != null
                && course.getLyceum().getId() != null
                && administratedLyceumId != null
                && administratedLyceumId.equals(course.getLyceum().getId());
        if (isLyceumAdministrator) {
            return;
        }

        boolean isLecturer = course.getLecturers() != null
                && course.getLecturers().stream()
                .anyMatch(lecturer -> lecturer.getId() != null && lecturer.getId().equals(user.getId()));
        if (isLecturer) {
            return;
        }

        throw new AccessDeniedException("You do not have permission to modify this course.");
    }

    private List<User> resolveLecturers(List<Long> lecturerIds, User creator, Lyceum lyceum) {
        Set<Long> lecturerIdSet = new LinkedHashSet<>();
        if (lecturerIds != null) {
            lecturerIds.stream()
                    //.filter(id -> id != null)
                    .filter(Objects::nonNull)
                    .forEach(lecturerIdSet::add);
        }
        boolean creatorIsLyceumLecturer = lyceum != null
                && lyceum.getLecturers() != null
                && lyceum.getLecturers().stream()
                .anyMatch(lecturer -> lecturer.getId() != null && lecturer.getId().equals(creator.getId()));
        if (creatorIsLyceumLecturer) {
            lecturerIdSet.add(creator.getId());
        }

        if (lecturerIdSet.isEmpty()) {
            return new ArrayList<>();
        }
        List<User> lecturers = userRepository.findAllById(lecturerIdSet);
        if (lecturers.size() != lecturerIdSet.size()) {
            throw new NoSuchElementException("One or more lecturers were" + NOT_FOUND);
        }
        return new ArrayList<>(lecturers);
    }

    private User getManagedCurrentUser() {
        User currentUser = getCurrentUser()
                .orElseThrow(() -> new UnauthorizedException("You must be authenticated to perform this action."));
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new UnauthorizedException("User" + NOT_FOUND));
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

    private CourseImageResponse mapToResponse(CourseImage image) {
        return CourseImageResponse.builder()
                .id(image.getId())
                .courseId(image.getCourse() != null ? image.getCourse().getId() : null)
                .s3Key(image.getS3Key())
                .url(image.getUrl())
                .role(image.getRole())
                .altText(image.getAltText())
                .width(image.getWidth())
                .height(image.getHeight())
                .mimeType(image.getMimeType())
                .orderIndex(image.getOrderIndex())
                .build();
    }

    private CourseResponse mapToResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .name(course.getName())
                .description(course.getDescription())
                .type(course.getType())
                .ageGroupList(course.getAgeGroupList())
                .schedule(course.getSchedule())
                .address(course.getAddress())
                .price(course.getPrice())
                .facebookLink(course.getFacebookLink())
                .websiteLink(course.getWebsiteLink())
                .lyceumId(course.getLyceum() != null ? course.getLyceum().getId() : null)
                .achievements(course.getAchievements())
                .lecturerIds(course.getLecturers() == null ? List.of() :
                        course.getLecturers().stream()
                                .map(User::getId)
                                .filter(Objects::nonNull)
                                .toList())
                .build();
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? null : value.trim();
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }
}
