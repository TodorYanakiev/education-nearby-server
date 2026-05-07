package com.dev.education_nearby_server.repositories;

import com.dev.education_nearby_server.models.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Page<Feedback> findAllByRead(boolean read, Pageable pageable);
}
