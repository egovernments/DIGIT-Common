package org.egov.config.repository;

import lombok.extern.slf4j.Slf4j;
import org.egov.config.repository.querybuilder.ConfigSetQueryBuilder;
import org.egov.config.repository.rowmapper.ConfigSetRowMapper;
import org.egov.config.utils.CustomException;
import org.egov.config.web.model.ConfigSet;
import org.egov.config.web.model.ConfigSetActivation;
import org.egov.config.web.model.ConfigSetSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class ConfigSetRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ConfigSetQueryBuilder queryBuilder;
    private final ConfigSetRowMapper rowMapper;

    private static final String INSERT_QUERY =
            "INSERT INTO eg_config_set (id, tenant_id, name, code, description, status, " +
            "created_by, created_time, last_modified_by, last_modified_time) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_QUERY =
            "UPDATE eg_config_set SET name = ?, description = ?, status = ?, " +
            "last_modified_by = ?, last_modified_time = ? WHERE id = ?";

    private static final String ACTIVATE_QUERY =
            "UPDATE eg_config_set SET status = ?, last_modified_by = ?, last_modified_time = ? " +
            "WHERE id = ?";

    private static final String DEACTIVATE_BY_TENANT_QUERY =
            "UPDATE eg_config_set SET status = 'INACTIVE', last_modified_by = ?, last_modified_time = ? " +
            "WHERE tenant_id = ? AND status = 'ACTIVE' AND id != ?";

    private static final String FIND_ACTIVE_SET_QUERY =
            "SELECT id FROM eg_config_set WHERE tenant_id = ? AND status = 'ACTIVE' LIMIT 1";

    private static final String INSERT_ACTIVATION_QUERY =
            "INSERT INTO eg_config_set_activation (id, config_set_id, tenant_id, activated_by, " +
            "activated_time, previous_active_set_id) VALUES (?, ?, ?, ?, ?, ?)";

    @Autowired
    public ConfigSetRepository(JdbcTemplate jdbcTemplate, ConfigSetQueryBuilder queryBuilder,
                               ConfigSetRowMapper rowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.queryBuilder = queryBuilder;
        this.rowMapper = rowMapper;
    }

    public void create(ConfigSet configSet) {
        AuditParams a = auditParams(configSet);
        jdbcTemplate.update(INSERT_QUERY, configSet.getId(), configSet.getTenantId(),
                configSet.getName(), configSet.getCode(), configSet.getDescription(),
                configSet.getStatus(), a.createdBy, a.createdTime, a.lastModifiedBy, a.lastModifiedTime);
    }

    public void update(ConfigSet configSet) {
        AuditParams a = auditParams(configSet);
        int rows = jdbcTemplate.update(UPDATE_QUERY, configSet.getName(), configSet.getDescription(),
                configSet.getStatus(), a.lastModifiedBy, a.lastModifiedTime, configSet.getId());
        if (rows == 0) {
            throw new CustomException("CONFIG_SET_NOT_FOUND", "Config set with id " + configSet.getId() + " not found");
        }
    }

    public String findActiveSetId(String tenantId) {
        List<String> ids = jdbcTemplate.queryForList(FIND_ACTIVE_SET_QUERY, String.class, tenantId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    public void activate(String configSetId, String tenantId, String userId) {
        long now = System.currentTimeMillis();
        jdbcTemplate.update(ACTIVATE_QUERY, "ACTIVE", userId, now, configSetId);
    }

    public void deactivateOthers(String configSetId, String tenantId, String userId) {
        long now = System.currentTimeMillis();
        jdbcTemplate.update(DEACTIVATE_BY_TENANT_QUERY, userId, now, tenantId, configSetId);
    }

    public void recordActivation(ConfigSetActivation activation) {
        jdbcTemplate.update(INSERT_ACTIVATION_QUERY, activation.getId(), activation.getConfigSetId(),
                activation.getTenantId(), activation.getActivatedBy(), activation.getActivatedTime(),
                activation.getPreviousActiveSetId());
    }

    public List<ConfigSet> search(ConfigSetSearchCriteria criteria) {
        List<Object> preparedStmtList = new ArrayList<>();
        String query = queryBuilder.getSearchQuery(criteria, preparedStmtList);
        log.debug("ConfigSet search query: {}", query);
        return jdbcTemplate.query(query, rowMapper, preparedStmtList.toArray());
    }

    public Long getCount(ConfigSetSearchCriteria criteria) {
        List<Object> preparedStmtList = new ArrayList<>();
        String query = queryBuilder.getCountQuery(criteria, preparedStmtList);
        return jdbcTemplate.queryForObject(query, Long.class, preparedStmtList.toArray());
    }

    private record AuditParams(String createdBy, Long createdTime, String lastModifiedBy, Long lastModifiedTime) {}

    private AuditParams auditParams(ConfigSet cs) {
        if (cs.getAuditDetails() == null) return new AuditParams(null, null, null, null);
        return new AuditParams(cs.getAuditDetails().getCreatedBy(), cs.getAuditDetails().getCreatedTime(),
                cs.getAuditDetails().getLastModifiedBy(), cs.getAuditDetails().getLastModifiedTime());
    }
}
