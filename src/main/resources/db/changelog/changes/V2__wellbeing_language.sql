--liquibase formatted sql

--changeset be:V2__wellbeing_language
ALTER TABLE wellbeing_resources
    ADD COLUMN language VARCHAR(5) NOT NULL DEFAULT 'en';
