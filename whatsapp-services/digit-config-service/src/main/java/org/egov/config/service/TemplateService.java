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

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TemplateService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");

    private final ConfigRepository repository;
    private final ConfigValidator validator;

    @Autowired
    public TemplateService(ConfigRepository repository, ConfigValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    public TemplatePreviewResponse preview(TemplatePreviewRequest request) {
        validator.validateTemplatePreviewRequest(request);
        TemplateRef ref = request.getTemplate();

        ConfigSearchCriteria criteria = ConfigSearchCriteria.builder()
                .tenantId(request.getTenantId())
                .namespace(ref.getNamespace())
                .configName(ref.getConfigName())
                .configCode(ref.getConfigCode())
                .build();

        List<Config> configs = repository.search(criteria);
        if (CollectionUtils.isEmpty(configs)) {
            throw new CustomException("TEMPLATE_NOT_FOUND", "No template config found for the given reference");
        }

        Config templateConfig = configs.get(0);
        List<ConfigVersion> versions = repository.getActiveVersion(templateConfig.getId());

        if (CollectionUtils.isEmpty(versions) || versions.get(0).getContent() == null) {
            throw new CustomException("TEMPLATE_CONTENT_EMPTY", "Template config has no active version with content");
        }

        JsonNode content = versions.get(0).getContent();
        String templateText = extractTemplateText(content);
        String rendered = renderTemplate(templateText, request.getData());
        String locale = request.getLocale() != null ? request.getLocale() : "en_IN";

        log.info("Template preview rendered for configCode: {}, locale: {}", ref.getConfigCode(), locale);

        return TemplatePreviewResponse.builder()
                .rendered(rendered)
                .locale(locale)
                .build();
    }

    private String extractTemplateText(JsonNode content) {
        if (content.has("template")) return content.get("template").asText();
        if (content.has("body")) return content.get("body").asText();
        return content.toString();
    }

    private String renderTemplate(String template, Map<String, Object> data) {
        if (data == null || data.isEmpty()) return template;

        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = data.get(key);
            String replacement = value != null ? Matcher.quoteReplacement(value.toString()) : matcher.group(0);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
