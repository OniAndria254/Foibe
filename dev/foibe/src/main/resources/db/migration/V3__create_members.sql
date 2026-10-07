CREATE TABLE members (
    id              bigint       GENERATED ALWAYS AS IDENTITY,
    church_id       bigint       NOT NULL,
    envelope_number integer      NOT NULL,
    last_name       varchar(100) NOT NULL,
    first_name      varchar(100) NOT NULL,
    sex             varchar(10),
    birth_date      date,
    phone           varchar(30),
    address         varchar(300),
    status          varchar(20)  NOT NULL DEFAULT 'ACTIVE',
    joined_on       date         NOT NULL DEFAULT CURRENT_DATE,
    notes           varchar(2000),
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now(),
    created_by      bigint,
    updated_by      bigint,
    CONSTRAINT pk_members PRIMARY KEY (id),
    CONSTRAINT uq_members_church_envelope UNIQUE (church_id, envelope_number),
    CONSTRAINT ck_members_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_members_sex CHECK (sex IS NULL OR sex IN ('MALE', 'FEMALE')),
    CONSTRAINT ck_members_envelope_number CHECK (envelope_number > 0),
    CONSTRAINT ck_members_last_name CHECK (length(trim(last_name)) > 0),
    CONSTRAINT ck_members_first_name CHECK (length(trim(first_name)) > 0),
    CONSTRAINT fk_members_church_id FOREIGN KEY (church_id) REFERENCES churches (id),
    CONSTRAINT fk_members_created_by FOREIGN KEY (created_by) REFERENCES app_users (id),
    CONSTRAINT fk_members_updated_by FOREIGN KEY (updated_by) REFERENCES app_users (id)
);

CREATE INDEX idx_members_church_last_name_first_name
    ON members (church_id, lower(last_name), lower(first_name));

CREATE INDEX idx_members_church_status ON members (church_id, status);
