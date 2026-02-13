package com.dev.education_nearby_server.repositories;

import com.dev.education_nearby_server.models.entity.UserImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserImageRepository extends JpaRepository<UserImage, Long> {
    Optional<UserImage> findByUserId(Long userId);
    Optional<UserImage> findByS3Key(String s3Key);
}
