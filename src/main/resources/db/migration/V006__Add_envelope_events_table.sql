CREATE TABLE envelope_events
(
    id          BIGSERIAL    PRIMARY KEY,
    envelope_id UUID         NOT NULL REFERENCES envelopes ON DELETE CASCADE,
    created_at  TIMESTAMP    NOT NULL,
    type        VARCHAR(100) NOT NULL,
    notes       TEXT         NULL
);
