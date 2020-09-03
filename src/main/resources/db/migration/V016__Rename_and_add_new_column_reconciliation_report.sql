ALTER TABLE envelope_reconciliation_reports
RENAME COLUMN content TO summary_content;

ALTER TABLE envelope_reconciliation_reports
ADD COLUMN detailed_content JSON NULL;
