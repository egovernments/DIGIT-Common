package org.egov.collection.repository.querybuilder;

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
 *      3) This license does not grant any rights to any user of the program
 *         with regards to rights under trademark law for use of the trade names
 *         or trademarks of eGovernments Foundation.
 *
 *  In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
 */


import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.egov.collection.web.contract.CollectionConfigGetRequest;
import org.egov.tracer.model.CustomException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CollectionConfigQueryBuilder {

	private static final Logger logger = LoggerFactory.getLogger(CollectionConfigQueryBuilder.class);


	// Columns and aliases of the configuration search that results may be sorted by
	private static final Set<String> SORTABLE_COLUMNS = new LinkedHashSet<>(Arrays.asList("keyname", "key", "value",
			"effectivefrom", "c.id", "c.keyname", "cv.value", "cv.effectivefrom", "cv.tenantid", "cv.keyid"));

	private static final Set<String> SORT_ORDERS = new LinkedHashSet<>(Arrays.asList("ASC", "DESC"));

	private static final String BASE_QUERY = "SELECT c.keyname as key, cv.value as value "
			+ "FROM {schema}.egcl_configuration c JOIN {schema}.egcl_configurationvalues cv ON c.id = cv.keyid";

	@SuppressWarnings("rawtypes")
	public String getQuery(CollectionConfigGetRequest collectionConfigGetRequest, List preparedStatementValues) {
		StringBuilder selectQuery = new StringBuilder(BASE_QUERY);

		addWhereClause(selectQuery, preparedStatementValues, collectionConfigGetRequest);
		addOrderByClause(selectQuery, collectionConfigGetRequest);
		addPagingClause(selectQuery, preparedStatementValues, collectionConfigGetRequest);

		logger.info("Query : " + selectQuery);
		return selectQuery.toString();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void addWhereClause(StringBuilder selectQuery, List preparedStatementValues,
			CollectionConfigGetRequest collectionConfigGetRequest) {

		if (collectionConfigGetRequest.getId() == null && collectionConfigGetRequest.getEffectiveFrom() == null
				&& collectionConfigGetRequest.getName() == null && collectionConfigGetRequest.getTenantId() == null)
			return;

		selectQuery.append(" WHERE");
		boolean isAppendAndClause = false;

		if (collectionConfigGetRequest.getTenantId() != null) {
			isAppendAndClause = true;
			selectQuery.append(" cv.tenantId = ?");
			preparedStatementValues.add(collectionConfigGetRequest.getTenantId());
		}

		if (collectionConfigGetRequest.getId() != null) {
			isAppendAndClause = addAndClauseIfRequired(isAppendAndClause, selectQuery);
			selectQuery.append(" c.id IN " + getIdQuery(collectionConfigGetRequest.getId()));
		}

		if (collectionConfigGetRequest.getName() != null) {
			isAppendAndClause = addAndClauseIfRequired(isAppendAndClause, selectQuery);
			selectQuery.append(" c.keyname = ?");
			preparedStatementValues.add(collectionConfigGetRequest.getName());
		}

		if (collectionConfigGetRequest.getEffectiveFrom() != null) {
			isAppendAndClause = addAndClauseIfRequired(isAppendAndClause, selectQuery);
			selectQuery.append(" cv.effectiveFrom = ?");
			preparedStatementValues.add(collectionConfigGetRequest.getEffectiveFrom());
		}
	}

	private void addOrderByClause(StringBuilder selectQuery, CollectionConfigGetRequest collectionConfigGetRequest) {
		// ORDER BY cannot be bound as a parameter, so both values are checked against allow-lists
		String sortBy = "keyname";
		if (collectionConfigGetRequest.getSortBy() != null) {
			String requestedSortBy = collectionConfigGetRequest.getSortBy().trim().toLowerCase(Locale.ROOT);
			if (!SORTABLE_COLUMNS.contains(requestedSortBy))
				throw new CustomException("INVALID_SORT_BY", "sortBy must be one of " + SORTABLE_COLUMNS);
			sortBy = requestedSortBy;
		}

		String sortOrder = "ASC";
		if (collectionConfigGetRequest.getSortOrder() != null) {
			String requestedSortOrder = collectionConfigGetRequest.getSortOrder().trim().toUpperCase(Locale.ROOT);
			if (!SORT_ORDERS.contains(requestedSortOrder))
				throw new CustomException("INVALID_SORT_ORDER", "sortOrder must be one of " + SORT_ORDERS);
			sortOrder = requestedSortOrder;
		}
		selectQuery.append(" ORDER BY " + sortBy + " " + sortOrder);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void addPagingClause(StringBuilder selectQuery, List preparedStatementValues,
			CollectionConfigGetRequest collectionConfigGetRequest) {
		// handle limit(also called pageSize) here
		selectQuery.append(" LIMIT ?");
		long pageSize = 500l;
		//FIXME GET PAGESIZE FROM APPLICATION PROPERTIEs 
		//Integer.parseInt(applicationProperties.collectionSearchPageSizeDefault());
		if (collectionConfigGetRequest.getPageSize() != null)
			pageSize = collectionConfigGetRequest.getPageSize();
		preparedStatementValues.add(pageSize); // Set limit to pageSize

		// handle offset here
		selectQuery.append(" OFFSET ?");
		int pageNumber = 0; // Default pageNo is zero meaning first page
		if (collectionConfigGetRequest.getPageNumber() != null)
			pageNumber = collectionConfigGetRequest.getPageNumber() - 1;
		preparedStatementValues.add(pageNumber * pageSize); // Set offset to pageNo * pageSize
	}

	/**
	 * This method is always called at the beginning of the method so that and
	 * is prepended before the field's predicate is handled.
	 * 
	 * @param appendAndClauseFlag
	 * @param queryString
	 * @return boolean indicates if the next predicate should append an "AND"
	 */
	private boolean addAndClauseIfRequired(boolean appendAndClauseFlag, StringBuilder queryString) {
		if (appendAndClauseFlag)
			queryString.append(" AND");

		return true;
	}

	private static String getIdQuery(List<Long> idList) {
		StringBuilder query = new StringBuilder("(");
		if (!idList.isEmpty()) {
			query.append(idList.get(0).toString());
			for (int i = 1; i < idList.size(); i++) {
				query.append(", " + idList.get(i));
			}
		}
		return query.append(")").toString();
	}
}

