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
 * Connects a review to a lyceum and its author.
 */
@Entity
@Table(name = "lyceum_reviews")
@Getter
@Setter
@NoArgsConstructor
public class LyceumReview implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private LyceumReviewId id = new LyceumReviewId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("lyceumId")
    @JoinColumn(name = "lyceum_id", nullable = false)
    private Lyceum lyceum;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;
}
