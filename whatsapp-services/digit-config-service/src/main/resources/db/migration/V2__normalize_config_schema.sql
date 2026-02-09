-- ============================================================
-- V2: Normalize config schema into sets, configs, versions
-- ============================================================
-- ER Model:
--   eg_config_set  1──N  eg_config  1──N  eg_config_version
--   eg_config_set  1──N  eg_config_set_activation (history)
-- ============================================================

-- Drop the flat bootstrap table
DROP TABLE IF EXISTS eg_config;

-- ----------------------------------------------------------
-- Config Sets: logical grouping of related configs
-- ----------------------------------------------------------
CREATE TABLE eg_config_set (
    id                  VARCHAR(64)   PRIMARY KEY,
    tenant_id           VARCHAR(64)   NOT NULL,
    name                VARCHAR(128)  NOT NULL,
    code                VARCHAR(128)  NOT NULL,
    description         VARCHAR(1024),
    status              VARCHAR(32)   NOT NULL DEFAULT 'INACTIVE',
    created_by          VARCHAR(64),
    created_time        BIGINT,
    last_modified_by    VARCHAR(64),
    last_modified_time  BIGINT,
    CONSTRAINT uq_config_set_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_config_set_tenant ON eg_config_set (tenant_id);
CREATE INDEX idx_config_set_status ON eg_config_set (status);

-- ----------------------------------------------------------
-- Configs: individual config entries
-- ----------------------------------------------------------
CREATE TABLE eg_config (
    id                  VARCHAR(64)   PRIMARY KEY,
    config_set_id       VARCHAR(64),
    tenant_id           VARCHAR(64)   NOT NULL,
    namespace           VARCHAR(128)  NOT NULL,
    config_name         VARCHAR(128)  NOT NULL,
    config_code         VARCHAR(128)  NOT NULL,
    environment         VARCHAR(32),
    description         VARCHAR(1024),
    status              VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE',
    created_by          VARCHAR(64),
    created_time        BIGINT,
    last_modified_by    VARCHAR(64),
    last_modified_time  BIGINT,
    CONSTRAINT uq_config_tenant_ns_code UNIQUE (tenant_id, namespace, config_code),
    CONSTRAINT fk_config_set FOREIGN KEY (config_set_id) REFERENCES eg_config_set(id)
);

CREATE INDEX idx_config_tenant    ON eg_config (tenant_id);
CREATE INDEX idx_config_namespace ON eg_config (namespace);
CREATE INDEX idx_config_code      ON eg_config (config_code);
CREATE INDEX idx_config_status    ON eg_config (status);
CREATE INDEX idx_config_set_ref   ON eg_config (config_set_id);

-- ----------------------------------------------------------
-- Config Versions: versioned content snapshots per config
-- ----------------------------------------------------------
CREATE TABLE eg_config_version (
    id                  VARCHAR(64)   PRIMARY KEY,
    config_id           VARCHAR(64)   NOT NULL,
    version             VARCHAR(64)   NOT NULL,
    content             CLOB,
    schema_ref          VARCHAR(256),
    status              VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE',
    created_by          VARCHAR(64),
    created_time        BIGINT,
    last_modified_by    VARCHAR(64),
    last_modified_time  BIGINT,
    CONSTRAINT uq_config_version UNIQUE (config_id, version),
    CONSTRAINT fk_config FOREIGN KEY (config_id) REFERENCES eg_config(id)
);

CREATE INDEX idx_config_version_config ON eg_config_version (config_id);
CREATE INDEX idx_config_version_status ON eg_config_version (status);

-- ----------------------------------------------------------
-- Config Set Activation: audit trail for set activations
-- ----------------------------------------------------------
CREATE TABLE eg_config_set_activation (
    id                      VARCHAR(64)  PRIMARY KEY,
    config_set_id           VARCHAR(64)  NOT NULL,
    tenant_id               VARCHAR(64)  NOT NULL,
    activated_by            VARCHAR(64),
    activated_time          BIGINT,
    previous_active_set_id  VARCHAR(64),
    CONSTRAINT fk_activation_set FOREIGN KEY (config_set_id) REFERENCES eg_config_set(id)
);

CREATE INDEX idx_activation_tenant ON eg_config_set_activation (tenant_id);
CREATE INDEX idx_activation_set    ON eg_config_set_activation (config_set_id);
