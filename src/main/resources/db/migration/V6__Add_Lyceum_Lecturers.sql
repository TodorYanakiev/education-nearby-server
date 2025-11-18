CREATE TABLE lyceum_lecturers (
    lyceum_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (lyceum_id, user_id),
    CONSTRAINT fk_lyceum_lecturers_lyceum
        FOREIGN KEY (lyceum_id)
        REFERENCES lyceums(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_lyceum_lecturers_user
        FOREIGN KEY (user_id)
        REFERENCES _users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_lyceum_lecturers_user ON lyceum_lecturers (user_id);
