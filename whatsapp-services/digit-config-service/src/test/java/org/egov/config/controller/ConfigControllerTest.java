package org.egov.config.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.egov.config.web.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private RequestInfo buildRequestInfo() {
        return RequestInfo.builder()
                .apiId("config-service").ver("1.0").ts(System.currentTimeMillis()).msgId("test-msg")
                .userInfo(RequestInfo.UserInfo.builder().uuid("test-user").userName("testuser").build())
                .build();
    }

    // ==================== ConfigSet Tests ====================

    @Test
    void configSet_createAndSearch() throws Exception {
        ConfigSetRequest req = ConfigSetRequest.builder()
                .requestInfo(buildRequestInfo())
                .configSet(ConfigSet.builder()
                        .tenantId("tenant.cs1").name("Test Set").code("TEST_SET_1")
                        .description("Test config set").build())
                .build();

        mockMvc.perform(post("/config-set/v1/_create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.configSets[0].id", notNullValue()))
                .andExpect(jsonPath("$.configSets[0].code", is("TEST_SET_1")));

        ConfigSetSearchRequest searchReq = ConfigSetSearchRequest.builder()
                .requestInfo(buildRequestInfo())
                .criteria(ConfigSetSearchCriteria.builder().tenantId("tenant.cs1").build())
                .build();

        mockMvc.perform(post("/config-set/v1/_search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configSets", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void configSet_duplicateReturns400() throws Exception {
        ConfigSetRequest req = ConfigSetRequest.builder()
                .requestInfo(buildRequestInfo())
                .configSet(ConfigSet.builder()
                        .tenantId("tenant.dup").name("Dup Set").code("DUP_SET").build())
                .build();

        mockMvc.perform(post("/config-set/v1/_create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        mockMvc.perform(post("/config-set/v1/_create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.Errors[0].code", is("DUPLICATE_CONFIG_SET")));
    }

    @Test
    void configSet_activateDeactivatesPrevious() throws Exception {
        // Create two sets
        for (String code : List.of("SET_A", "SET_B")) {
            ConfigSetRequest req = ConfigSetRequest.builder()
                    .requestInfo(buildRequestInfo())
                    .configSet(ConfigSet.builder()
                            .tenantId("tenant.act").name(code).code(code).build())
                    .build();
            mockMvc.perform(post("/config-set/v1/_create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)));
        }

        // Search to get IDs
        ConfigSetSearchRequest search = ConfigSetSearchRequest.builder()
                .requestInfo(buildRequestInfo())
                .criteria(ConfigSetSearchCriteria.builder().tenantId("tenant.act").code("SET_A").build())
                .build();
        String resp = mockMvc.perform(post("/config-set/v1/_search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(search)))
                .andReturn().getResponse().getContentAsString();
        String setAId = objectMapper.readTree(resp).at("/configSets/0/id").asText();

        // Activate SET_A
        ConfigSetActivateRequest activateReq = ConfigSetActivateRequest.builder()
                .requestInfo(buildRequestInfo())
                .tenantId("tenant.act").configSetId(setAId)
                .build();

        mockMvc.perform(post("/config-set/v1/_activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    // ==================== Config Catalog Tests ====================

    @Test
    void config_createWithVersion() throws Exception {
        ObjectNode content = objectMapper.createObjectNode();
        content.put("key1", "value1");

        ConfigRequest req = ConfigRequest.builder()
                .requestInfo(buildRequestInfo())
                .config(Config.builder()
                        .tenantId("tenant.cfg1").namespace("runtime").configName("Test Config")
                        .configCode("CFG_001").status("ACTIVE").environment("dev")
                        .versions(List.of(ConfigVersion.builder()
                                .version("v1").content(content).build()))
                        .build())
                .build();

        mockMvc.perform(post("/v1/_create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.configs[0].id", notNullValue()))
                .andExpect(jsonPath("$.configs[0].versions[0].version", is("v1")));
    }

    @Test
    void config_searchWithVersions() throws Exception {
        ObjectNode content = objectMapper.createObjectNode();
        content.put("data", "searchable");

        ConfigRequest createReq = ConfigRequest.builder()
                .requestInfo(buildRequestInfo())
                .config(Config.builder()
                        .tenantId("tenant.srch").namespace("search-ns").configName("Search Config")
                        .configCode("SRCH_001").status("ACTIVE")
                        .versions(List.of(ConfigVersion.builder()
                                .version("v1").content(content).build()))
                        .build())
                .build();

        mockMvc.perform(post("/v1/_create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)));

        ConfigSearchRequest searchReq = ConfigSearchRequest.builder()
                .requestInfo(buildRequestInfo())
                .criteria(ConfigSearchCriteria.builder()
                        .tenantId("tenant.srch").namespace("search-ns").build())
                .build();

        mockMvc.perform(post("/v1/_search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configs", hasSize(1)))
                .andExpect(jsonPath("$.configs[0].versions", hasSize(1)))
                .andExpect(jsonPath("$.pagination.totalCount", is(1)));
    }

    @Test
    void config_updateAddsNewVersion() throws Exception {
        ObjectNode v1Content = objectMapper.createObjectNode();
        v1Content.put("val", "original");

        ConfigRequest createReq = ConfigRequest.builder()
                .requestInfo(buildRequestInfo())
                .config(Config.builder()
                        .tenantId("tenant.upd").namespace("update-ns").configName("Update Config")
                        .configCode("UPD_001").status("ACTIVE")
                        .versions(List.of(ConfigVersion.builder()
                                .version("v1").content(v1Content).build()))
                        .build())
                .build();

        String createResp = mockMvc.perform(post("/v1/_create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(createResp);
        String configId = node.at("/configs/0/id").asText();

        ObjectNode v2Content = objectMapper.createObjectNode();
        v2Content.put("val", "updated");

        ConfigRequest updateReq = ConfigRequest.builder()
                .requestInfo(buildRequestInfo())
                .config(Config.builder()
                        .id(configId).tenantId("tenant.upd").namespace("update-ns")
                        .configName("Update Config").configCode("UPD_001").status("ACTIVE")
                        .versions(List.of(ConfigVersion.builder()
                                .version("v2").content(v2Content).build()))
                        .auditDetails(AuditDetails.builder().createdBy("test-user").createdTime(System.currentTimeMillis()).build())
                        .build())
                .build();

        mockMvc.perform(post("/v1/_update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configs[0].versions[0].version", is("v2")));
    }

    // ==================== Resolve Tests ====================

    @Test
    void resolve_returnsActiveConfig() throws Exception {
        ObjectNode content = objectMapper.createObjectNode();
        content.put("setting", "resolved_value");

        ConfigRequest createReq = ConfigRequest.builder()
                .requestInfo(buildRequestInfo())
                .config(Config.builder()
                        .tenantId("tenant.resolve").namespace("resolve-ns").configName("Resolve Config")
                        .configCode("RESOLVE_001").status("ACTIVE").environment("prod")
                        .versions(List.of(ConfigVersion.builder()
                                .version("v1").content(content).build()))
                        .build())
                .build();

        mockMvc.perform(post("/v1/_create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)));

        ConfigResolveRequest resolveReq = ConfigResolveRequest.builder()
                .requestInfo(buildRequestInfo())
                .tenantId("tenant.resolve")
                .namespace("resolve-ns")
                .configCode("RESOLVE_001")
                .build();

        mockMvc.perform(post("/v1/_resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.setting", is("resolved_value")))
                .andExpect(jsonPath("$.version", is("v1")))
                .andExpect(jsonPath("$.resolvedFrom", is("tenant.resolve")));
    }

    @Test
    void resolve_tenantFallback() throws Exception {
        ObjectNode content = objectMapper.createObjectNode();
        content.put("level", "parent");

        ConfigRequest createReq = ConfigRequest.builder()
                .requestInfo(buildRequestInfo())
                .config(Config.builder()
                        .tenantId("state").namespace("fallback-ns").configName("Fallback Config")
                        .configCode("FB_001").status("ACTIVE")
                        .versions(List.of(ConfigVersion.builder()
                                .version("v1").content(content).build()))
                        .build())
                .build();

        mockMvc.perform(post("/v1/_create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)));

        ConfigResolveRequest resolveReq = ConfigResolveRequest.builder()
                .requestInfo(buildRequestInfo())
                .tenantId("state.city.ward")
                .namespace("fallback-ns")
                .configCode("FB_001")
                .build();

        mockMvc.perform(post("/v1/_resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolvedFrom", is("state")))
                .andExpect(jsonPath("$.content.level", is("parent")));
    }

    @Test
    void resolve_notFoundReturns400() throws Exception {
        ConfigResolveRequest resolveReq = ConfigResolveRequest.builder()
                .requestInfo(buildRequestInfo())
                .tenantId("nonexistent")
                .namespace("nope")
                .configCode("MISSING")
                .build();

        mockMvc.perform(post("/v1/_resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.Errors[0].code", is("CONFIG_NOT_RESOLVED")));
    }

    // ==================== Template Preview Tests ====================

    @Test
    void templatePreview_rendersPlaceholders() throws Exception {
        ObjectNode templateContent = objectMapper.createObjectNode();
        templateContent.put("template", "Hello {{name}}, your bill is Rs. {{amount}}.");

        ConfigRequest createReq = ConfigRequest.builder()
                .requestInfo(buildRequestInfo())
                .config(Config.builder()
                        .tenantId("tenant.tpl").namespace("templates").configName("Bill Template")
                        .configCode("BILL_TPL").status("ACTIVE")
                        .versions(List.of(ConfigVersion.builder()
                                .version("v1").content(templateContent).build()))
                        .build())
                .build();

        mockMvc.perform(post("/v1/_create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)));

        TemplatePreviewRequest previewReq = TemplatePreviewRequest.builder()
                .requestInfo(buildRequestInfo())
                .tenantId("tenant.tpl")
                .template(TemplateRef.builder()
                        .namespace("templates").configName("Bill Template").configCode("BILL_TPL").build())
                .locale("en_IN")
                .data(Map.of("name", "Lokendra", "amount", "500"))
                .build();

        mockMvc.perform(post("/v1/template/_preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(previewReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rendered", is("Hello Lokendra, your bill is Rs. 500.")))
                .andExpect(jsonPath("$.locale", is("en_IN")));
    }

    // ==================== Seed Data Tests ====================

    @Test
    void seedData_ootbTemplatesLoadedAndSearchable() throws Exception {
        ConfigSearchRequest searchReq = ConfigSearchRequest.builder()
                .requestInfo(buildRequestInfo())
                .criteria(ConfigSearchCriteria.builder()
                        .tenantId("default").namespace("OOTB_TEMPLATES").build())
                .build();

        mockMvc.perform(post("/v1/_search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configs", hasSize(4)))
                .andExpect(jsonPath("$.configs[*].namespace", everyItem(is("OOTB_TEMPLATES"))));
    }

    @Test
    void seedData_featureFlagsLoaded() throws Exception {
        ConfigSearchRequest searchReq = ConfigSearchRequest.builder()
                .requestInfo(buildRequestInfo())
                .criteria(ConfigSearchCriteria.builder()
                        .tenantId("default").namespace("FEATURE_FLAGS").build())
                .build();

        mockMvc.perform(post("/v1/_search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configs", hasSize(1)))
                .andExpect(jsonPath("$.configs[0].versions[0].content.flags.whatsapp_bidirectional_enabled", is(true)));
    }

    @Test
    void seedData_resolveOotbTemplate() throws Exception {
        ConfigResolveRequest resolveReq = ConfigResolveRequest.builder()
                .requestInfo(buildRequestInfo())
                .tenantId("default")
                .namespace("OOTB_TEMPLATES")
                .configCode("WELCOME_MSG")
                .build();

        mockMvc.perform(post("/v1/_resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.template", containsString("{{name}}")))
                .andExpect(jsonPath("$.version", is("v1")));
    }
}
