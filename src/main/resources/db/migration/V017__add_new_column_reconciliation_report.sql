ALTER TABLE envelope_reconciliation_reports
ADD COLUMN report_for DATE DEFAULT NOW();
