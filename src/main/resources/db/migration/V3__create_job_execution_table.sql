-- Auditoria: uma linha por TENTATIVA de execucao, inclusive as que nao
-- conseguiram o lock. E isso que torna o skip visivel.
CREATE TABLE job_execution (
    id              BIGSERIAL PRIMARY KEY,
    job_name        VARCHAR(64)  NOT NULL,
    owner           VARCHAR(120) NOT NULL,
    status          VARCHAR(40)  NOT NULL,
    processed_items INTEGER      NOT NULL DEFAULT 0,
    started_at      TIMESTAMPTZ  NOT NULL,
    finished_at     TIMESTAMPTZ  NOT NULL,
    trigger_source  VARCHAR(20)  NOT NULL,
    error_message   VARCHAR(500),
    CONSTRAINT chk_job_execution_status
        CHECK (status IN ('SUCCESS', 'SKIPPED_LOCK_NOT_ACQUIRED', 'FAILED')),
    CONSTRAINT chk_job_execution_trigger
        CHECK (trigger_source IN ('SCHEDULED', 'MANUAL'))
);

CREATE INDEX idx_job_execution_job_started ON job_execution (job_name, started_at DESC);
