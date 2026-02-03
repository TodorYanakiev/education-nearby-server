ALTER TABLE courses
    ADD COLUMN active_start_month VARCHAR(20),
    ADD COLUMN active_end_month VARCHAR(20),
    ADD COLUMN execution_type VARCHAR(20);
