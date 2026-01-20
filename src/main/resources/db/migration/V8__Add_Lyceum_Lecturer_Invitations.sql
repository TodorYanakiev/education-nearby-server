CREATE TABLE lyceum_lecturer_invitations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lyceum_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lyceum_lecturer_invites_lyceum
        FOREIGN KEY (lyceum_id)
        REFERENCES lyceums(id)
        ON DELETE CASCADE,
    CONSTRAINT uk_lyceum_lecturer_invite_email
        UNIQUE (lyceum_id, email)
);

CREATE INDEX idx_lyceum_lecturer_invites_email
    ON lyceum_lecturer_invitations (email);
