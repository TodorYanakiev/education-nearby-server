CREATE TABLE subscriber_export_jobs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    export_scope VARCHAR(16) NOT NULL,
    target_id BIGINT NOT NULL,
    export_format VARCHAR(16) NOT NULL,
    export_status VARCHAR(24) NOT NULL,
    requested_by_user_id BIGINT NOT NULL,
    file_name VARCHAR(255),
    file_path VARCHAR(1024),
    content_type VARCHAR(255),
    error_message VARCHAR(1024),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    completed_at DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_subscriber_export_jobs_user
        FOREIGN KEY (requested_by_user_id)
        REFERENCES _users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_subscriber_export_jobs_scope_target ON subscriber_export_jobs (export_scope, target_id);
CREATE INDEX idx_subscriber_export_jobs_status ON subscriber_export_jobs (export_status);
CREATE INDEX idx_subscriber_export_jobs_requested_by ON subscriber_export_jobs (requested_by_user_id);
