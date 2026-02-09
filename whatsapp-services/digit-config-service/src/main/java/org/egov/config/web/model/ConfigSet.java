package org.egov.config.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConfigSet {

    @JsonProperty("id")
    @Size(min = 1, max = 64)
    private String id;

    @JsonProperty("tenantId")
    @NotNull
    @Size(min = 2, max = 64)
    private String tenantId;

    @JsonProperty("name")
    @NotNull
    @Size(min = 2, max = 128)
    private String name;

    @JsonProperty("code")
    @NotNull
    @Size(min = 2, max = 128)
    private String code;

    @JsonProperty("description")
    @Size(max = 1024)
    private String description;

    @JsonProperty("status")
    @Size(min = 2, max = 32)
    @Builder.Default
    private String status = "INACTIVE";

    @JsonProperty("auditDetails")
    @Valid
    private AuditDetails auditDetails;
}
