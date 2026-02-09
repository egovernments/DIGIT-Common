package org.egov.config.service.enrichment;

import org.egov.config.web.model.AuditDetails;
import org.egov.config.web.model.Config;
import org.egov.config.web.model.ConfigRequest;
import org.egov.config.web.model.ConfigVersion;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ConfigEnricher {

    public void enrichCreateRequest(ConfigRequest request) {
        Config config = request.getConfig();
        config.setId(UUID.randomUUID().toString());

        String userId = getUserId(request);
        long now = System.currentTimeMillis();
        AuditDetails audit = AuditDetails.builder()
                .createdBy(userId).createdTime(now)
                .lastModifiedBy(userId).lastModifiedTime(now)
                .build();
        config.setAuditDetails(audit);

        if (config.getVersions() != null) {
            for (ConfigVersion v : config.getVersions()) {
                enrichVersion(v, config.getId(), userId, now);
            }
        }
    }

    public void enrichUpdateRequest(ConfigRequest request) {
        Config config = request.getConfig();
        String userId = getUserId(request);
        long now = System.currentTimeMillis();

        AuditDetails existing = config.getAuditDetails() != null ? config.getAuditDetails() : new AuditDetails();
        existing.setLastModifiedBy(userId);
        existing.setLastModifiedTime(now);
        config.setAuditDetails(existing);

        if (config.getVersions() != null) {
            for (ConfigVersion v : config.getVersions()) {
                if (v.getId() == null) {
                    enrichVersion(v, config.getId(), userId, now);
                }
            }
        }
    }

    public ConfigVersion enrichNewVersion(String configId, ConfigVersion version, String userId) {
        long now = System.currentTimeMillis();
        enrichVersion(version, configId, userId, now);
        return version;
    }

    private void enrichVersion(ConfigVersion v, String configId, String userId, long now) {
        v.setId(UUID.randomUUID().toString());
        v.setConfigId(configId);
        if (v.getStatus() == null) v.setStatus("ACTIVE");
        v.setAuditDetails(AuditDetails.builder()
                .createdBy(userId).createdTime(now)
                .lastModifiedBy(userId).lastModifiedTime(now)
                .build());
    }

    private String getUserId(ConfigRequest request) {
        if (request.getRequestInfo() != null && request.getRequestInfo().getUserInfo() != null
                && request.getRequestInfo().getUserInfo().getUuid() != null) {
            return request.getRequestInfo().getUserInfo().getUuid();
        }
        return "SYSTEM";
    }
}
