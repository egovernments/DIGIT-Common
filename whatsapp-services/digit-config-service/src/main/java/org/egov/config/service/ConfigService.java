package org.egov.config.service;

import lombok.extern.slf4j.Slf4j;
import org.egov.config.config.ApplicationConfig;
import org.egov.config.repository.ConfigRepository;
import org.egov.config.service.enrichment.ConfigEnricher;
import org.egov.config.service.validator.ConfigValidator;
import org.egov.config.web.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class ConfigService {

    private final ConfigValidator validator;
    private final ConfigEnricher enricher;
    private final ConfigRepository repository;
    private final ApplicationConfig config;

    @Autowired
    public ConfigService(ConfigValidator validator, ConfigEnricher enricher,
                         ConfigRepository repository, ApplicationConfig config) {
        this.validator = validator;
        this.enricher = enricher;
        this.repository = repository;
        this.config = config;
    }

    public List<Config> create(ConfigRequest request) {
        validator.validateCreateRequest(request);
        enricher.enrichCreateRequest(request);

        Config cfg = request.getConfig();
        repository.createConfig(cfg);

        if (!CollectionUtils.isEmpty(cfg.getVersions())) {
            for (ConfigVersion v : cfg.getVersions()) {
                repository.createVersion(v);
            }
        }

        log.info("Config created: id={}, code={}", cfg.getId(), cfg.getConfigCode());
        return Collections.singletonList(cfg);
    }

    public List<Config> update(ConfigRequest request) {
        validator.validateUpdateRequest(request);
        enricher.enrichUpdateRequest(request);

        Config cfg = request.getConfig();
        repository.updateConfig(cfg);

        if (!CollectionUtils.isEmpty(cfg.getVersions())) {
            String userId = cfg.getAuditDetails().getLastModifiedBy();
            for (ConfigVersion v : cfg.getVersions()) {
                if (v.getId() == null || v.getId().isEmpty()) {
                    repository.deactivateVersions(cfg.getId(), userId);
                    repository.createVersion(v);
                }
            }
        }

        log.info("Config updated: id={}", cfg.getId());
        return Collections.singletonList(cfg);
    }

    public List<Config> search(ConfigSearchRequest request) {
        validator.validateSearchRequest(request);
        ConfigSearchCriteria criteria = request.getCriteria();
        List<Config> configs = repository.search(criteria);

        boolean includeContent = criteria.getIncludeContent() == null || criteria.getIncludeContent();
        if (includeContent) {
            for (Config c : configs) {
                c.setVersions(repository.getVersionsByConfigId(c.getId()));
            }
        }

        return configs;
    }

    public Pagination getSearchPagination(ConfigSearchCriteria criteria) {
        Long totalCount = repository.getCount(criteria);
        return Pagination.builder()
                .totalCount(totalCount)
                .limit(criteria.getLimit() != null ? criteria.getLimit() : config.getDefaultLimit())
                .offSet(criteria.getOffset() != null ? criteria.getOffset() : config.getDefaultOffset())
                .build();
    }
}
