CREATE TABLE token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    value VARCHAR(255) NOT NULL UNIQUE,
    token_type VARCHAR(50) NOT NULL DEFAULT 'BEARER',
    revoked BOOLEAN NOT NULL,
    expired BOOLEAN NOT NULL,
    user_id BIGINT,
    CONSTRAINT fk_token_user FOREIGN KEY (user_id) REFERENCES _users(id)
);
