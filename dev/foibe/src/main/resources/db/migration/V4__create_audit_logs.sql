CREATE TABLE audit_logs (
    id           bigint       GENERATED ALWAYS AS IDENTITY,
    church_id    bigint,
    entity_type  varchar(60)  NOT NULL,
    entity_id    bigint       NOT NULL,
    action       varchar(30)  NOT NULL,
    old_values   jsonb,
    new_values   jsonb,
    changed_by   bigint,
    changed_at   timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_audit_logs PRIMARY KEY (id),
    CONSTRAINT ck_audit_logs_action
        CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'STATUS_CHANGE', 'IMPORT')),
    CONSTRAINT ck_audit_logs_entity_type CHECK (length(trim(entity_type)) > 0),
    CONSTRAINT fk_audit_logs_church_id FOREIGN KEY (church_id) REFERENCES churches (id),
    CONSTRAINT fk_audit_logs_changed_by FOREIGN KEY (changed_by) REFERENCES app_users (id)
);

CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_logs_changed_at ON audit_logs (changed_at DESC);
