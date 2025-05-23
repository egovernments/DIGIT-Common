/*
 * eGov suite of products aim to improve the internal efficiency,transparency,
 * accountability and the service delivery of the government  organizations.
 *
 *  Copyright (C) 2016  eGovernments Foundation
 *
 *  The updated version of eGov suite of products as by eGovernments Foundation
 *  is available at http://www.egovernments.org
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see http://www.gnu.org/licenses/ or
 *  http://www.gnu.org/licenses/gpl.html .
 *
 *  In addition to the terms of the GPL license to be adhered to in using this
 *  program, the following additional terms are to be complied with:
 *
 *      1) All versions of this program, verbatim or modified must carry this
 *         Legal Notice.
 *
 *      2) Any misrepresentation of the origin of the material is prohibited. It
 *         is required that all modified versions of this material be marked in
 *         reasonable ways as different from the original version.
 *
 *      3) This license does not grant any rights to any Long of the program
 *         with regards to rights under trademark law for use of the trade names
 *         or trademarks of eGovernments Foundation.
 *
 *  In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
 */

package org.egov.collection.web.contract;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotNull;


import jakarta.validation.constraints.Size;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

@JsonPropertyOrder({ "id", "name", "description", "tableName", "columnName", "attributeName", "active",
		"fullyQualifiedName" })
public class AccountDetailTypeContract extends AuditableContract implements java.io.Serializable {

	private Long id;

	@JsonProperty(access = Access.WRITE_ONLY)
	private List<Long> ids = new ArrayList<Long>();

	@NotNull
	@Size(max = 50, min = 1)
	private String name;

	@NotNull
	@Size(max = 50, min = 1)
	private String description;

	@Size(max = 25)
	private String tableName;

	@NotNull
	private Boolean active;

	@Size(max = 250, min = 1)
	private String fullyQualifiedName;

	public Long getId() {
		return this.id;
	}

	public AccountDetailTypeContract(final String id) {
		super();
		this.id = Long.valueOf(id);
	}

}