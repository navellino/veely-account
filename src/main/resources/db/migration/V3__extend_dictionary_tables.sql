-- ============================================================================
-- V3__extend_dictionary_tables.sql
-- Estende le tabelle dizionario per supportare tutte le colonne degli Excel
-- ============================================================================

-- VAT CODES: aggiungo colonne che esistono nel tuo excel ma non in V1
ALTER TABLE vat_codes
  ADD COLUMN vat_edf_code VARCHAR(50) NULL,
  ADD COLUMN vat_grouping VARCHAR(100) NULL,
  ADD COLUMN stamp_duty_applicable BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN agri_comp_rate DECIMAL(5,2) NULL,
  ADD COLUMN notes TEXT NULL,
  ADD COLUMN external_code VARCHAR(50) NULL;

-- WITHHOLDING TYPES: aggiungo campi per locazioni brevi e scadenza data
ALTER TABLE withholding_types
  ADD COLUMN short_rent BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN due_date DATE NULL;
