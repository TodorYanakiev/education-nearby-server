package com.dev.education_nearby_server.repositories;

import com.dev.education_nearby_server.enums.SubscriberExportScope;
import com.dev.education_nearby_server.models.entity.SubscriberExportJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriberExportJobRepository extends JpaRepository<SubscriberExportJob, Long> {
    Optional<SubscriberExportJob> findByIdAndScopeAndTargetId(Long id, SubscriberExportScope scope, Long targetId);
}

