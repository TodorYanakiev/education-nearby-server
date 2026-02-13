CREATE TABLE lyceum_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    s3_key VARCHAR(512) NOT NULL,
    url VARCHAR(1024) NOT NULL,
    role VARCHAR(50) NOT NULL,
    alt_text VARCHAR(255),
    width INT,
    height INT,
    mime_type VARCHAR(100),
    order_index INT NOT NULL DEFAULT 0,
    lyceum_id BIGINT NOT NULL,
    CONSTRAINT uq_lyceum_images_s3_key UNIQUE (s3_key),
    CONSTRAINT fk_lyceum_images_lyceum
        FOREIGN KEY (lyceum_id)
        REFERENCES lyceums(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_lyceum_images_lyceum ON lyceum_images (lyceum_id);
CREATE INDEX idx_lyceum_images_role ON lyceum_images (role);
