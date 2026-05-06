package com.dev.education_nearby_server.models.entity;

import com.dev.education_nearby_server.enums.ImageRole;
import com.dev.education_nearby_server.enums.VerificationStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cultural center entity tracked for verification, administration, and course assignments.
 */
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

    private Double longitude;

    private Double latitude;

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus = VerificationStatus.NOT_VERIFIED;

    @Column(name = "seen_in_results_count", nullable = false)
    private long seenInResultsCount;

    @OneToMany(mappedBy = "administratedLyceum")
    private List<User> administrators = new ArrayList<>();

    @OneToMany(mappedBy = "lyceum")
    private List<Course> courses = new ArrayList<>();

    @OneToMany(mappedBy = "lyceum", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LyceumReview> reviewLinks = new ArrayList<>();

    @OneToMany(mappedBy = "lyceum", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LyceumImage> images = new ArrayList<>();

    @Transient
    public List<Review> getReviews() {
        return reviewLinks.stream().map(LyceumReview::getReview).toList();
    }

    @JsonIgnore
    public Optional<LyceumImage> getMainImage() {
        return images.stream().filter(i -> i.getRole() == ImageRole.MAIN).findFirst();
    }

    @JsonIgnore
    public List<LyceumImage> getGalleryImages() {
        return images.stream().filter(i -> i.getRole() == ImageRole.GALLERY).toList();
    }

    @ManyToMany
    @JoinTable(
            name = "lyceum_lecturers",
            joinColumns = @JoinColumn(name = "lyceum_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> lecturers = new ArrayList<>();

    @ManyToMany(mappedBy = "subscribedLyceums")
    private List<User> subscribers = new ArrayList<>();
}
