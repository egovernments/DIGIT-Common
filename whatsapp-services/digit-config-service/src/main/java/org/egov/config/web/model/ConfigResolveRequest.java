package org.egov.config.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConfigResolveRequest {

    @JsonProperty("RequestInfo")
    @Valid
    private RequestInfo requestInfo;

    @JsonProperty("tenantId")
    @NotNull
    @Size(min = 2, max = 64)
    private String tenantId;

    @JsonProperty("namespace")
    @NotNull
    @Size(min = 2, max = 128)
    private String namespace;

    @JsonProperty("configCode")
    @NotNull
    @Size(min = 2, max = 128)
    private String configCode;

    @JsonProperty("context")
    private Map<String, String> context;

    @JsonProperty("environment")
    @Size(min = 2, max = 32)
    private String environment;
}
