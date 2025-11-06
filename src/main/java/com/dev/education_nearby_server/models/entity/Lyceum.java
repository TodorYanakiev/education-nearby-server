package com.dev.education_nearby_server.models.entity;

import com.dev.education_nearby_server.enums.VerificationStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name="lyceums")
public class Lyceum implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    private String name;

    private String chitalishtaUrl;

    private String status; // Active/closed

    @Column(unique = true)
    private String bulstat;

    private String chairman;

    private String secretary;

    private String phone;

    private String email;

    private String region;

    private String municipality;

    private String town;

    private String address;

    private String urlToLibrariesSite;

    private Integer registrationNumber;

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus = VerificationStatus.NOT_VERIFIED;

    @OneToMany(mappedBy = "administratedLyceum")
    private List<User> administrators = new ArrayList<>();
}
