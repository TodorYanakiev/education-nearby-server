package com.dev.education_nearby_server.models.entity;

import com.dev.education_nearby_server.enums.AgeGroup;
import com.dev.education_nearby_server.enums.CourseType;
import com.dev.education_nearby_server.enums.ImageRole;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents an educational course with schedule, pricing, media, and lecturer associations.
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name="courses")
public class Course implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @NotNull(message = "Name should not be null!")
    private String name;

    @NotNull(message = "Description should not be null!")
    private String description;

    @NotNull(message = "Type should not be null!")
    @Enumerated(EnumType.STRING)
    private CourseType type;

    @NotNull(message = "Age group list should not be null!")
    @ElementCollection
    @CollectionTable(name = "course_age_groups", joinColumns = @JoinColumn(name = "course_id"))
    @Column(name = "age_group", nullable = false)
    @Enumerated(EnumType.STRING)
    @OrderColumn(name = "age_group_order")
    private List<AgeGroup> ageGroupList = new ArrayList<>();

    @Embedded
    private CourseSchedule schedule = new CourseSchedule();

    private String address;

    private Float price;

    private String facebookLink;

    private String websiteLink;

    @ManyToOne
    @JoinColumn(name = "lyceum_id")
    private Lyceum lyceum;

    @ManyToMany
    @JoinTable(
            name = "course_lecturer",
            joinColumns = @JoinColumn(name =  "course_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> lecturers = new ArrayList<>();

    private String achievements;

    @Enumerated(EnumType.STRING)
    @Column(name = "active_start_month")
    private Month activeStartMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "active_end_month")
    private Month activeEndMonth;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseImage> images = new ArrayList<>();

    @JsonIgnore
    public Optional<CourseImage> getLogoImage() {
        return images.stream().filter(i -> i.getRole() == ImageRole.LOGO).findFirst();
    }

    @JsonIgnore
    public Optional<CourseImage> getMainImage() {
        return images.stream().filter(i -> i.getRole() == ImageRole.MAIN).findFirst();
    }

    @JsonIgnore
    public List<CourseImage> getGalleryImages() {
        return images.stream().filter(i -> i.getRole() == ImageRole.GALLERY).toList();
    }
}
