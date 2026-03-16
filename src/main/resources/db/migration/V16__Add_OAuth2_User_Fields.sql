ALTER TABLE _users
    MODIFY password VARCHAR(255) NULL,
    ADD COLUMN auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN auth_provider_id VARCHAR(255),
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN registration_complete BOOLEAN NOT NULL DEFAULT TRUE;

CREATE UNIQUE INDEX uq_users_auth_provider_id ON _users (auth_provider, auth_provider_id);
