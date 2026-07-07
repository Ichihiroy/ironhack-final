-- Overcast core schema (SQL Server dialect — Azure SQL in production)
CREATE TABLE scan (
    id                  NVARCHAR(64)  PRIMARY KEY,
    source_cloud        NVARCHAR(16)  NOT NULL,
    filename            NVARCHAR(255) NOT NULL,
    uploaded_at         DATETIME2     NOT NULL,
    currency            NVARCHAR(8)   NOT NULL,
    total_monthly_cost  DECIMAL(14,2) NOT NULL,
    total_monthly_waste DECIMAL(14,2) NOT NULL
);

CREATE TABLE finding (
    id                NVARCHAR(64)   PRIMARY KEY,
    scan_id           NVARCHAR(64)   NOT NULL REFERENCES scan (id) ON DELETE CASCADE,
    resource_id       NVARCHAR(512)  NOT NULL,
    resource_type     NVARCHAR(128)  NOT NULL,
    resource_group    NVARCHAR(128),
    region            NVARCHAR(64),
    rule_id           NVARCHAR(64)   NOT NULL,
    category          NVARCHAR(16)   NOT NULL,
    monthly_cost      DECIMAL(14,2)  NOT NULL,
    monthly_saving    DECIMAL(14,2)  NOT NULL,
    remediation       NVARCHAR(1024) NOT NULL,
    explanation_cache NVARCHAR(MAX)
);

CREATE INDEX idx_finding_scan ON finding (scan_id);
