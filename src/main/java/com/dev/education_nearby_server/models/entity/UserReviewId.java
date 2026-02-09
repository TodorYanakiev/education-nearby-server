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
public class UserReviewId implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "reviewed_user_id")
    private Long reviewedUserId;

    @Column(name = "user_id")
    private Long userId;
}
