package com.dev.education_nearby_server.repositories;

import com.dev.education_nearby_server.models.entity.Course;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @EntityGraph(attributePaths = {"images", "lyceum"})
    Optional<Course> findDetailedById(Long id);
}
