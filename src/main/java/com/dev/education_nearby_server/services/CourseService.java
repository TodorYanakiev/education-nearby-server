package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.config.S3Properties;
import com.dev.education_nearby_server.enums.AgeGroup;
import com.dev.education_nearby_server.enums.CourseExecutionType;
import com.dev.education_nearby_server.enums.CourseType;
import com.dev.education_nearby_server.enums.ImageRole;
import com.dev.education_nearby_server.enums.Role;
import com.dev.education_nearby_server.exceptions.common.AccessDeniedException;
import com.dev.education_nearby_server.exceptions.common.BadRequestException;
import com.dev.education_nearby_server.exceptions.common.ConflictException;
import com.dev.education_nearby_server.exceptions.common.NoSuchElementException;
import com.dev.education_nearby_server.exceptions.common.UnauthorizedException;
import com.dev.education_nearby_server.exceptions.common.ValidationException;
import com.dev.education_nearby_server.models.dto.request.CourseFilterRequest;
import com.dev.education_nearby_server.models.dto.request.CourseImageRequest;
import com.dev.education_nearby_server.models.dto.request.CourseRequest;
import com.dev.education_nearby_server.models.dto.request.CourseUpdateRequest;
import com.dev.education_nearby_server.models.dto.response.CourseImageResponse;
import com.dev.education_nearby_server.models.dto.response.CourseResponse;
import com.dev.education_nearby_server.models.entity.Course;
import com.dev.education_nearby_server.models.entity.CourseImage;
import com.dev.education_nearby_server.models.entity.CourseSchedule;
import com.dev.education_nearby_server.models.entity.CourseScheduleSlot;
import com.dev.education_nearby_server.models.entity.Lyceum;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.repositories.CourseImageRepository;
import com.dev.education_nearby_server.repositories.CourseRepository;
import com.dev.education_nearby_server.repositories.LyceumRepository;
import com.dev.education_nearby_server.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Coordinates course lifecycle operations, including filtering, CRUD, lecturer management,
 * and course image validation/registration with S3 metadata checks.
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseImageRepository courseImageRepository;
    private final LyceumRepository lyceumRepository;
    private final UserRepository userRepository;
    private final S3Properties s3Properties;
    private static final String NOT_FOUND = " not found.";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "name", "price", "type");

    /**
     * Returns all courses without applying filters.
     *
     * @return list of all courses
     */
    @Transactional(readOnly = true)
    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Returns courses linked to the provided lyceum id.
     *
     * @param lyceumId lyceum identifier
     * @return courses for the lyceum
     */
    @Transactional(readOnly = true)
    public List<CourseResponse> getCoursesByLyceumId(Long lyceumId) {
        if (lyceumId == null) {
            throw new BadRequestException("Lyceum id must be provided.");
        }
        return courseRepository.findAllByLyceum_Id(lyceumId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Returns courses linked to the provided lecturer id.
     *
     * @param lecturerId lecturer identifier
     * @return courses for the lecturer
     */
    @Transactional(readOnly = true)
    public List<CourseResponse> getCoursesByLecturerId(Long lecturerId) {
        if (lecturerId == null) {
            throw new BadRequestException("Lecturer id must be provided.");
        }
        return courseRepository.findDistinctByLecturers_Id(lecturerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Filters courses by optional type, age group, price, recurrence, day, and time ranges.
     * Empty or null lists are treated as no filter; invalid ranges are rejected.
     *
     * @param filterRequest filter criteria; null values are ignored
     * @param page zero-based page index
     * @param size page size
     * @param sort sorting configuration
     * @return courses that satisfy the provided filters
     */
    @Transactional(readOnly = true)
    public Page<CourseResponse> filterCourses(CourseFilterRequest filterRequest, Integer page, Integer size, Sort sort) {
        CourseFilterRequest filters = filterRequest != null ? filterRequest : new CourseFilterRequest();
        validatePageRequest(page, size);

        Float minPrice = filters.getMinPrice();
        Float maxPrice = filters.getMaxPrice();
        validatePriceRange(minPrice, maxPrice);
        validateStartTimeRange(filters.getStartTimeFrom(), filters.getStartTimeTo());
        Month activeStartMonth = filters.getActiveStartMonth();
        Month activeEndMonth = filters.getActiveEndMonth();
        validateActivePeriod(activeStartMonth, activeEndMonth);

        List<CourseType> courseTypes = sanitizeList(filters.getCourseTypes());
        List<AgeGroup> ageGroups = sanitizeList(filters.getAgeGroups());
        boolean applyCourseTypeFilter = courseTypes != null;
        boolean applyAgeGroupFilter = ageGroups != null;
        boolean applyActivePeriodFilter = activeStartMonth != null && activeEndMonth != null;
        Integer activeStartMonthValue = activeStartMonth != null ? activeStartMonth.getValue() : null;
        Integer activeEndMonthValue = activeEndMonth != null ? activeEndMonth.getValue() : null;

        Sort resolvedSort = resolveSort(sort);
        Pageable pageable = PageRequest.of(page, size, resolvedSort);
        Page<Course> courses = courseRepository.filterCourses(
                defaultList(courseTypes),
                applyCourseTypeFilter,
                defaultList(ageGroups),
                applyAgeGroupFilter,
                minPrice,
                maxPrice,
                filters.getRecurrence(),
                filters.getDayOfWeek(),
                filters.getStartTimeFrom(),
                filters.getStartTimeTo(),
                activeStartMonthValue,
                activeEndMonthValue,
                applyActivePeriodFilter,
                pageable
        );
        return courses.map(this::mapToResponse);
    }

    /**
     * Returns course details by id.
     *
     * @param courseId course identifier
     * @return course response
     */
    @Transactional(readOnly = true)
    public CourseResponse getCourseById(Long courseId) {
        Course course = requireCourse(courseId, true);
        return mapToResponse(course);
    }

    /**
     * Lists images for a given course id.
     *
     * @param courseId course identifier
     * @return images sorted by order and id
     */
    public List<CourseImageResponse> getCourseImages(Long courseId) {
        Course course = requireCourse(courseId, false);
        List<CourseImage> images = courseImageRepository.findAllByCourseIdOrderByOrderIndexAscIdAsc(course.getId());
        return images.stream().map(this::mapToResponse).toList();
    }

    /**
     * Adds a course image after validating S3 key/url consistency, uniqueness, and role constraints,
     * then returns the persisted image metadata.
     */
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

    /**
     * Removes a course image after verifying course ownership and association.
     *
     * @param courseId course identifier
     * @param imageId image identifier to delete
     */
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

    /**
     * Deletes a course after ensuring the caller has rights to modify it.
     *
     * @param courseId course identifier
     */
    @Transactional
    public void deleteCourse(Long courseId) {
        Course course = requireCourse(courseId, true);
        ensureUserCanModifyCourse(course);

        // Remove only the course entity itself; lyceums and lecturers stay untouched.
        courseRepository.delete(course);
    }

    /**
     * Creates a new course, validating permissions and linking lecturers and lyceum when provided.
     *
     * @param request course creation payload
     * @return persisted course response
     */
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
        course.setExecutionType(request.getExecutionType());
        course.setAgeGroupList(request.getAgeGroupList());
        CourseSchedule schedule = request.getSchedule() != null ? request.getSchedule() : new CourseSchedule();
        validateSchedule(schedule);
        course.setSchedule(schedule);
        validateActivePeriod(request.getActiveStartMonth(), request.getActiveEndMonth());
        course.setActiveStartMonth(request.getActiveStartMonth());
        course.setActiveEndMonth(request.getActiveEndMonth());
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

    /**
     * Applies partial updates to a course, enforcing authorization and validating
     * provided fields before persisting.
     */
    @Transactional
    public CourseResponse updateCourse(Long courseId, CourseUpdateRequest request) {
        CourseUpdateRequest validatedRequest = requireValidCourseUpdateRequest(request);

        Course course = requireCourse(courseId, true);
        User currentUser = getManagedCurrentUser();
        ensureUserCanModifyCourse(currentUser, course);

        updateCourseFields(course, validatedRequest);
        updateCourseLyceum(course, currentUser, validatedRequest);
        applyLecturerUpdates(course, currentUser, validatedRequest);

        Course saved = courseRepository.save(course);
        return mapToResponse(saved);
    }

    /**
     * Adds a lecturer to a course, ensuring permissions and avoiding duplicates.
     *
     * @param courseId course identifier
     * @param userId lecturer identifier
     */
    @Transactional
    public void addLecturerToCourse(Long courseId, Long userId) {
        if (userId == null) {
            throw new BadRequestException("User id must be provided.");
        }
        Course course = requireCourse(courseId, true);
        ensureUserCanModifyCourse(course);

        User lecturer = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User with id " + userId + NOT_FOUND));

        if (course.getLecturers() == null) {
            course.setLecturers(new ArrayList<>());
        }

        boolean alreadyLecturer = course.getLecturers().stream()
                .anyMatch(existing -> existing.getId() != null && existing.getId().equals(lecturer.getId()));
        if (!alreadyLecturer) {
            course.getLecturers().add(lecturer);
        }

        courseRepository.save(course);
    }

    private CourseUpdateRequest requireValidCourseUpdateRequest(CourseUpdateRequest request) {
        if (request == null) {
            throw new BadRequestException("Course payload must not be null.");
        }
        if (!hasUpdates(request)) {
            throw new BadRequestException("At least one field must be provided for update.");
        }
        return request;
    }

    private void updateCourseFields(Course course, CourseUpdateRequest request) {
        updateName(course, request);
        updateDescription(course, request);
        updateCourseType(course, request);
        updateExecutionType(course, request);
        updateAgeGroups(course, request);
        updateSchedule(course, request);
        updateActivePeriod(course, request);
        updateLocationAndLinks(course, request);
    }

    private void updateName(Course course, CourseUpdateRequest request) {
        if (request.getName() == null) {
            return;
        }

        String trimmedName = trimToNull(request.getName());
        if (trimmedName == null) {
            throw new ValidationException("Course name must not be blank.");
        }
        course.setName(trimmedName);
    }

    private void updateDescription(Course course, CourseUpdateRequest request) {
        if (request.getDescription() == null) {
            return;
        }

        String trimmedDescription = trimToNull(request.getDescription());
        if (trimmedDescription == null) {
            throw new ValidationException("Course description must not be blank.");
        }
        course.setDescription(trimmedDescription);
    }

    private void updateCourseType(Course course, CourseUpdateRequest request) {
        if (request.getType() != null) {
            course.setType(request.getType());
        }
    }

    private void updateExecutionType(Course course, CourseUpdateRequest request) {
        CourseExecutionType executionType = request.getExecutionType();
        if (executionType != null) {
            course.setExecutionType(executionType);
        }
    }

    private void updateAgeGroups(Course course, CourseUpdateRequest request) {
        List<AgeGroup> ageGroups = request.getAgeGroupList();
        if (ageGroups == null) {
            return;
        }
        if (ageGroups.isEmpty()) {
            throw new ValidationException("At least one age group must be specified.");
        }
        course.setAgeGroupList(new ArrayList<>(ageGroups));
    }

    private void updateSchedule(Course course, CourseUpdateRequest request) {
        if (request.getSchedule() != null) {
            validateSchedule(request.getSchedule());
            course.setSchedule(request.getSchedule());
        }
    }

    private void updateActivePeriod(Course course, CourseUpdateRequest request) {
        Month startMonth = request.getActiveStartMonth();
        Month endMonth = request.getActiveEndMonth();
        if (startMonth == null && endMonth == null) {
            return;
        }
        validateActivePeriod(startMonth, endMonth);
        course.setActiveStartMonth(startMonth);
        course.setActiveEndMonth(endMonth);
    }

    private void updateLocationAndLinks(Course course, CourseUpdateRequest request) {
        if (request.getAddress() != null) {
            course.setAddress(trimToNull(request.getAddress()));
        }
        if (request.getPrice() != null) {
            course.setPrice(request.getPrice());
        }
        if (request.getFacebookLink() != null) {
            course.setFacebookLink(trimToNull(request.getFacebookLink()));
        }
        if (request.getWebsiteLink() != null) {
            course.setWebsiteLink(trimToNull(request.getWebsiteLink()));
        }
        if (request.getAchievements() != null) {
            course.setAchievements(trimToNull(request.getAchievements()));
        }
    }

    private void updateCourseLyceum(Course course, User currentUser, CourseUpdateRequest request) {
        Long lyceumId = request.getLyceumId();
        if (lyceumId == null) {
            return;
        }

        Lyceum currentLyceum = course.getLyceum();
        boolean sameLyceum = currentLyceum != null && currentLyceum.getId() != null && currentLyceum.getId().equals(lyceumId);
        if (!sameLyceum) {
            Lyceum newLyceum = lyceumRepository.findWithLecturersById(lyceumId)
                    .orElseThrow(() -> new NoSuchElementException("Lyceum with id " + lyceumId + NOT_FOUND));
            ensureUserCanCreateCourse(currentUser, newLyceum);
            course.setLyceum(newLyceum);
        }
    }

    private boolean hasUpdates(CourseUpdateRequest request) {
        return request.getName() != null
                || request.getDescription() != null
                || request.getType() != null
                || request.getExecutionType() != null
                || request.getAgeGroupList() != null
                || request.getSchedule() != null
                || request.getAddress() != null
                || request.getPrice() != null
                || request.getFacebookLink() != null
                || request.getWebsiteLink() != null
                || request.getLyceumId() != null
                || request.getAchievements() != null
                || request.getActiveStartMonth() != null
                || request.getActiveEndMonth() != null
                || request.getLecturerIds() != null
                || request.getLecturerIdsToAdd() != null
                || request.getLecturerIdsToRemove() != null;
    }

    private void applyLecturerUpdates(Course course, User currentUser, CourseUpdateRequest request) {
        if (course.getLecturers() == null) {
            course.setLecturers(new ArrayList<>());
        }
        if (request.getLecturerIds() != null) {
            List<User> lecturers = resolveLecturers(request.getLecturerIds(), currentUser, course.getLyceum());
            course.setLecturers(lecturers);
            return;
        }

        List<Long> idsToRemove = request.getLecturerIdsToRemove();
        if (idsToRemove != null) {
            Set<Long> toRemove = collectLecturerIds(idsToRemove);
            if (!toRemove.isEmpty()) {
                course.getLecturers().removeIf(lecturer -> lecturer.getId() != null && toRemove.contains(lecturer.getId()));
            }
        }

        List<Long> idsToAdd = request.getLecturerIdsToAdd();
        if (idsToAdd != null) {
            List<User> newLecturers = loadLecturers(collectLecturerIds(idsToAdd));
            for (User lecturer : newLecturers) {
                boolean alreadyPresent = course.getLecturers().stream()
                        .anyMatch(existing -> existing.getId() != null && existing.getId().equals(lecturer.getId()));
                if (!alreadyPresent) {
                    course.getLecturers().add(lecturer);
                }
            }
        }
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
        ensureUserCanModifyCourse(user, course);
    }

    private void ensureUserCanModifyCourse(User user, Course course) {
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

    private LinkedHashSet<Long> collectLecturerIds(List<Long> lecturerIds) {
        LinkedHashSet<Long> lecturerIdSet = new LinkedHashSet<>();
        if (lecturerIds != null) {
            lecturerIds.stream()
                    .filter(Objects::nonNull)
                    .forEach(lecturerIdSet::add);
        }
        return lecturerIdSet;
    }

    private List<User> loadLecturers(LinkedHashSet<Long> lecturerIdSet) {
        if (lecturerIdSet.isEmpty()) {
            return new ArrayList<>();
        }
        List<User> lecturers = userRepository.findAllById(lecturerIdSet);
        if (lecturers.size() != lecturerIdSet.size()) {
            throw new NoSuchElementException("One or more lecturers were" + NOT_FOUND);
        }
        Map<Long, User> userById = new HashMap<>();
        for (User lecturer : lecturers) {
            if (lecturer.getId() != null) {
                userById.put(lecturer.getId(), lecturer);
            }
        }
        List<User> ordered = new ArrayList<>(lecturerIdSet.size());
        for (Long id : lecturerIdSet) {
            User lecturer = userById.get(id);
            if (lecturer == null) {
                throw new NoSuchElementException("One or more lecturers were" + NOT_FOUND);
            }
            ordered.add(lecturer);
        }
        return ordered;
    }

    private List<User> resolveLecturers(List<Long> lecturerIds, User creator, Lyceum lyceum) {
        LinkedHashSet<Long> lecturerIdSet = collectLecturerIds(lecturerIds);
        boolean creatorIsLyceumLecturer = lyceum != null
                && lyceum.getLecturers() != null
                && lyceum.getLecturers().stream()
                .anyMatch(lecturer -> lecturer.getId() != null && lecturer.getId().equals(creator.getId()));
        if (creatorIsLyceumLecturer) {
            lecturerIdSet.add(creator.getId());
        }

        return loadLecturers(lecturerIdSet);
    }

    private <T> List<T> sanitizeList(List<T> values) {
        if (values == null) {
            return null;
        }
        List<T> sanitized = values.stream()
                .filter(Objects::nonNull)
                .toList();
        return sanitized.isEmpty() ? null : sanitized;
    }

    private <T> List<T> defaultList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private void validatePriceRange(Float minPrice, Float maxPrice) {
        if (minPrice != null && minPrice < 0) {
            throw new BadRequestException("Minimum price must be zero or positive.");
        }
        if (maxPrice != null && maxPrice < 0) {
            throw new BadRequestException("Maximum price must be zero or positive.");
        }
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new BadRequestException("Minimum price cannot be greater than maximum price.");
        }
    }

    private void validateStartTimeRange(LocalTime startTimeFrom, LocalTime startTimeTo) {
        if (startTimeFrom != null && startTimeTo != null && startTimeTo.isBefore(startTimeFrom)) {
            throw new BadRequestException("startTimeTo must be after or equal to startTimeFrom.");
        }
    }

    private void validatePageRequest(Integer page, Integer size) {
        if (page == null || page < 0) {
            throw new BadRequestException("Page index must be zero or positive.");
        }
        if (size == null || size <= 0) {
            throw new BadRequestException("Page size must be greater than zero.");
        }
    }

    private Sort resolveSort(Sort sort) {
        Sort resolved = (sort == null || sort.isUnsorted()) ? Sort.by("id") : sort;
        for (Sort.Order order : resolved) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException("Sorting by '" + order.getProperty() + "' is not supported.");
            }
        }
        return resolved;
    }

    private void validateSchedule(CourseSchedule schedule) {
        if (schedule == null || schedule.getSlots() == null) {
            return;
        }
        List<CourseScheduleSlot> slots = schedule.getSlots();
        for (int index = 0; index < slots.size(); index++) {
            validateScheduleSlot(slots.get(index), index);
        }
    }

    private void validateScheduleSlot(CourseScheduleSlot slot, int index) {
        if (slot == null || slot.getEndTime() == null) {
            return;
        }

        LocalTime startTime = slot.getStartTime();
        LocalTime endTime = slot.getEndTime();
        int displayIndex = index + 1;
        if (startTime == null) {
            throw new ValidationException("Schedule slot #" + displayIndex + " must include startTime when endTime is provided.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new ValidationException("Schedule slot #" + displayIndex + " endTime must be after startTime.");
        }

        Integer singleClassDuration = slot.getSingleClassDurationMinutes();
        if (singleClassDuration == null) {
            return;
        }

        Integer classesCount = slot.getClassesCount() != null ? slot.getClassesCount() : 1;
        if (classesCount < 1) {
            throw new ValidationException("Schedule slot #" + displayIndex + " classesCount must be at least 1.");
        }
        if (singleClassDuration <= 0) {
            throw new ValidationException("Schedule slot #" + displayIndex + " singleClassDurationMinutes must be positive.");
        }
        Integer gapBetweenClasses = slot.getGapBetweenClassesMinutes() != null ? slot.getGapBetweenClassesMinutes() : 0;
        if (gapBetweenClasses < 0) {
            throw new ValidationException("Schedule slot #" + displayIndex + " gapBetweenClassesMinutes must be zero or positive.");
        }
    }

    private void validateActivePeriod(Month startMonth, Month endMonth) {
        if (startMonth == null && endMonth == null) {
            return;
        }
        if (startMonth == null || endMonth == null) {
            throw new ValidationException("Active period requires both start and end months.");
        }
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
        List<CourseImageResponse> images = mapCourseImages(course);
        return CourseResponse.builder()
                .id(course.getId())
                .name(course.getName())
                .description(course.getDescription())
                .type(course.getType())
                .executionType(course.getExecutionType())
                .ageGroupList(course.getAgeGroupList())
                .schedule(course.getSchedule())
                .images(images)
                .address(course.getAddress())
                .price(course.getPrice())
                .facebookLink(course.getFacebookLink())
                .websiteLink(course.getWebsiteLink())
                .lyceumId(course.getLyceum() != null ? course.getLyceum().getId() : null)
                .achievements(course.getAchievements())
                .activeStartMonth(course.getActiveStartMonth())
                .activeEndMonth(course.getActiveEndMonth())
                .lecturerIds(course.getLecturers() == null ? List.of() :
                        course.getLecturers().stream()
                                .map(User::getId)
                                .filter(Objects::nonNull)
                                .toList())
                .build();
    }

    private List<CourseImageResponse> mapCourseImages(Course course) {
        if (course.getImages() == null || course.getImages().isEmpty()) {
            return List.of();
        }
        Comparator<CourseImage> ordering = Comparator
                .comparing(CourseImage::getOrderIndex, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(CourseImage::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        return course.getImages().stream()
                .sorted(ordering)
                .map(this::mapToResponse)
                .toList();
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? null : value.trim();
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }
}
