-- Overcast core schema (PostgreSQL — local dev / docker-compose)
CREATE TABLE scan (
    id                  VARCHAR(64)   PRIMARY KEY,
    source_cloud        VARCHAR(16)   NOT NULL,
    filename            VARCHAR(255)  NOT NULL,
    uploaded_at         TIMESTAMP     NOT NULL,
    currency            VARCHAR(8)    NOT NULL,
    total_monthly_cost  NUMERIC(14,2) NOT NULL,
    total_monthly_waste NUMERIC(14,2) NOT NULL
);

CREATE TABLE finding (
    id                VARCHAR(64)   PRIMARY KEY,
    scan_id           VARCHAR(64)   NOT NULL REFERENCES scan (id) ON DELETE CASCADE,
    resource_id       VARCHAR(512)  NOT NULL,
    resource_type     VARCHAR(128)  NOT NULL,
    resource_group    VARCHAR(128),
    region            VARCHAR(64),
    rule_id           VARCHAR(64)   NOT NULL,
    category          VARCHAR(16)   NOT NULL,
    monthly_cost      NUMERIC(14,2) NOT NULL,
    monthly_saving    NUMERIC(14,2) NOT NULL,
    remediation       VARCHAR(1024) NOT NULL,
    explanation_cache TEXT
);

CREATE INDEX idx_finding_scan ON finding (scan_id);
