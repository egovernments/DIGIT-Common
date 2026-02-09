package org.egov.config.repository.rowmapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.egov.config.web.model.AuditDetails;
import org.egov.config.web.model.ConfigVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ConfigVersionRowMapper implements ResultSetExtractor<List<ConfigVersion>> {

    private final ObjectMapper objectMapper;

    @Autowired
    public ConfigVersionRowMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ConfigVersion> extractData(ResultSet rs) throws SQLException {
        List<ConfigVersion> versions = new ArrayList<>();
        while (rs.next()) {
            String contentStr = rs.getString("content");
            JsonNode content = null;
            if (contentStr != null) {
                try {
                    content = objectMapper.readTree(contentStr);
                } catch (Exception e) {
                    log.error("Error parsing version content JSON for version id: {}", rs.getString("id"), e);
                }
            }

            ConfigVersion version = ConfigVersion.builder()
                    .id(rs.getString("id"))
                    .configId(rs.getString("config_id"))
                    .version(rs.getString("version"))
                    .content(content)
                    .schemaRef(rs.getString("schema_ref"))
                    .status(rs.getString("status"))
                    .auditDetails(AuditDetails.builder()
                            .createdBy(rs.getString("created_by"))
                            .createdTime(rs.getLong("created_time"))
                            .lastModifiedBy(rs.getString("last_modified_by"))
                            .lastModifiedTime(rs.getLong("last_modified_time"))
                            .build())
                    .build();
            versions.add(version);
        }
        return versions;
    }
}
