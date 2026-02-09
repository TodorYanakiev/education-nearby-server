package com.dev.education_nearby_server.models.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * Connects a review to a reviewed user and its author.
 */
@Entity
@Table(name = "user_reviews")
@Getter
@Setter
@NoArgsConstructor
public class UserReview implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private UserReviewId id = new UserReviewId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("reviewedUserId")
    @JoinColumn(name = "reviewed_user_id", nullable = false)
    private User reviewedUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;
}
