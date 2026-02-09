package org.egov.config.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
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
public class ConfigVersion {

    @JsonProperty("id")
    @Size(min = 1, max = 64)
    private String id;

    @JsonProperty("configId")
    @Size(min = 1, max = 64)
    private String configId;

    @JsonProperty("version")
    @NotNull
    @Size(min = 1, max = 64)
    private String version;

    @JsonProperty("content")
    private JsonNode content;

    @JsonProperty("schemaRef")
    @Size(max = 256)
    private String schemaRef;

    @JsonProperty("status")
    @Size(min = 2, max = 32)
    @Builder.Default
    private String status = "ACTIVE";

    @JsonProperty("auditDetails")
    @Valid
    private AuditDetails auditDetails;
}
