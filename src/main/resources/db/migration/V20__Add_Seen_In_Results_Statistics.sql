ALTER TABLE courses
    ADD COLUMN seen_in_results_count BIGINT NOT NULL DEFAULT 0;

ALTER TABLE lyceums
    ADD COLUMN seen_in_results_count BIGINT NOT NULL DEFAULT 0;
