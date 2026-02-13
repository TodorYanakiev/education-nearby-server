package com.dev.education_nearby_server.repositories;

import com.dev.education_nearby_server.models.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    void deleteAllByUser_Id(Long userId);
}
