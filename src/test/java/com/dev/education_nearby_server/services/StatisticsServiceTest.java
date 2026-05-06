package com.dev.education_nearby_server.services;

import com.dev.education_nearby_server.repositories.CourseRepository;
import com.dev.education_nearby_server.repositories.LyceumRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private LyceumRepository lyceumRepository;

    @InjectMocks
    private StatisticsService statisticsService;

    @Test
    void recordCoursesSeenInResultsSanitizesIds() {
        statisticsService.recordCoursesSeenInResults(Arrays.asList(1L, null, 1L, 2L));

        verify(courseRepository).incrementSeenInResultsCount(List.of(1L, 2L));
        verifyNoInteractions(lyceumRepository);
    }

    @Test
    void recordLyceumsSeenInResultsSkipsEmptyInput() {
        statisticsService.recordLyceumsSeenInResults(List.of());

        verifyNoInteractions(courseRepository, lyceumRepository);
    }

    @Test
    void recordCourseVisitIncrementsCourseCounter() {
        statisticsService.recordCourseVisit(5L);

        verify(courseRepository).incrementVisitCount(5L);
        verifyNoInteractions(lyceumRepository);
    }

    @Test
    void recordLyceumVisitSkipsNullInput() {
        statisticsService.recordLyceumVisit(null);

        verifyNoInteractions(courseRepository, lyceumRepository);
    }
}
