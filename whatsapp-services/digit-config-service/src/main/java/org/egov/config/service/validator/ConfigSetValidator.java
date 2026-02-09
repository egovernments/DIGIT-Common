package org.egov.config.service.validator;

import org.egov.config.repository.ConfigSetRepository;
import org.egov.config.utils.CustomException;
import org.egov.config.web.model.ConfigSet;
import org.egov.config.web.model.ConfigSetRequest;
import org.egov.config.web.model.ConfigSetSearchCriteria;
import org.egov.config.web.model.ConfigSetActivateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ConfigSetValidator {

    private final ConfigSetRepository repository;

    @Autowired
    public ConfigSetValidator(ConfigSetRepository repository) {
        this.repository = repository;
    }

    public void validateCreateRequest(ConfigSetRequest request) {
        Map<String, String> errors = new HashMap<>();
        ConfigSet cs = request.getConfigSet();

        if (cs == null) throw new CustomException("INVALID_REQUEST", "ConfigSet object is required");
        if (!StringUtils.hasText(cs.getTenantId())) errors.put("MISSING_TENANT_ID", "Tenant ID is required");
        if (!StringUtils.hasText(cs.getName())) errors.put("MISSING_NAME", "Name is required");
        if (!StringUtils.hasText(cs.getCode())) errors.put("MISSING_CODE", "Code is required");

        if (errors.isEmpty()) {
            List<ConfigSet> existing = repository.search(ConfigSetSearchCriteria.builder()
                    .tenantId(cs.getTenantId()).code(cs.getCode()).build());
            if (!CollectionUtils.isEmpty(existing)) {
                errors.put("DUPLICATE_CONFIG_SET", "Config set with code '" + cs.getCode() + "' already exists for this tenant");
            }
        }

        if (!errors.isEmpty()) throw new CustomException(errors);
    }

    public void validateUpdateRequest(ConfigSetRequest request) {
        Map<String, String> errors = new HashMap<>();
        ConfigSet cs = request.getConfigSet();

        if (cs == null) throw new CustomException("INVALID_REQUEST", "ConfigSet object is required");
        if (!StringUtils.hasText(cs.getId())) errors.put("MISSING_ID", "Config set ID is required for update");
        if (!StringUtils.hasText(cs.getTenantId())) errors.put("MISSING_TENANT_ID", "Tenant ID is required");

        if (!errors.isEmpty()) throw new CustomException(errors);
    }

    public void validateActivateRequest(ConfigSetActivateRequest request) {
        Map<String, String> errors = new HashMap<>();
        if (!StringUtils.hasText(request.getTenantId())) errors.put("MISSING_TENANT_ID", "Tenant ID is required");
        if (!StringUtils.hasText(request.getConfigSetId())) errors.put("MISSING_CONFIG_SET_ID", "Config set ID is required");

        if (errors.isEmpty()) {
            List<ConfigSet> existing = repository.search(ConfigSetSearchCriteria.builder()
                    .tenantId(request.getTenantId()).build());
            boolean found = existing.stream().anyMatch(cs -> cs.getId().equals(request.getConfigSetId()));
            if (!found) {
                errors.put("CONFIG_SET_NOT_FOUND", "Config set with id '" + request.getConfigSetId() + "' not found");
            }
        }

        if (!errors.isEmpty()) throw new CustomException(errors);
    }
}
