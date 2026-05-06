package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.repositories.CourseRepository;
import com.dev.education_nearby_server.repositories.LyceumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Records lightweight aggregate counters without changing the read transaction that produced the event.
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final CourseRepository courseRepository;
    private final LyceumRepository lyceumRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordCoursesSeenInResults(Collection<Long> courseIds) {
        List<Long> ids = sanitizeIds(courseIds);
        if (ids.isEmpty()) {
            return;
        }
        courseRepository.incrementSeenInResultsCount(ids);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLyceumsSeenInResults(Collection<Long> lyceumIds) {
        List<Long> ids = sanitizeIds(lyceumIds);
        if (ids.isEmpty()) {
            return;
        }
        lyceumRepository.incrementSeenInResultsCount(ids);
    }

    private List<Long> sanitizeIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
