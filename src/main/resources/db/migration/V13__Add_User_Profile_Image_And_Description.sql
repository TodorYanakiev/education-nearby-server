ALTER TABLE _users
    ADD COLUMN description VARCHAR(500);

CREATE TABLE user_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    s3_key VARCHAR(512) NOT NULL,
    url VARCHAR(1024) NOT NULL,
    role VARCHAR(50) NOT NULL,
    alt_text VARCHAR(255),
    width INT,
    height INT,
    mime_type VARCHAR(100),
    order_index INT NOT NULL DEFAULT 0,
    user_id BIGINT NOT NULL,
    CONSTRAINT uq_user_images_s3_key UNIQUE (s3_key),
    CONSTRAINT uq_user_images_user UNIQUE (user_id),
    CONSTRAINT fk_user_images_user
        FOREIGN KEY (user_id)
        REFERENCES _users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_user_images_role ON user_images (role);
