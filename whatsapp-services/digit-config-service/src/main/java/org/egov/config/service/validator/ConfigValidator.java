package org.egov.config.service.validator;

import org.egov.config.repository.ConfigRepository;
import org.egov.config.utils.CustomException;
import org.egov.config.web.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ConfigValidator {

    private final ConfigRepository configRepository;

    @Autowired
    public ConfigValidator(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    public void validateCreateRequest(ConfigRequest request) {
        Map<String, String> errors = new HashMap<>();
        Config config = request.getConfig();

        if (config == null) throw new CustomException("INVALID_REQUEST", "Config object is required");

        validateRequiredFields(config, errors);

        if (errors.isEmpty()) {
            List<Config> existing = configRepository.search(ConfigSearchCriteria.builder()
                    .tenantId(config.getTenantId())
                    .namespace(config.getNamespace())
                    .configCode(config.getConfigCode())
                    .build());
            if (!CollectionUtils.isEmpty(existing)) {
                errors.put("DUPLICATE_CONFIG", "Config with same tenantId, namespace and configCode already exists. Use update to add a new version.");
            }
        }

        throwIfErrors(errors);
    }

    public void validateUpdateRequest(ConfigRequest request) {
        Map<String, String> errors = new HashMap<>();
        Config config = request.getConfig();

        if (config == null) throw new CustomException("INVALID_REQUEST", "Config object is required");
        if (!StringUtils.hasText(config.getId())) errors.put("MISSING_ID", "Config id is required for update");

        validateRequiredFields(config, errors);
        throwIfErrors(errors);
    }

    public void validateSearchRequest(ConfigSearchRequest request) {
        if (request.getCriteria() == null) {
            throw new CustomException("INVALID_REQUEST", "Search criteria is required");
        }
    }

    public void validateTemplatePreviewRequest(TemplatePreviewRequest request) {
        Map<String, String> errors = new HashMap<>();
        if (!StringUtils.hasText(request.getTenantId())) errors.put("MISSING_TENANT_ID", "Tenant ID is required");
        if (request.getTemplate() == null) errors.put("MISSING_TEMPLATE", "Template reference is required");
        throwIfErrors(errors);
    }

    public void validateResolveRequest(ConfigResolveRequest request) {
        Map<String, String> errors = new HashMap<>();
        if (!StringUtils.hasText(request.getTenantId())) errors.put("MISSING_TENANT_ID", "Tenant ID is required");
        if (!StringUtils.hasText(request.getNamespace())) errors.put("MISSING_NAMESPACE", "Namespace is required");
        if (!StringUtils.hasText(request.getConfigCode())) errors.put("MISSING_CONFIG_CODE", "Config code is required");
        throwIfErrors(errors);
    }

    private void validateRequiredFields(Config config, Map<String, String> errors) {
        if (!StringUtils.hasText(config.getTenantId())) errors.put("MISSING_TENANT_ID", "Tenant ID is required");
        if (!StringUtils.hasText(config.getNamespace())) errors.put("MISSING_NAMESPACE", "Namespace is required");
        if (!StringUtils.hasText(config.getConfigName())) errors.put("MISSING_CONFIG_NAME", "Config name is required");
        if (!StringUtils.hasText(config.getConfigCode())) errors.put("MISSING_CONFIG_CODE", "Config code is required");
        if (!StringUtils.hasText(config.getStatus())) errors.put("MISSING_STATUS", "Status is required");
    }

    private void throwIfErrors(Map<String, String> errors) {
        if (!errors.isEmpty()) throw new CustomException(errors);
    }
}
