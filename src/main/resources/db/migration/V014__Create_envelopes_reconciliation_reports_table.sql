CREATE TABLE envelope_reconciliation_reports
(
    id                             UUID        PRIMARY KEY,
    envelope_supplier_statement_id UUID        NOT NULL REFERENCES envelope_supplier_statements ON DELETE CASCADE,
    account                        VARCHAR(50) NOT NULL,
    content                        JSON        NOT NULL,
    content_type_version           VARCHAR(20) NOT NULL,
    sent_at                        TIMESTAMP   NULL,
    created_at                     TIMESTAMP   NOT NULL
)
