CREATE TABLE invoices (
    id         BIGSERIAL PRIMARY KEY,
    customer   VARCHAR(120)   NOT NULL,
    amount     NUMERIC(12, 2) NOT NULL,
    status     VARCHAR(20)    NOT NULL,
    created_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    closed_at  TIMESTAMPTZ,
    closed_by  VARCHAR(120),
    CONSTRAINT chk_invoices_status CHECK (status IN ('PENDING', 'CLOSED'))
);

CREATE INDEX idx_invoices_status ON invoices (status);
