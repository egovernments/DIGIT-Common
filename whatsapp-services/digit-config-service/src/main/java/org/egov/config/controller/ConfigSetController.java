package org.egov.config.controller;

import jakarta.validation.Valid;
import org.egov.config.service.ConfigSetService;
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
@RequestMapping("/config-set/v1")
public class ConfigSetController {

    private final ConfigSetService configSetService;

    @Autowired
    public ConfigSetController(ConfigSetService configSetService) {
        this.configSetService = configSetService;
    }

    @PostMapping("/_create")
    public ResponseEntity<ConfigSetResponse> create(@Valid @RequestBody ConfigSetRequest request) {
        List<ConfigSet> configSets = configSetService.create(request);
        ConfigSetResponse response = ConfigSetResponse.builder()
                .responseInfo(ResponseUtil.createResponseInfo(request.getRequestInfo(), true))
                .configSets(configSets)
                .build();
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/_update")
    public ResponseEntity<ConfigSetResponse> update(@Valid @RequestBody ConfigSetRequest request) {
        List<ConfigSet> configSets = configSetService.update(request);
        ConfigSetResponse response = ConfigSetResponse.builder()
                .responseInfo(ResponseUtil.createResponseInfo(request.getRequestInfo(), true))
                .configSets(configSets)
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/_activate")
    public ResponseEntity<ConfigSetActivateResponse> activate(@Valid @RequestBody ConfigSetActivateRequest request) {
        ConfigSetActivateResponse result = configSetService.activate(request);
        result.setResponseInfo(ResponseUtil.createResponseInfo(request.getRequestInfo(), true));
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/_search")
    public ResponseEntity<ConfigSetResponse> search(@Valid @RequestBody ConfigSetSearchRequest request) {
        List<ConfigSet> configSets = configSetService.search(request);
        Pagination pagination = configSetService.getSearchPagination(request.getCriteria());
        ConfigSetResponse response = ConfigSetResponse.builder()
                .responseInfo(ResponseUtil.createResponseInfo(request.getRequestInfo(), true))
                .configSets(configSets)
                .pagination(pagination)
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
