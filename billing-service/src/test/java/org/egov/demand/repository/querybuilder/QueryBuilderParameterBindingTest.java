package org.egov.demand.repository.querybuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.egov.demand.model.BusinessServiceDetail;
import org.egov.demand.model.TaxHeadMasterCriteria;
import org.egov.demand.model.TaxPeriod;
import org.egov.demand.model.enums.PeriodCycle;
import org.egov.demand.web.contract.BusinessServiceDetailCriteria;
import org.egov.demand.web.contract.TaxPeriodCriteria;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;

/**
 * Checks that the query builders pass search values as bind parameters, or
 * escape them where the method returns only the query text.
 */
class QueryBuilderParameterBindingTest {

	private static final String QUOTED_VALUE = "x' OR '1'='1";

	private static Set<String> setOf(String... values) {
		Set<String> set = new LinkedHashSet<>();
		Collections.addAll(set, values);
		return set;
	}

	private static int placeholderCount(String query) {
		return query.length() - query.replace("?", "").length();
	}

	@Test
	void demandQueryForConsumerCodesBindsBusinessService() {
		Map<String, Set<String>> businessConsumerCodes = new HashMap<>();
		businessConsumerCodes.put(QUOTED_VALUE, setOf("CC-1", "CC-2"));
		List<Object> params = new ArrayList<>();

		String query = new DemandQueryBuilder().getDemandQueryForConsumerCodes(businessConsumerCodes, params, "pb.amritsar");

		assertFalse(query.contains(QUOTED_VALUE));
		assertTrue(query.endsWith("AND dmd.businessservice=? AND dmd.consumercode IN ( ? , ? )"));
		assertEquals(List.of("pb.amritsar", QUOTED_VALUE, "CC-1", "CC-2"), params);
		assertEquals(placeholderCount(query), params.size());
	}

	@Test
	void businessServiceSearchBindsCollections() {
		BusinessServiceDetailCriteria criteria = BusinessServiceDetailCriteria.builder().tenantId("pb")
				.businessService(setOf(QUOTED_VALUE, "PT")).id(setOf("1")).build();
		List<Object> params = new ArrayList<>();

		String query = new BusinessServDetailQueryBuilder().prepareSearchQuery(criteria, params);

		assertFalse(query.contains(QUOTED_VALUE));
		assertTrue(query.endsWith("and businessservice.businessservice IN  (?,?) and businessservice.id IN  (?)"));
		assertEquals(List.of("pb", QUOTED_VALUE, "PT", "1"), params);
		assertEquals(placeholderCount(query), params.size());
	}

	@Test
	void businessServiceValidationEscapesLiterals() {
		BusinessServiceDetail detail = BusinessServiceDetail.builder().id("1").tenantId("pb").businessService(QUOTED_VALUE)
				.build();

		String query = new BusinessServDetailQueryBuilder().prepareQueryForValidation(Collections.singletonList(detail),
				"edit");

		assertTrue(query.contains("businessservice.businessservice = 'x'' OR ''1''=''1' and "));

		detail.setBusinessService("x\\");
		assertThrows(CustomException.class, () -> new BusinessServDetailQueryBuilder()
				.prepareQueryForValidation(Collections.singletonList(detail), "edit"));
	}

	@Test
	void taxPeriodSearchBindsCollectionsInQueryOrder() {
		TaxPeriodCriteria criteria = TaxPeriodCriteria.builder().tenantId("pb").service(setOf(QUOTED_VALUE))
				.periodCycle(PeriodCycle.ANNUAL).fromDate(10L).toDate(20L).id(setOf("7")).build();
		List<Object> params = new ArrayList<>();

		String query = new TaxPeriodQueryBuilder().prepareSearchQuery(criteria, params);

		assertFalse(query.contains(QUOTED_VALUE));
		assertEquals(placeholderCount(query), params.size());
		String cycle = PeriodCycle.ANNUAL.toString();
		assertEquals(List.of("pb", QUOTED_VALUE, cycle, // outer filters
				"pb", 10L, QUOTED_VALUE, cycle, // CASE WHEN sub-query
				"pb", 10L, QUOTED_VALUE, cycle, // THEN sub-query
				"pb", // ELSE sub-query
				"pb", 20L, QUOTED_VALUE, cycle, // todate sub-query
				"7"), params);
	}

	@Test
	void taxPeriodValidationEscapesLiterals() {
		TaxPeriod taxPeriod = new TaxPeriod("1", "pb", 10L, 20L, PeriodCycle.ANNUAL, QUOTED_VALUE, "it's", "2024-25", null);

		String query = new TaxPeriodQueryBuilder().prepareQueryForValidation(Collections.singletonList(taxPeriod), "edit");

		assertTrue(query.contains(" taxperiod.service = 'x'' OR ''1''=''1' and "));
		assertTrue(query.contains(" taxperiod.code = 'it''s' and "));

		taxPeriod.setTenantId("pb\\");
		assertThrows(CustomException.class,
				() -> new TaxPeriodQueryBuilder().prepareQueryForValidation(Collections.singletonList(taxPeriod), "edit"));
	}

	@Test
	void taxHeadSearchBindsIdsAndCodes() {
		TaxHeadMasterCriteria byId = TaxHeadMasterCriteria.builder().tenantId("pb").service("PT").id(setOf(QUOTED_VALUE, "2"))
				.code(setOf()).category("TAX").build();
		List<Object> idParams = new ArrayList<>();
		String idQuery = new TaxHeadMasterQueryBuilder().getQuery(byId, idParams);

		assertFalse(idQuery.contains(QUOTED_VALUE));
		assertTrue(idQuery.contains(" AND taxhead.id IN (?,?) AND taxhead.category = ?"));
		assertEquals(List.of("pb", "PT", QUOTED_VALUE, "2", "TAX"), idParams);

		TaxHeadMasterCriteria byCode = TaxHeadMasterCriteria.builder().tenantId("pb").id(setOf()).code(setOf(QUOTED_VALUE))
				.build();
		List<Object> codeParams = new ArrayList<>();
		String codeQuery = new TaxHeadMasterQueryBuilder().getQuery(byCode, codeParams);

		assertFalse(codeQuery.contains(QUOTED_VALUE));
		assertTrue(codeQuery.contains(" AND taxhead.code IN (?)"));
		assertEquals(List.of("pb", QUOTED_VALUE), codeParams);
	}
}
