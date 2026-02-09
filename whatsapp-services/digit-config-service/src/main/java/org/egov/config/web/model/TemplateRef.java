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
public class TemplateRef {

    @JsonProperty("namespace")
    @Size(min = 2, max = 128)
    private String namespace;

    @JsonProperty("configName")
    @Size(min = 2, max = 128)
    private String configName;

    @JsonProperty("configCode")
    @Size(min = 2, max = 128)
    private String configCode;

    @JsonProperty("version")
    @Size(min = 1, max = 64)
    private String version;
}
