CREATE TABLE app_users (
    id             bigint       GENERATED ALWAYS AS IDENTITY,
    church_id      bigint,
    username       varchar(50)  NOT NULL,
    password_hash  varchar(100) NOT NULL,
    full_name      varchar(120) NOT NULL,
    role           varchar(20)  NOT NULL,
    is_enabled     boolean      NOT NULL DEFAULT true,
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now(),
    created_by     bigint,
    updated_by     bigint,
    CONSTRAINT pk_app_users PRIMARY KEY (id),
    CONSTRAINT uq_app_users_username UNIQUE (username),
    CONSTRAINT ck_app_users_role CHECK (role IN ('OPERATOR', 'RESPONSIBLE', 'ADMIN')),
    CONSTRAINT ck_app_users_username CHECK (username ~ '^[A-Za-z0-9._-]{2,50}$'),
    CONSTRAINT fk_app_users_church_id FOREIGN KEY (church_id) REFERENCES churches (id),
    CONSTRAINT fk_app_users_created_by FOREIGN KEY (created_by) REFERENCES app_users (id),
    CONSTRAINT fk_app_users_updated_by FOREIGN KEY (updated_by) REFERENCES app_users (id)
);

ALTER TABLE churches
    ADD CONSTRAINT fk_churches_created_by FOREIGN KEY (created_by) REFERENCES app_users (id),
    ADD CONSTRAINT fk_churches_updated_by FOREIGN KEY (updated_by) REFERENCES app_users (id);
