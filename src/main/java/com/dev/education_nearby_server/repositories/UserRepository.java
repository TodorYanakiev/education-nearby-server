package com.dev.education_nearby_server.repositories;

import com.dev.education_nearby_server.enums.AuthProvider;
import com.dev.education_nearby_server.models.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByAuthProviderAndAuthProviderId(AuthProvider authProvider, String authProviderId);
    List<User> findAllByAdministratedLyceum_Id(Long lyceumId);
    List<User> findDistinctBySubscribedCourses_IdOrderByIdAsc(Long courseId);
    List<User> findDistinctBySubscribedLyceums_IdOrderByIdAsc(Long lyceumId);
}
