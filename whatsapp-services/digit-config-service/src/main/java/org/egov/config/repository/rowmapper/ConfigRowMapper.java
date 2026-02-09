package org.egov.config.repository.rowmapper;

import org.egov.config.web.model.AuditDetails;
import org.egov.config.web.model.Config;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class ConfigRowMapper implements ResultSetExtractor<List<Config>> {

    @Override
    public List<Config> extractData(ResultSet rs) throws SQLException {
        List<Config> configs = new ArrayList<>();
        while (rs.next()) {
            Config config = Config.builder()
                    .id(rs.getString("id"))
                    .configSetId(rs.getString("config_set_id"))
                    .tenantId(rs.getString("tenant_id"))
                    .namespace(rs.getString("namespace"))
                    .configName(rs.getString("config_name"))
                    .configCode(rs.getString("config_code"))
                    .environment(rs.getString("environment"))
                    .description(rs.getString("description"))
                    .status(rs.getString("status"))
                    .auditDetails(AuditDetails.builder()
                            .createdBy(rs.getString("created_by"))
                            .createdTime(rs.getLong("created_time"))
                            .lastModifiedBy(rs.getString("last_modified_by"))
                            .lastModifiedTime(rs.getLong("last_modified_time"))
                            .build())
                    .build();
            configs.add(config);
        }
        return configs;
    }
}
