-- ============================================================================
-- V2__seed_lookups.sql
-- Seed tabelle lookup base
-- ============================================================================

INSERT INTO counterparty_kinds (code, description) VALUES
('CUSTOMER', 'Cliente'),
('SUPPLIER', 'Fornitore'),
('PROFESSIONAL', 'Professionista');

INSERT INTO invoice_directions (code, description) VALUES
('ACTIVE', 'Fattura attiva'),
('PASSIVE', 'Fattura passiva');

INSERT INTO invoice_statuses (code, description) VALUES
('ISSUED', 'Emessa'),
('PAID', 'Pagata'),
('CANCELLED', 'Annullata');

INSERT INTO bank_transaction_categories (code, description) VALUES
('UNCATEGORIZED', 'Non categorizzato'),
('INVOICE_PAYMENT', 'Pagamento fattura'),
('TAX', 'Imposte e tributi'),
('FEE', 'Commissioni bancarie'),
('OTHER', 'Altro');
