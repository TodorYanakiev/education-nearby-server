CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rating INT NOT NULL,
    comment_text VARCHAR(500),
    user_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME DEFAULT NULL,
    CONSTRAINT fk_reviews_user
        FOREIGN KEY (user_id)
        REFERENCES _users(id)
);

CREATE INDEX idx_reviews_user ON reviews (user_id);
CREATE INDEX idx_reviews_rating ON reviews (rating);
CREATE UNIQUE INDEX uq_reviews_id_user ON reviews (id, user_id);

CREATE TABLE course_reviews (
    course_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    review_id BIGINT NOT NULL,
    PRIMARY KEY (course_id, user_id),
    CONSTRAINT fk_course_reviews_course
        FOREIGN KEY (course_id)
        REFERENCES courses(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_course_reviews_review
        FOREIGN KEY (review_id, user_id)
        REFERENCES reviews(id, user_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_course_reviews_review ON course_reviews (review_id);

CREATE TABLE lyceum_reviews (
    lyceum_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    review_id BIGINT NOT NULL,
    PRIMARY KEY (lyceum_id, user_id),
    CONSTRAINT fk_lyceum_reviews_lyceum
        FOREIGN KEY (lyceum_id)
        REFERENCES lyceums(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_lyceum_reviews_review
        FOREIGN KEY (review_id, user_id)
        REFERENCES reviews(id, user_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_lyceum_reviews_review ON lyceum_reviews (review_id);

CREATE TABLE user_reviews (
    reviewed_user_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    review_id BIGINT NOT NULL,
    PRIMARY KEY (reviewed_user_id, user_id),
    CONSTRAINT fk_user_reviews_reviewed_user
        FOREIGN KEY (reviewed_user_id)
        REFERENCES _users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_reviews_review
        FOREIGN KEY (review_id, user_id)
        REFERENCES reviews(id, user_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_user_reviews_review ON user_reviews (review_id);
