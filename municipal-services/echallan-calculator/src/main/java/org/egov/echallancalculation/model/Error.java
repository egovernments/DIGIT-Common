/*
 * eChallan System
 * ### API Specs For eChallan System ### 1. Generate the new challan. 2. Update the details of existing challan 3. Search the existing challan 4. Generate the demand and bill for the challan amount so that collection can be done in online and offline mode. 
 *
 * OpenAPI spec version: 1.0.0
 * Contact: contact@egovernments.org
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package org.egov.echallancalculation.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Error object will be returned as a part of reponse body in conjunction with ResponseHeader as part of ErrorResponse whenever the request processing status in the ResponseHeader is FAILED. HTTP return in this scenario will usually be HTTP 400.
 */
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2020-08-10T16:46:24.044+05:30[Asia/Calcutta]")
@Getter 
@Setter
@AllArgsConstructor
@NoArgsConstructor 
@Builder
public class Error {

  @JsonProperty("code")

  private String code = null;

  @JsonProperty("message")

  private String message = null;

  @JsonProperty("description")

  private String description = null;

  @JsonProperty("params")

  private List<String> params = null;
 
}
