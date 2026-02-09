package com.dev.education_nearby_server.models.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CourseReviewId implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "user_id")
    private Long userId;
}
