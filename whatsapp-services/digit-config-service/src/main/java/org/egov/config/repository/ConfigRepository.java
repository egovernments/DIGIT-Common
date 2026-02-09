package org.egov.config.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.egov.config.repository.querybuilder.ConfigQueryBuilder;
import org.egov.config.repository.rowmapper.ConfigRowMapper;
import org.egov.config.repository.rowmapper.ConfigVersionRowMapper;
import org.egov.config.utils.CustomException;
import org.egov.config.web.model.Config;
import org.egov.config.web.model.ConfigSearchCriteria;
import org.egov.config.web.model.ConfigVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class ConfigRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ConfigQueryBuilder queryBuilder;
    private final ConfigRowMapper rowMapper;
    private final ConfigVersionRowMapper versionRowMapper;
    private final ObjectMapper objectMapper;

    private static final String INSERT_CONFIG =
            "INSERT INTO eg_config (id, config_set_id, tenant_id, namespace, config_name, config_code, " +
            "environment, description, status, created_by, created_time, last_modified_by, last_modified_time) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_CONFIG =
            "UPDATE eg_config SET config_name = ?, environment = ?, description = ?, status = ?, " +
            "last_modified_by = ?, last_modified_time = ? WHERE id = ?";

    private static final String INSERT_VERSION =
            "INSERT INTO eg_config_version (id, config_id, version, content, schema_ref, status, " +
            "created_by, created_time, last_modified_by, last_modified_time) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String DEACTIVATE_VERSIONS =
            "UPDATE eg_config_version SET status = 'INACTIVE', last_modified_by = ?, last_modified_time = ? " +
            "WHERE config_id = ? AND status = 'ACTIVE'";

    @Autowired
    public ConfigRepository(JdbcTemplate jdbcTemplate, ConfigQueryBuilder queryBuilder,
                            ConfigRowMapper rowMapper, ConfigVersionRowMapper versionRowMapper,
                            ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.queryBuilder = queryBuilder;
        this.rowMapper = rowMapper;
        this.versionRowMapper = versionRowMapper;
        this.objectMapper = objectMapper;
    }

    public void createConfig(Config config) {
        jdbcTemplate.update(INSERT_CONFIG, config.getId(), config.getConfigSetId(),
                config.getTenantId(), config.getNamespace(), config.getConfigName(),
                config.getConfigCode(), config.getEnvironment(), config.getDescription(),
                config.getStatus(),
                config.getAuditDetails().getCreatedBy(), config.getAuditDetails().getCreatedTime(),
                config.getAuditDetails().getLastModifiedBy(), config.getAuditDetails().getLastModifiedTime());
    }

    public void updateConfig(Config config) {
        int rows = jdbcTemplate.update(UPDATE_CONFIG, config.getConfigName(), config.getEnvironment(),
                config.getDescription(), config.getStatus(),
                config.getAuditDetails().getLastModifiedBy(), config.getAuditDetails().getLastModifiedTime(),
                config.getId());
        if (rows == 0) {
            throw new CustomException("CONFIG_NOT_FOUND", "Config with id " + config.getId() + " not found");
        }
    }

    public void createVersion(ConfigVersion version) {
        String contentJson = serializeContent(version);
        jdbcTemplate.update(INSERT_VERSION, version.getId(), version.getConfigId(),
                version.getVersion(), contentJson, version.getSchemaRef(), version.getStatus(),
                version.getAuditDetails().getCreatedBy(), version.getAuditDetails().getCreatedTime(),
                version.getAuditDetails().getLastModifiedBy(), version.getAuditDetails().getLastModifiedTime());
    }

    public void deactivateVersions(String configId, String userId) {
        long now = System.currentTimeMillis();
        jdbcTemplate.update(DEACTIVATE_VERSIONS, userId, now, configId);
    }

    public List<Config> search(ConfigSearchCriteria criteria) {
        List<Object> preparedStmtList = new ArrayList<>();
        String query = queryBuilder.getSearchQuery(criteria, preparedStmtList);
        log.debug("Config search query: {}", query);
        return jdbcTemplate.query(query, rowMapper, preparedStmtList.toArray());
    }

    public Long getCount(ConfigSearchCriteria criteria) {
        List<Object> preparedStmtList = new ArrayList<>();
        String query = queryBuilder.getCountQuery(criteria, preparedStmtList);
        return jdbcTemplate.queryForObject(query, Long.class, preparedStmtList.toArray());
    }

    public List<ConfigVersion> getVersionsByConfigId(String configId) {
        return jdbcTemplate.query(queryBuilder.getVersionsByConfigQuery(), versionRowMapper, configId);
    }

    public List<ConfigVersion> getActiveVersion(String configId) {
        return jdbcTemplate.query(queryBuilder.getActiveVersionQuery(), versionRowMapper, configId);
    }

    private String serializeContent(ConfigVersion version) {
        if (version.getContent() == null) return null;
        try {
            return objectMapper.writeValueAsString(version.getContent());
        } catch (JsonProcessingException e) {
            throw new CustomException("INVALID_CONTENT", "Failed to serialize config content");
        }
    }
}
