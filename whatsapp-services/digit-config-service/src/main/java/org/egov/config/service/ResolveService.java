package org.egov.config.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.egov.config.repository.ConfigRepository;
import org.egov.config.service.validator.ConfigValidator;
import org.egov.config.utils.CustomException;
import org.egov.config.web.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ResolveService {

    private final ConfigRepository repository;
    private final ConfigValidator validator;

    @Autowired
    public ResolveService(ConfigRepository repository, ConfigValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    public ConfigResolveResponse resolve(ConfigResolveRequest request) {
        validator.validateResolveRequest(request);

        List<String> tenantFallback = buildTenantFallback(request.getTenantId());

        for (String tenant : tenantFallback) {
            ConfigSearchCriteria criteria = ConfigSearchCriteria.builder()
                    .tenantId(tenant)
                    .namespace(request.getNamespace())
                    .configCode(request.getConfigCode())
                    .status("ACTIVE")
                    .build();

            if (StringUtils.hasText(request.getEnvironment())) {
                criteria.setEnvironment(request.getEnvironment());
            }

            List<Config> configs = repository.search(criteria);

            if (!CollectionUtils.isEmpty(configs)) {
                Config matched = selectBestMatch(configs, request.getContext());
                List<ConfigVersion> activeVersions = repository.getActiveVersion(matched.getId());

                if (!CollectionUtils.isEmpty(activeVersions)) {
                    ConfigVersion version = activeVersions.get(0);
                    log.info("Resolved config: tenant={}, ns={}, code={}, version={}, resolvedFrom={}",
                            request.getTenantId(), request.getNamespace(), request.getConfigCode(),
                            version.getVersion(), tenant);

                    return ConfigResolveResponse.builder()
                            .tenantId(request.getTenantId())
                            .namespace(request.getNamespace())
                            .configCode(request.getConfigCode())
                            .version(version.getVersion())
                            .content(version.getContent())
                            .resolvedFrom(tenant)
                            .build();
                }
            }
        }

        throw new CustomException("CONFIG_NOT_RESOLVED",
                "No active config found for tenantId=" + request.getTenantId()
                        + ", namespace=" + request.getNamespace()
                        + ", configCode=" + request.getConfigCode());
    }

    private List<String> buildTenantFallback(String tenantId) {
        List<String> fallback = new ArrayList<>();
        fallback.add(tenantId);
        String current = tenantId;
        while (current.contains(".")) {
            current = current.substring(0, current.lastIndexOf("."));
            fallback.add(current);
        }
        return fallback;
    }

    private Config selectBestMatch(List<Config> configs, Map<String, String> context) {
        if (context == null || context.isEmpty() || configs.size() == 1) {
            return configs.get(0);
        }

        Config bestMatch = null;
        int bestScore = -1;

        for (Config config : configs) {
            int score = computeContextScore(config, context);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = config;
            }
        }

        return bestMatch != null ? bestMatch : configs.get(0);
    }

    private int computeContextScore(Config config, Map<String, String> context) {
        int score = 0;
        if (config.getEnvironment() != null && config.getEnvironment().equals(context.get("environment"))) {
            score += 10;
        }
        if (config.getDescription() != null) {
            for (Map.Entry<String, String> entry : context.entrySet()) {
                if (config.getDescription().toLowerCase().contains(entry.getValue().toLowerCase())) {
                    score += 1;
                }
            }
        }
        return score;
    }
}
