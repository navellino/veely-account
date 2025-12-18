-- ============================================================================
-- V1__init.sql
-- Inizializzazione schema veely-account
-- Approccio table-driven (no ENUM DB)
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================
-- LOOKUP TABLES
-- =========================

CREATE TABLE counterparty_kinds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE invoice_directions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE invoice_statuses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE bank_transaction_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- DICTIONARY TABLES
-- =========================

CREATE TABLE accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    level TINYINT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE vat_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    rate DECIMAL(5,2) NULL,
    registry_description VARCHAR(255) NULL,
    long_description TEXT NULL,
    operation_type VARCHAR(100) NULL,
    category VARCHAR(100) NULL,
    use_purchases BOOLEAN NOT NULL DEFAULT TRUE,
    use_sales BOOLEAN NOT NULL DEFAULT TRUE,
    use_receipts BOOLEAN NOT NULL DEFAULT TRUE,
    custom_nature_purchases VARCHAR(50) NULL,
    custom_nature_sales VARCHAR(50) NULL,
    reverse_charge_relevant BOOLEAN NOT NULL DEFAULT FALSE,
    validity VARCHAR(50) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE withholding_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    category VARCHAR(100) NULL,
    effective_from DATE NULL,
    rate DECIMAL(5,2) NULL,
    taxable_percent DECIMAL(5,2) NULL,
    tribute_code VARCHAR(50) NULL,
    tribute_description VARCHAR(255) NULL,
    due_date_description VARCHAR(255) NULL,
    long_description TEXT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- MASTER DATA
-- =========================

CREATE TABLE counterparties (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kind_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    vat_number VARCHAR(30) NULL,
    tax_code VARCHAR(30) NULL,
    pec VARCHAR(255) NULL,
    sdi_code VARCHAR(20) NULL,
    iban VARCHAR(50) NULL,
    notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_counterparty_kind
        FOREIGN KEY (kind_id) REFERENCES counterparty_kinds(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_counterparties_name ON counterparties(name);

-- =========================
-- INVOICES
-- =========================

CREATE TABLE invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    direction_id BIGINT NOT NULL,
    status_id BIGINT NOT NULL,
    counterparty_id BIGINT NOT NULL,
    number VARCHAR(50) NOT NULL,
    year INT NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NULL,
    notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoice_direction
        FOREIGN KEY (direction_id) REFERENCES invoice_directions(id),
    CONSTRAINT fk_invoice_status
        FOREIGN KEY (status_id) REFERENCES invoice_statuses(id),
    CONSTRAINT fk_invoice_counterparty
        FOREIGN KEY (counterparty_id) REFERENCES counterparties(id),
    CONSTRAINT uq_invoice UNIQUE (direction_id, number, year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_invoices_issue_date ON invoices(issue_date);
CREATE INDEX idx_invoices_counterparty ON invoices(counterparty_id);

-- =========================
-- INVOICE LINES
-- =========================

CREATE TABLE invoice_lines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    description VARCHAR(255) NOT NULL,
    net_amount DECIMAL(12,2) NOT NULL,
    vat_code_id BIGINT NULL,
    account_id BIGINT NULL,
    withholding_type_id BIGINT NULL,
    CONSTRAINT fk_line_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    CONSTRAINT fk_line_vat
        FOREIGN KEY (vat_code_id) REFERENCES vat_codes(id),
    CONSTRAINT fk_line_account
        FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT fk_line_withholding
        FOREIGN KEY (withholding_type_id) REFERENCES withholding_types(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_invoice_lines_invoice ON invoice_lines(invoice_id);

-- =========================
-- BANK TRANSACTIONS
-- =========================

CREATE TABLE bank_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_date DATE NOT NULL,
    value_date DATE NULL,
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    counterparty_id BIGINT NULL,
    linked_invoice_id BIGINT NULL,
    category_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_bank_counterparty
        FOREIGN KEY (counterparty_id) REFERENCES counterparties(id),
    CONSTRAINT fk_bank_invoice
        FOREIGN KEY (linked_invoice_id) REFERENCES invoices(id),
    CONSTRAINT fk_bank_category
        FOREIGN KEY (category_id) REFERENCES bank_transaction_categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_bank_booking_date ON bank_transactions(booking_date);
CREATE INDEX idx_bank_amount ON bank_transactions(amount);

SET FOREIGN_KEY_CHECKS = 1;