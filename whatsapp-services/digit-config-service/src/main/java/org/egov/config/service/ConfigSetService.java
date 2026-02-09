package org.egov.config.service;

import lombok.extern.slf4j.Slf4j;
import org.egov.config.config.ApplicationConfig;
import org.egov.config.repository.ConfigSetRepository;
import org.egov.config.service.enrichment.ConfigSetEnricher;
import org.egov.config.service.validator.ConfigSetValidator;
import org.egov.config.web.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ConfigSetService {

    private final ConfigSetValidator validator;
    private final ConfigSetEnricher enricher;
    private final ConfigSetRepository repository;
    private final ApplicationConfig config;

    @Autowired
    public ConfigSetService(ConfigSetValidator validator, ConfigSetEnricher enricher,
                            ConfigSetRepository repository, ApplicationConfig config) {
        this.validator = validator;
        this.enricher = enricher;
        this.repository = repository;
        this.config = config;
    }

    public List<ConfigSet> create(ConfigSetRequest request) {
        validator.validateCreateRequest(request);
        enricher.enrichCreateRequest(request);
        repository.create(request.getConfigSet());
        log.info("ConfigSet created: id={}, code={}", request.getConfigSet().getId(), request.getConfigSet().getCode());
        return Collections.singletonList(request.getConfigSet());
    }

    public List<ConfigSet> update(ConfigSetRequest request) {
        validator.validateUpdateRequest(request);
        enricher.enrichUpdateRequest(request);
        repository.update(request.getConfigSet());
        log.info("ConfigSet updated: id={}", request.getConfigSet().getId());
        return Collections.singletonList(request.getConfigSet());
    }

    public ConfigSetActivateResponse activate(ConfigSetActivateRequest request) {
        validator.validateActivateRequest(request);

        String userId = "SYSTEM";
        if (request.getRequestInfo() != null && request.getRequestInfo().getUserInfo() != null
                && request.getRequestInfo().getUserInfo().getUuid() != null) {
            userId = request.getRequestInfo().getUserInfo().getUuid();
        }

        String previousActiveSetId = repository.findActiveSetId(request.getTenantId());

        repository.deactivateOthers(request.getConfigSetId(), request.getTenantId(), userId);
        repository.activate(request.getConfigSetId(), request.getTenantId(), userId);

        ConfigSetActivation activation = ConfigSetActivation.builder()
                .id(UUID.randomUUID().toString())
                .configSetId(request.getConfigSetId())
                .tenantId(request.getTenantId())
                .activatedBy(userId)
                .activatedTime(System.currentTimeMillis())
                .previousActiveSetId(previousActiveSetId)
                .build();
        repository.recordActivation(activation);

        log.info("ConfigSet activated: id={}, tenant={}, previousActive={}",
                request.getConfigSetId(), request.getTenantId(), previousActiveSetId);

        return ConfigSetActivateResponse.builder()
                .configSetId(request.getConfigSetId())
                .status("ACTIVE")
                .build();
    }

    public List<ConfigSet> search(ConfigSetSearchRequest request) {
        if (request.getCriteria() == null) {
            return repository.search(ConfigSetSearchCriteria.builder().build());
        }
        return repository.search(request.getCriteria());
    }

    public Pagination getSearchPagination(ConfigSetSearchCriteria criteria) {
        Long totalCount = repository.getCount(criteria != null ? criteria : ConfigSetSearchCriteria.builder().build());
        return Pagination.builder()
                .totalCount(totalCount)
                .limit(criteria != null && criteria.getLimit() != null ? criteria.getLimit() : config.getDefaultLimit())
                .offSet(criteria != null && criteria.getOffset() != null ? criteria.getOffset() : config.getDefaultOffset())
                .build();
    }
}
