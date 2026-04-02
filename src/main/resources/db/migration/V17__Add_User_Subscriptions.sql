CREATE TABLE user_course_subscriptions (
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, course_id),
    CONSTRAINT fk_user_course_subscriptions_user
        FOREIGN KEY (user_id)
        REFERENCES _users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_course_subscriptions_course
        FOREIGN KEY (course_id)
        REFERENCES courses(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_user_course_subscriptions_course ON user_course_subscriptions (course_id);

CREATE TABLE user_lyceum_subscriptions (
    user_id BIGINT NOT NULL,
    lyceum_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, lyceum_id),
    CONSTRAINT fk_user_lyceum_subscriptions_user
        FOREIGN KEY (user_id)
        REFERENCES _users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_lyceum_subscriptions_lyceum
        FOREIGN KEY (lyceum_id)
        REFERENCES lyceums(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_user_lyceum_subscriptions_lyceum ON user_lyceum_subscriptions (lyceum_id);
