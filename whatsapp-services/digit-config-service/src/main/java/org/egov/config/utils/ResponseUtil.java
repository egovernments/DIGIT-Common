package org.egov.config.utils;

import org.egov.config.web.model.*;

import java.util.List;

public class ResponseUtil {

    private ResponseUtil() {
    }

    public static ResponseInfo createResponseInfo(RequestInfo requestInfo, boolean success) {
        return ResponseInfo.builder()
                .apiId(requestInfo != null ? requestInfo.getApiId() : null)
                .ver(requestInfo != null ? requestInfo.getVer() : null)
                .ts(System.currentTimeMillis())
                .msgId(requestInfo != null ? requestInfo.getMsgId() : null)
                .resMsgId(requestInfo != null ? requestInfo.getMsgId() : null)
                .status(success ? "successful" : "failed")
                .build();
    }

    public static ConfigResponse getConfigResponse(RequestInfo requestInfo, List<Config> configs, Pagination pagination) {
        return ConfigResponse.builder()
                .responseInfo(createResponseInfo(requestInfo, true))
                .configs(configs)
                .pagination(pagination)
                .build();
    }

    public static ConfigSetActivateResponse getActivateResponse(RequestInfo requestInfo, String configSetId, String status) {
        return ConfigSetActivateResponse.builder()
                .responseInfo(createResponseInfo(requestInfo, true))
                .configSetId(configSetId)
                .status(status)
                .build();
    }

    public static TemplatePreviewResponse getTemplatePreviewResponse(RequestInfo requestInfo, String rendered, String locale) {
        return TemplatePreviewResponse.builder()
                .responseInfo(createResponseInfo(requestInfo, true))
                .rendered(rendered)
                .locale(locale)
                .build();
    }
}
