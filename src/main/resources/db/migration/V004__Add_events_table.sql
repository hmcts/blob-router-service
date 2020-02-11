CREATE TABLE events (
  id          BIGSERIAL     PRIMARY KEY,
  container   VARCHAR(50)   NOT NULL,
  file_name   VARCHAR(255)  NOT NULL,
  created_at  TIMESTAMP     NOT NULL,
  event       VARCHAR(100)  NOT NULL,
  notes       TEXT          NULL
);

CREATE INDEX events_container_filename_idx ON events (container, file_name);
CREATE INDEX events_container_event_idx ON events (container, event);
