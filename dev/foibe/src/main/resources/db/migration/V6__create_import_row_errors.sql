CREATE TABLE import_row_errors (
    id            bigint       GENERATED ALWAYS AS IDENTITY,
    batch_id      bigint       NOT NULL,
    line_number   integer      NOT NULL,
    raw_line      varchar(1000),
    error_message varchar(500) NOT NULL,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_import_row_errors PRIMARY KEY (id),
    CONSTRAINT uq_import_row_errors_batch_line UNIQUE (batch_id, line_number),
    CONSTRAINT ck_import_row_errors_line_number CHECK (line_number > 0),
    CONSTRAINT fk_import_row_errors_batch_id
        FOREIGN KEY (batch_id) REFERENCES import_batches (id) ON DELETE CASCADE
);

CREATE INDEX idx_import_row_errors_batch_id ON import_row_errors (batch_id);
