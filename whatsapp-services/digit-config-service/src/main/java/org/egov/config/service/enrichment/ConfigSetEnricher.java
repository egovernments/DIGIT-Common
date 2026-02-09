package org.egov.config.service.enrichment;

import org.egov.config.web.model.AuditDetails;
import org.egov.config.web.model.ConfigSet;
import org.egov.config.web.model.ConfigSetRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ConfigSetEnricher {

    public void enrichCreateRequest(ConfigSetRequest request) {
        ConfigSet cs = request.getConfigSet();
        cs.setId(UUID.randomUUID().toString());
        if (cs.getStatus() == null) cs.setStatus("INACTIVE");

        String userId = getUserId(request);
        long now = System.currentTimeMillis();
        cs.setAuditDetails(AuditDetails.builder()
                .createdBy(userId).createdTime(now)
                .lastModifiedBy(userId).lastModifiedTime(now)
                .build());
    }

    public void enrichUpdateRequest(ConfigSetRequest request) {
        ConfigSet cs = request.getConfigSet();
        String userId = getUserId(request);
        long now = System.currentTimeMillis();

        AuditDetails existing = cs.getAuditDetails() != null ? cs.getAuditDetails() : new AuditDetails();
        existing.setLastModifiedBy(userId);
        existing.setLastModifiedTime(now);
        cs.setAuditDetails(existing);
    }

    private String getUserId(ConfigSetRequest request) {
        if (request.getRequestInfo() != null && request.getRequestInfo().getUserInfo() != null
                && request.getRequestInfo().getUserInfo().getUuid() != null) {
            return request.getRequestInfo().getUserInfo().getUuid();
        }
        return "SYSTEM";
    }
}
