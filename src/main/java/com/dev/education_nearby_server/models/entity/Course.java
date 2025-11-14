package com.dev.education_nearby_server.models.entity;

import com.dev.education_nearby_server.enums.AgeGroup;
import com.dev.education_nearby_server.enums.CourseType;
import com.dev.education_nearby_server.enums.ImageRole;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name="courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @NotNull(message = "Name should not be null!")
    private String name;

    @NotNull(message = "Description should not be null!")
    private String description;

    @NotNull(message = "Type should not be null!")
    private CourseType type;

    @NotNull(message = "Age group list should not be null!")
    private List<AgeGroup> ageGroupList;

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
    private List<User> lecturers;

    private String achievements;

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
