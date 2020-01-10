CREATE TABLE envelopes (
  id              UUID         PRIMARY KEY,
  container       VARCHAR(50)  NOT NULL,
  file_name       VARCHAR(255) NOT NULL,
  file_created_at TIMESTAMP    NOT NULL,
  dispatched_at   TIMESTAMP        NULL,
  status          VARCHAR(50)  NOT NULL,
  is_deleted      BOOLEAN      DEFAULT FALSE,
  UNIQUE (container, file_name)
);
