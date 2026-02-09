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
public class ConfigSetSearchCriteria {

    @JsonProperty("tenantId")
    @Size(min = 2, max = 64)
    private String tenantId;

    @JsonProperty("name")
    @Size(min = 2, max = 128)
    private String name;

    @JsonProperty("code")
    @Size(min = 2, max = 128)
    private String code;

    @JsonProperty("status")
    @Size(min = 2, max = 32)
    private String status;

    @JsonProperty("limit")
    private Integer limit;

    @JsonProperty("offset")
    private Integer offset;
}
