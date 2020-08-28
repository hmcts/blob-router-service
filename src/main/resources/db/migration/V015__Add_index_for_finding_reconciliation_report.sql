CREATE INDEX envelope_reconciliation_reports_account_created_at_idx
  ON envelope_reconciliation_reports (account, created_at);
