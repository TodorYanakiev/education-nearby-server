package com.dev.education_nearby_server.repositories;

import com.dev.education_nearby_server.models.entity.Lyceum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LyceumRepository  extends JpaRepository<Lyceum, Long> {
    Optional<Lyceum> findFirstByNameIgnoreCaseAndTownIgnoreCase(String name, String town);
}
