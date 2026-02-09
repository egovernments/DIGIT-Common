package org.egov.config.repository.querybuilder;

import org.egov.config.config.ApplicationConfig;
import org.egov.config.web.model.ConfigSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class ConfigQueryBuilder {

    private static final String BASE_CONFIG_QUERY =
            "SELECT c.id, c.config_set_id, c.tenant_id, c.namespace, c.config_name, c.config_code, " +
            "c.environment, c.description, c.status, " +
            "c.created_by, c.created_time, c.last_modified_by, c.last_modified_time " +
            "FROM eg_config c";

    private static final String CONFIG_COUNT_QUERY = "SELECT COUNT(*) FROM eg_config c";

    private static final String VERSION_BY_CONFIG_QUERY =
            "SELECT cv.id, cv.config_id, cv.version, cv.content, cv.schema_ref, cv.status, " +
            "cv.created_by, cv.created_time, cv.last_modified_by, cv.last_modified_time " +
            "FROM eg_config_version cv WHERE cv.config_id = ? ORDER BY cv.created_time DESC";

    private static final String ACTIVE_VERSION_QUERY =
            "SELECT cv.id, cv.config_id, cv.version, cv.content, cv.schema_ref, cv.status, " +
            "cv.created_by, cv.created_time, cv.last_modified_by, cv.last_modified_time " +
            "FROM eg_config_version cv WHERE cv.config_id = ? AND cv.status = 'ACTIVE' LIMIT 1";

    private final ApplicationConfig config;

    @Autowired
    public ConfigQueryBuilder(ApplicationConfig config) {
        this.config = config;
    }

    public String getSearchQuery(ConfigSearchCriteria criteria, List<Object> preparedStmtList) {
        StringBuilder builder = new StringBuilder(BASE_CONFIG_QUERY);
        addWhereClause(builder, criteria, preparedStmtList);
        builder.append(" ORDER BY c.created_time DESC");
        addPagination(builder, criteria, preparedStmtList);
        return builder.toString();
    }

    public String getCountQuery(ConfigSearchCriteria criteria, List<Object> preparedStmtList) {
        StringBuilder builder = new StringBuilder(CONFIG_COUNT_QUERY);
        addWhereClause(builder, criteria, preparedStmtList);
        return builder.toString();
    }

    public String getVersionsByConfigQuery() {
        return VERSION_BY_CONFIG_QUERY;
    }

    public String getActiveVersionQuery() {
        return ACTIVE_VERSION_QUERY;
    }

    private void addWhereClause(StringBuilder builder, ConfigSearchCriteria criteria, List<Object> preparedStmtList) {
        if (criteria == null) return;

        if (StringUtils.hasText(criteria.getTenantId())) {
            addClause(builder, preparedStmtList);
            builder.append(" c.tenant_id = ? ");
            preparedStmtList.add(criteria.getTenantId());
        }
        if (StringUtils.hasText(criteria.getNamespace())) {
            addClause(builder, preparedStmtList);
            builder.append(" c.namespace = ? ");
            preparedStmtList.add(criteria.getNamespace());
        }
        if (StringUtils.hasText(criteria.getConfigName())) {
            addClause(builder, preparedStmtList);
            builder.append(" c.config_name = ? ");
            preparedStmtList.add(criteria.getConfigName());
        }
        if (StringUtils.hasText(criteria.getConfigCode())) {
            addClause(builder, preparedStmtList);
            builder.append(" c.config_code = ? ");
            preparedStmtList.add(criteria.getConfigCode());
        }
        if (StringUtils.hasText(criteria.getEnvironment())) {
            addClause(builder, preparedStmtList);
            builder.append(" c.environment = ? ");
            preparedStmtList.add(criteria.getEnvironment());
        }
        if (StringUtils.hasText(criteria.getStatus())) {
            addClause(builder, preparedStmtList);
            builder.append(" c.status = ? ");
            preparedStmtList.add(criteria.getStatus());
        }
        if (StringUtils.hasText(criteria.getVersion())) {
            addClause(builder, preparedStmtList);
            builder.append(" EXISTS (SELECT 1 FROM eg_config_version cv WHERE cv.config_id = c.id AND cv.version = ?) ");
            preparedStmtList.add(criteria.getVersion());
        }
    }

    private void addPagination(StringBuilder builder, ConfigSearchCriteria criteria, List<Object> preparedStmtList) {
        int limit = criteria.getLimit() != null ? criteria.getLimit() : config.getDefaultLimit();
        int offset = criteria.getOffset() != null ? criteria.getOffset() : config.getDefaultOffset();
        builder.append(" LIMIT ? OFFSET ?");
        preparedStmtList.add(limit);
        preparedStmtList.add(offset);
    }

    private void addClause(StringBuilder builder, List<Object> preparedStmtList) {
        builder.append(preparedStmtList.isEmpty() ? " WHERE " : " AND ");
    }
}
