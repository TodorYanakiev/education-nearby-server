CREATE TABLE feedback (
    id BIGINT NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message_text TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_feedback_created_at ON feedback (created_at);
CREATE INDEX idx_feedback_email ON feedback (email);
CREATE INDEX idx_feedback_is_read ON feedback (is_read);
