CREATE TABLE envelope_supplier_statements (
    id                   UUID        PRIMARY KEY,
    date                 DATE        NOT NULL,
    content              JSON        NOT NULL,
    content_type_version VARCHAR(20) NOT NULL,
    created_at           TIMESTAMP   NOT NULL
);
