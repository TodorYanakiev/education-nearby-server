CREATE TABLE courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    address VARCHAR(255),
    price FLOAT(10, 2),
    facebook_link VARCHAR(512),
    website_link VARCHAR(512),
    lyceum_id BIGINT,
    achievements TEXT,
    CONSTRAINT fk_courses_lyceum
        FOREIGN KEY (lyceum_id)
        REFERENCES lyceums(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_courses_lyceum ON courses (lyceum_id);
CREATE INDEX idx_courses_type ON courses (type);

CREATE TABLE course_lecturer (
    course_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (course_id, user_id),
    CONSTRAINT fk_course_lecturer_course
        FOREIGN KEY (course_id)
        REFERENCES courses(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_course_lecturer_user
        FOREIGN KEY (user_id)
        REFERENCES _users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_course_lecturer_user ON course_lecturer (user_id);

CREATE TABLE course_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    s3_key VARCHAR(512) NOT NULL,
    url VARCHAR(1024) NOT NULL,
    role VARCHAR(50) NOT NULL,
    alt_text VARCHAR(255),
    width INT,
    height INT,
    mime_type VARCHAR(100),
    order_index INT NOT NULL DEFAULT 0,
    course_id BIGINT NOT NULL,
    CONSTRAINT uq_course_images_s3_key UNIQUE (s3_key),
    CONSTRAINT fk_course_images_course
        FOREIGN KEY (course_id)
        REFERENCES courses(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_course_images_course ON course_images (course_id);
CREATE INDEX idx_course_images_role ON course_images (role);

CREATE TABLE course_age_groups (
    course_id BIGINT NOT NULL,
    age_group_order INT NOT NULL,
    age_group VARCHAR(50) NOT NULL,
    PRIMARY KEY (course_id, age_group_order),
    CONSTRAINT uq_course_age_group UNIQUE (course_id, age_group),
    CONSTRAINT fk_course_age_groups_course
        FOREIGN KEY (course_id)
        REFERENCES courses(id)
        ON DELETE CASCADE
);

CREATE TABLE course_schedule_slots (
    course_id BIGINT NOT NULL,
    slot_order INT NOT NULL,
    recurrence VARCHAR(20) NOT NULL,
    day_of_week VARCHAR(20),
    day_of_month INT,
    start_time TIME,
    classes_count INT NOT NULL DEFAULT 1,
    single_class_duration_minutes INT,
    gap_between_classes_minutes INT,
    PRIMARY KEY (course_id, slot_order),
    CONSTRAINT fk_course_schedule_slots_course
        FOREIGN KEY (course_id)
        REFERENCES courses(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_course_schedule_slots_recurrence ON course_schedule_slots (recurrence);

CREATE TABLE course_schedule_special_cases (
    course_id BIGINT NOT NULL,
    special_case_order INT NOT NULL,
    `date` DATE NOT NULL,
    is_cancelled BOOLEAN NOT NULL DEFAULT TRUE,
    reason VARCHAR(512),
    PRIMARY KEY (course_id, special_case_order),
    CONSTRAINT fk_course_schedule_special_cases_course
        FOREIGN KEY (course_id)
        REFERENCES courses(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_course_schedule_special_cases_date ON course_schedule_special_cases (`date`);
