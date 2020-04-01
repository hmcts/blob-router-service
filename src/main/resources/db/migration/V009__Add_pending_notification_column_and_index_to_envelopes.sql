ALTER TABLE envelopes
ADD COLUMN pending_notification BOOLEAN DEFAULT FALSE;

CREATE INDEX envelopes_pending_notification_envelope_id_idx ON envelopes (id, pending_notification);
