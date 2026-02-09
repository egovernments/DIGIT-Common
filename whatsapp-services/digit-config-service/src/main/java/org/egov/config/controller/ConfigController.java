package org.egov.config.controller;

import jakarta.validation.Valid;
import org.egov.config.service.ConfigService;
import org.egov.config.service.ResolveService;
import org.egov.config.service.TemplateService;
import org.egov.config.utils.ResponseUtil;
import org.egov.config.web.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1")
public class ConfigController {

    private final ConfigService configService;
    private final TemplateService templateService;
    private final ResolveService resolveService;

    @Autowired
    public ConfigController(ConfigService configService, TemplateService templateService,
                            ResolveService resolveService) {
        this.configService = configService;
        this.templateService = templateService;
        this.resolveService = resolveService;
    }

    @PostMapping("/_create")
    public ResponseEntity<ConfigResponse> createConfig(@Valid @RequestBody ConfigRequest request) {
        List<Config> configs = configService.create(request);
        ConfigResponse response = ResponseUtil.getConfigResponse(request.getRequestInfo(), configs, null);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/_update")
    public ResponseEntity<ConfigResponse> updateConfig(@Valid @RequestBody ConfigRequest request) {
        List<Config> configs = configService.update(request);
        ConfigResponse response = ResponseUtil.getConfigResponse(request.getRequestInfo(), configs, null);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/_search")
    public ResponseEntity<ConfigResponse> searchConfigs(@Valid @RequestBody ConfigSearchRequest request) {
        List<Config> configs = configService.search(request);
        Pagination pagination = configService.getSearchPagination(request.getCriteria());
        ConfigResponse response = ResponseUtil.getConfigResponse(request.getRequestInfo(), configs, pagination);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/_resolve")
    public ResponseEntity<ConfigResolveResponse> resolveConfig(@Valid @RequestBody ConfigResolveRequest request) {
        ConfigResolveResponse result = resolveService.resolve(request);
        result.setResponseInfo(ResponseUtil.createResponseInfo(request.getRequestInfo(), true));
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/template/_preview")
    public ResponseEntity<TemplatePreviewResponse> previewTemplate(
            @Valid @RequestBody TemplatePreviewRequest request) {
        TemplatePreviewResponse result = templateService.preview(request);
        result.setResponseInfo(ResponseUtil.createResponseInfo(request.getRequestInfo(), true));
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
