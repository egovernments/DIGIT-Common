package org.egov.config.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.egov.config.utils.CustomException;
import org.egov.config.web.model.ConfigVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SchemaValidationService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final Map<String, JsonNode> schemaCache = new ConcurrentHashMap<>();

    @Value("${mdms.schema.host:}")
    private String mdmsHost;

    @Value("${mdms.schema.search.path:/mdms-v2/schema/v1/_search}")
    private String mdmsSchemaSearchPath;

    @Autowired
    public SchemaValidationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    public void validateContent(ConfigVersion version) {
        if (!StringUtils.hasText(version.getSchemaRef()) || version.getContent() == null) {
            return;
        }

        JsonNode schema = getSchema(version.getSchemaRef());
        if (schema == null) {
            log.warn("Schema not found for ref: {}, skipping validation", version.getSchemaRef());
            return;
        }

        Map<String, String> errors = new HashMap<>();
        validateAgainstSchema(version.getContent(), schema, errors, "");

        if (!errors.isEmpty()) {
            throw new CustomException(errors);
        }

        log.info("Content validated against schema: {}", version.getSchemaRef());
    }

    private JsonNode getSchema(String schemaRef) {
        return schemaCache.computeIfAbsent(schemaRef, this::fetchSchema);
    }

    private JsonNode fetchSchema(String schemaRef) {
        if (!StringUtils.hasText(mdmsHost)) {
            log.debug("MDMS host not configured, returning null schema for ref: {}", schemaRef);
            return null;
        }

        try {
            String url = mdmsHost + mdmsSchemaSearchPath;
            Map<String, Object> requestBody = Map.of(
                    "RequestInfo", Map.of("apiId", "config-service"),
                    "SchemaDefCriteria", Map.of("tenantId", "default", "codes", new String[]{schemaRef})
            );
            String response = restTemplate.postForObject(url, requestBody, String.class);
            JsonNode responseNode = objectMapper.readTree(response);
            JsonNode definitions = responseNode.path("SchemaDefinitions");
            if (definitions.isArray() && !definitions.isEmpty()) {
                return definitions.get(0).path("definition");
            }
        } catch (Exception e) {
            log.error("Failed to fetch schema from MDMS for ref: {}", schemaRef, e);
        }
        return null;
    }

    private void validateAgainstSchema(JsonNode content, JsonNode schema, Map<String, String> errors, String path) {
        JsonNode requiredNode = schema.path("required");
        if (requiredNode.isArray()) {
            for (JsonNode req : requiredNode) {
                String fieldName = req.asText();
                if (!content.has(fieldName) || content.get(fieldName).isNull()) {
                    errors.put("SCHEMA_VALIDATION_" + fieldName,
                            "Required field '" + path + fieldName + "' is missing");
                }
            }
        }

        JsonNode propertiesNode = schema.path("properties");
        if (propertiesNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = propertiesNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                JsonNode fieldSchema = field.getValue();
                JsonNode fieldValue = content.get(fieldName);

                if (fieldValue != null && !fieldValue.isNull()) {
                    validateFieldType(fieldValue, fieldSchema, errors, path + fieldName);
                }
            }
        }
    }

    private void validateFieldType(JsonNode value, JsonNode fieldSchema, Map<String, String> errors, String path) {
        String expectedType = fieldSchema.path("type").asText("");

        boolean valid = switch (expectedType) {
            case "string" -> value.isTextual();
            case "integer" -> value.isInt() || value.isLong();
            case "number" -> value.isNumber();
            case "boolean" -> value.isBoolean();
            case "array" -> value.isArray();
            case "object" -> value.isObject();
            default -> true;
        };

        if (!valid) {
            errors.put("SCHEMA_VALIDATION_TYPE_" + path,
                    "Field '" + path + "' expected type '" + expectedType + "' but got '" + value.getNodeType() + "'");
        }

        if ("string".equals(expectedType) && value.isTextual()) {
            int maxLength = fieldSchema.path("maxLength").asInt(0);
            int minLength = fieldSchema.path("minLength").asInt(0);
            String text = value.asText();
            if (maxLength > 0 && text.length() > maxLength) {
                errors.put("SCHEMA_VALIDATION_MAXLEN_" + path,
                        "Field '" + path + "' exceeds maxLength " + maxLength);
            }
            if (text.length() < minLength) {
                errors.put("SCHEMA_VALIDATION_MINLEN_" + path,
                        "Field '" + path + "' is shorter than minLength " + minLength);
            }
        }
    }

    public void evictCache(String schemaRef) {
        schemaCache.remove(schemaRef);
    }

    public void evictAllCache() {
        schemaCache.clear();
    }
}
