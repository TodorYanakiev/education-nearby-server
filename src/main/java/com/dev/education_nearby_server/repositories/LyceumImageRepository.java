package com.dev.education_nearby_server.repositories;

import com.dev.education_nearby_server.models.entity.LyceumImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LyceumImageRepository extends JpaRepository<LyceumImage, Long> {

    Optional<LyceumImage> findByS3Key(String s3Key);

    List<LyceumImage> findAllByLyceumIdOrderByOrderIndexAscIdAsc(Long lyceumId);
}
