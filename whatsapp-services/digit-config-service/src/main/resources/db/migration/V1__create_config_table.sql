CREATE TABLE IF NOT EXISTS eg_config (
    id              VARCHAR(64)   PRIMARY KEY,
    tenant_id       VARCHAR(64)   NOT NULL,
    namespace       VARCHAR(128)  NOT NULL,
    config_name     VARCHAR(128)  NOT NULL,
    config_code     VARCHAR(128)  NOT NULL,
    version         VARCHAR(64)   NOT NULL,
    status          VARCHAR(32)   NOT NULL,
    environment     VARCHAR(32),
    description     VARCHAR(1024),
    content         CLOB,
    created_by      VARCHAR(64),
    created_time    BIGINT,
    last_modified_by VARCHAR(64),
    last_modified_time BIGINT,
    CONSTRAINT uq_config_tenant_ns_code_ver UNIQUE (tenant_id, namespace, config_code, version)
);

CREATE INDEX IF NOT EXISTS idx_config_tenant ON eg_config (tenant_id);
CREATE INDEX IF NOT EXISTS idx_config_namespace ON eg_config (namespace);
CREATE INDEX IF NOT EXISTS idx_config_code ON eg_config (config_code);
CREATE INDEX IF NOT EXISTS idx_config_status ON eg_config (status);
