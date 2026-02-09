package org.egov.config.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Config {

    @JsonProperty("id")
    @Size(min = 1, max = 64)
    private String id;

    @JsonProperty("configSetId")
    @Size(max = 64)
    private String configSetId;

    @JsonProperty("tenantId")
    @NotNull
    @Size(min = 2, max = 64)
    private String tenantId;

    @JsonProperty("namespace")
    @NotNull
    @Size(min = 2, max = 128)
    private String namespace;

    @JsonProperty("configName")
    @NotNull
    @Size(min = 2, max = 128)
    private String configName;

    @JsonProperty("configCode")
    @NotNull
    @Size(min = 2, max = 128)
    private String configCode;

    @JsonProperty("environment")
    @Size(min = 2, max = 32)
    private String environment;

    @JsonProperty("description")
    @Size(max = 1024)
    private String description;

    @JsonProperty("status")
    @NotNull
    @Size(min = 2, max = 32)
    private String status;

    @JsonProperty("versions")
    @Valid
    private List<ConfigVersion> versions;

    @JsonProperty("auditDetails")
    @Valid
    private AuditDetails auditDetails;
}
