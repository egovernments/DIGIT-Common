package org.egov.config.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConfigSetActivation {

    @JsonProperty("id")
    private String id;

    @JsonProperty("configSetId")
    private String configSetId;

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("activatedBy")
    private String activatedBy;

    @JsonProperty("activatedTime")
    private Long activatedTime;

    @JsonProperty("previousActiveSetId")
    private String previousActiveSetId;
}
