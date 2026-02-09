package org.egov.config.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConfigSearchCriteria {

    @JsonProperty("tenantId")
    @Size(min = 2, max = 64)
    private String tenantId;

    @JsonProperty("namespace")
    @Size(min = 2, max = 128)
    private String namespace;

    @JsonProperty("configName")
    @Size(min = 2, max = 128)
    private String configName;

    @JsonProperty("configCode")
    @Size(min = 2, max = 128)
    private String configCode;

    @JsonProperty("environment")
    @Size(min = 2, max = 32)
    private String environment;

    @JsonProperty("status")
    @Size(min = 2, max = 32)
    private String status;

    @JsonProperty("version")
    @Size(min = 1, max = 64)
    private String version;

    @JsonProperty("includeContent")
    @Builder.Default
    private Boolean includeContent = true;

    @JsonProperty("limit")
    private Integer limit;

    @JsonProperty("offset")
    private Integer offset;
}
