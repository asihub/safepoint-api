--liquibase formatted sql

--changeset be:V3__wellbeing_resource_status
ALTER TABLE wellbeing_resources
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE';

--rollback ALTER TABLE wellbeing_resources DROP COLUMN status;
