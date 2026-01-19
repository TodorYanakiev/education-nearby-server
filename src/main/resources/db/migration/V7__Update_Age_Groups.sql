DELETE FROM course_age_groups
WHERE age_group NOT IN ('TODDLER', 'CHILD', 'TEEN', 'ADULT', 'SENIOR');
