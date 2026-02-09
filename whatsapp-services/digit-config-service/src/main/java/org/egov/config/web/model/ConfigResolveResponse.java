package org.egov.config.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConfigResolveResponse {

    @JsonProperty("ResponseInfo")
    @Valid
    private ResponseInfo responseInfo;

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("configCode")
    private String configCode;

    @JsonProperty("version")
    private String version;

    @JsonProperty("content")
    private JsonNode content;

    @JsonProperty("resolvedFrom")
    private String resolvedFrom;
}
