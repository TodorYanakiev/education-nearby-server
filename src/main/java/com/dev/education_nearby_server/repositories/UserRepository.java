package com.dev.education_nearby_server.repositories;

import com.dev.education_nearby_server.models.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    List<User> findAllByAdministratedLyceum_Id(Long lyceumId);
}
