package org.egov.config.repository.querybuilder;

import org.egov.config.config.ApplicationConfig;
import org.egov.config.web.model.ConfigSetSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class ConfigSetQueryBuilder {

    private static final String BASE_QUERY =
            "SELECT cs.id, cs.tenant_id, cs.name, cs.code, cs.description, cs.status, " +
            "cs.created_by, cs.created_time, cs.last_modified_by, cs.last_modified_time " +
            "FROM eg_config_set cs";

    private static final String COUNT_QUERY = "SELECT COUNT(*) FROM eg_config_set cs";

    private final ApplicationConfig config;

    @Autowired
    public ConfigSetQueryBuilder(ApplicationConfig config) {
        this.config = config;
    }

    public String getSearchQuery(ConfigSetSearchCriteria criteria, List<Object> preparedStmtList) {
        StringBuilder builder = new StringBuilder(BASE_QUERY);
        addWhereClause(builder, criteria, preparedStmtList);
        builder.append(" ORDER BY cs.created_time DESC");
        addPagination(builder, criteria, preparedStmtList);
        return builder.toString();
    }

    public String getCountQuery(ConfigSetSearchCriteria criteria, List<Object> preparedStmtList) {
        StringBuilder builder = new StringBuilder(COUNT_QUERY);
        addWhereClause(builder, criteria, preparedStmtList);
        return builder.toString();
    }

    private void addWhereClause(StringBuilder builder, ConfigSetSearchCriteria criteria, List<Object> preparedStmtList) {
        if (criteria == null) return;

        if (StringUtils.hasText(criteria.getTenantId())) {
            addClause(builder, preparedStmtList);
            builder.append(" cs.tenant_id = ? ");
            preparedStmtList.add(criteria.getTenantId());
        }
        if (StringUtils.hasText(criteria.getName())) {
            addClause(builder, preparedStmtList);
            builder.append(" cs.name = ? ");
            preparedStmtList.add(criteria.getName());
        }
        if (StringUtils.hasText(criteria.getCode())) {
            addClause(builder, preparedStmtList);
            builder.append(" cs.code = ? ");
            preparedStmtList.add(criteria.getCode());
        }
        if (StringUtils.hasText(criteria.getStatus())) {
            addClause(builder, preparedStmtList);
            builder.append(" cs.status = ? ");
            preparedStmtList.add(criteria.getStatus());
        }
    }

    private void addPagination(StringBuilder builder, ConfigSetSearchCriteria criteria, List<Object> preparedStmtList) {
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
