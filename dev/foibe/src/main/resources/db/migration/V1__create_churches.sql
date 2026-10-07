CREATE TABLE churches (
    id             bigint       GENERATED ALWAYS AS IDENTITY,
    name           varchar(200) NOT NULL,
    city           varchar(100),
    address        varchar(300),
    phone          varchar(50),
    currency_code  varchar(3)   NOT NULL DEFAULT 'MGA',
    is_active      boolean      NOT NULL DEFAULT true,
    envelope_next_number integer NOT NULL DEFAULT 1,
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now(),
    created_by     bigint,
    updated_by     bigint,
    CONSTRAINT pk_churches PRIMARY KEY (id),
    CONSTRAINT ck_churches_currency_code CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_churches_name CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_churches_envelope_next_number CHECK (envelope_next_number > 0)
);

CREATE INDEX idx_churches_name ON churches (lower(name));
