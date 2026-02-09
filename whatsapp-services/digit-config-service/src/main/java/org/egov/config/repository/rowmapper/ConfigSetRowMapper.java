package org.egov.config.repository.rowmapper;

import org.egov.config.web.model.AuditDetails;
import org.egov.config.web.model.ConfigSet;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class ConfigSetRowMapper implements ResultSetExtractor<List<ConfigSet>> {

    @Override
    public List<ConfigSet> extractData(ResultSet rs) throws SQLException {
        List<ConfigSet> configSets = new ArrayList<>();
        while (rs.next()) {
            ConfigSet cs = ConfigSet.builder()
                    .id(rs.getString("id"))
                    .tenantId(rs.getString("tenant_id"))
                    .name(rs.getString("name"))
                    .code(rs.getString("code"))
                    .description(rs.getString("description"))
                    .status(rs.getString("status"))
                    .auditDetails(AuditDetails.builder()
                            .createdBy(rs.getString("created_by"))
                            .createdTime(rs.getLong("created_time"))
                            .lastModifiedBy(rs.getString("last_modified_by"))
                            .lastModifiedTime(rs.getLong("last_modified_time"))
                            .build())
                    .build();
            configSets.add(cs);
        }
        return configSets;
    }
}
