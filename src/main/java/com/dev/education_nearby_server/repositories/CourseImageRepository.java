package com.dev.education_nearby_server.repositories;

import com.dev.education_nearby_server.models.entity.CourseImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseImageRepository extends JpaRepository<CourseImage, Long> {

    Optional<CourseImage> findByS3Key(String s3Key);

    List<CourseImage> findAllByCourseIdOrderByOrderIndexAscIdAsc(Long courseId);
}
