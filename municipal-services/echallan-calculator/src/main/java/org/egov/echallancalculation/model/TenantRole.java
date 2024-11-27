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
import java.util.ArrayList;
import java.util.List;
import org.egov.common.contract.request.Role;

/**
 * User role carries the tenant related role information for the user. A user can have multiple roles per tenant based on the need of the tenant. A user may also have multiple roles for multiple tenants.
 */
@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.JavaClientCodegen", date = "2020-08-10T16:46:24.044+05:30[Asia/Calcutta]")public class TenantRole {

  @JsonProperty("tenantId")

  private String tenantId = null;

  @JsonProperty("roles")

  private List<Role> roles = new ArrayList<Role>();
  public TenantRole tenantId(String tenantId) {
    this.tenantId = tenantId;
    return this;
  }

  

  /**
  * tenantid for the tenant
  * @return tenantId
  **/
  public String getTenantId() {
    return tenantId;
  }
  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }
  public TenantRole roles(List<Role> roles) {
    this.roles = roles;
    return this;
  }

  public TenantRole addRolesItem(Role rolesItem) {
    this.roles.add(rolesItem);
    return this;
  }

  /**
  * Roles assigned for a particular tenant - array of role codes/names
  * @return roles
  **/
  public List<Role> getRoles() {
    return roles;
  }
  public void setRoles(List<Role> roles) {
    this.roles = roles;
  }
  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TenantRole tenantRole = (TenantRole) o;
    return Objects.equals(this.tenantId, tenantRole.tenantId) &&
        Objects.equals(this.roles, tenantRole.roles);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(tenantId, roles);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TenantRole {\n");
    
    sb.append("    tenantId: ").append(toIndentedString(tenantId)).append("\n");
    sb.append("    roles: ").append(toIndentedString(roles)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}
