package com.dev.education_nearby_server.repositories;

import com.dev.education_nearby_server.models.entity.LyceumLecturerInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LyceumLecturerInvitationRepository extends JpaRepository<LyceumLecturerInvitation, Long> {

    Optional<LyceumLecturerInvitation> findByLyceum_IdAndEmailIgnoreCase(Long lyceumId, String email);

    List<LyceumLecturerInvitation> findAllByEmailIgnoreCase(String email);
}
