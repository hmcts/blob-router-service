ALTER TABLE envelopes
ADD COLUMN pending_notification BOOLEAN DEFAULT FALSE;

CREATE INDEX envelopes_pending_notification_idx ON envelopes (pending_notification);
