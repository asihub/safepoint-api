--liquibase formatted sql

--changeset be:V1__baseline
CREATE TABLE IF NOT EXISTS anonymous_users (
    id         BIGSERIAL PRIMARY KEY,
    username   VARCHAR(255) NOT NULL,
    pin_hash   VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    CONSTRAINT uk_anonymous_users_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS safety_plans (
    id                     BIGSERIAL PRIMARY KEY,
    user_hash              VARCHAR(255) NOT NULL,
    warning_signs          TEXT,
    coping_strategies      TEXT,
    social_distractions    TEXT,
    trusted_contacts       TEXT,
    professional_resources TEXT,
    environment_safety     TEXT,
    created_at             TIMESTAMP,
    updated_at             TIMESTAMP,
    CONSTRAINT uk_safety_plans_user_hash UNIQUE (user_hash)
);

CREATE TABLE IF NOT EXISTS progress_backups (
    id             BIGSERIAL PRIMARY KEY,
    user_hash      VARCHAR(255) NOT NULL,
    encrypted_data TEXT NOT NULL,
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP,
    CONSTRAINT uk_progress_backups_user_hash UNIQUE (user_hash)
);

CREATE TABLE IF NOT EXISTS wellbeing_resources (
    id                 BIGSERIAL PRIMARY KEY,
    title              VARCHAR(255) NOT NULL,
    url                VARCHAR(255) NOT NULL,
    category           VARCHAR(255) NOT NULL,
    description        TEXT,
    excerpt            TEXT,
    excerpt_updated_at TIMESTAMP,
    created_at         TIMESTAMP,
    CONSTRAINT uk_wellbeing_resources_url UNIQUE (url)
);
