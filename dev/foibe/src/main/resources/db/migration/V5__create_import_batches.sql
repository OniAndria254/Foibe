CREATE TABLE import_batches (
    id               bigint        GENERATED ALWAYS AS IDENTITY,
    church_id        bigint        NOT NULL,
    filename         varchar(255)  NOT NULL,
    source_text      varchar(500000) NOT NULL,
    status           varchar(20)   NOT NULL,
    total_rows       integer       NOT NULL DEFAULT 0,
    valid_rows       integer       NOT NULL DEFAULT 0,
    error_rows       integer       NOT NULL DEFAULT 0,
    assigned_from    integer,
    assigned_to      integer,
    ignored_columns  varchar(500),
    preview_members  text,
    started_by       bigint,
    started_at       timestamptz   NOT NULL DEFAULT now(),
    applied_at       timestamptz,
    created_at       timestamptz   NOT NULL DEFAULT now(),
    updated_at       timestamptz   NOT NULL DEFAULT now(),
    created_by       bigint,
    updated_by       bigint,
    CONSTRAINT pk_import_batches PRIMARY KEY (id),
    CONSTRAINT ck_import_batches_status CHECK (status IN ('PENDING', 'APPLIED', 'CANCELLED')),
    CONSTRAINT ck_import_batches_rows
        CHECK (total_rows >= 0 AND valid_rows >= 0 AND error_rows >= 0),
    CONSTRAINT ck_import_batches_assignment
        CHECK (
            (assigned_from IS NULL AND assigned_to IS NULL)
            OR (assigned_from IS NOT NULL AND assigned_to IS NOT NULL AND assigned_to >= assigned_from)
        ),
    CONSTRAINT fk_import_batches_church_id FOREIGN KEY (church_id) REFERENCES churches (id),
    CONSTRAINT fk_import_batches_started_by FOREIGN KEY (started_by) REFERENCES app_users (id),
    CONSTRAINT fk_import_batches_created_by FOREIGN KEY (created_by) REFERENCES app_users (id),
    CONSTRAINT fk_import_batches_updated_by FOREIGN KEY (updated_by) REFERENCES app_users (id)
);

CREATE INDEX idx_import_batches_church_status ON import_batches (church_id, status);
