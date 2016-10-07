/*
 * eGov suite of products aim to improve the internal efficiency,transparency,
 *    accountability and the service delivery of the government  organizations.
 *
 *     Copyright (C) <2015>  eGovernments Foundation
 *
 *     The updated version of eGov suite of products as by eGovernments Foundation
 *     is available at http://www.egovernments.org
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see http://www.gnu.org/licenses/ or
 *     http://www.gnu.org/licenses/gpl.html .
 *
 *     In addition to the terms of the GPL license to be adhered to in using this
 *     program, the following additional terms are to be complied with:
 *
 *         1) All versions of this program, verbatim or modified must carry this
 *            Legal Notice.
 *
 *         2) Any misrepresentation of the origin of the material is prohibited. It
 *            is required that all modified versions of this material be marked in
 *            reasonable ways as different from the original version.
 *
 *         3) This license does not grant any rights to any user of the program
 *            with regards to rights under trademark law for use of the trade names
 *            or trademarks of eGovernments Foundation.
 *
 *   In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
 */

package org.egov.ptis.service.elasticsearch;

import static org.egov.ptis.constants.PropertyTaxConstants.COLLECTION_INDEX_NAME;
import static org.egov.ptis.constants.PropertyTaxConstants.PROPERTY_TAX_INDEX_NAME;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.egov.commons.CFinancialYear;
import org.egov.commons.service.CFinancialYearService;
import org.egov.infra.utils.DateUtils;
import org.egov.ptis.bean.dashboard.CollIndexTableData;
import org.egov.ptis.bean.dashboard.CollectionDetailsRequest;
import org.egov.ptis.bean.dashboard.CollectionIndexDetails;
import org.egov.ptis.bean.dashboard.CollectionTrend;
import org.egov.ptis.constants.PropertyTaxConstants;
import org.egov.ptis.repository.elasticsearch.CollectionIndexESRepository;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ResultsExtractor;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

@Service
public class CollectionIndexElasticSearchService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CollectionIndexElasticSearchService.class); 
	
	private CollectionIndexESRepository collectionIndexESRepository;
	
	@Autowired
	private CFinancialYearService cFinancialYearService;
	
	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;
	
	@Autowired
	private Client client;
	
	@Autowired
	public CollectionIndexElasticSearchService(final CollectionIndexESRepository collectionIndexESRepository) {
        this.collectionIndexESRepository = collectionIndexESRepository;
    }
	
	/**
	 * Gives the consolidated collection for the current year and last year - used in collectionstats rest API
	 * @param billingService
	 * @return Map
	 */
	public Map<String, BigDecimal> getConsolidatedCollection(String billingService){
		LOGGER.info("---- Entered getConsolidatedCollection ---- ");
		/**
		 * As per Elastic Search functionality, to get the total collections between 2 dates, add a day to the endDate and fetch the results
		 */
		Long startTime = System.currentTimeMillis();
		Map<String, BigDecimal> consolidatedCollValues = new HashMap<>();
		CFinancialYear currFinYear = cFinancialYearService.getFinancialYearByDate(new Date());
		//For current year results
		consolidatedCollValues.put("cytdColln", getConsolidatedCollForYears(currFinYear.getStartingDate(), DateUtils.addDays(new Date(), 1), billingService));
		//For last year results
		consolidatedCollValues.put("lytdColln", getConsolidatedCollForYears(DateUtils.addYears(currFinYear.getStartingDate(), -1), 
				DateUtils.addDays(DateUtils.addYears(new Date(), -1), 1), billingService));
		Long timeTaken = System.currentTimeMillis() - startTime;
		LOGGER.info("getConsolidatedCollection ----> calculations done in " + timeTaken / 1000 + " (secs)");
		return consolidatedCollValues;
	}

	/**
	 * Gives the consolidated collection for the dates and billing service
	 * @param fromDate
	 * @param toDate
	 * @param billingService
	 * @return BigDecimal
	 */
	public BigDecimal getConsolidatedCollForYears(Date fromDate, Date toDate, String billingService) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		QueryBuilder queryBuilder = QueryBuilders.boolQuery().must(QueryBuilders.rangeQuery("receiptdate").gte(sdf.format(fromDate)).lte(sdf.format(toDate)))
							.must(QueryBuilders.termQuery("billingservice", billingService));
		SearchQuery searchQueryColl = new NativeSearchQueryBuilder().withIndices(COLLECTION_INDEX_NAME).withQuery(queryBuilder)
						.addAggregation(AggregationBuilders.sum("collectiontotal").field("totalamount"))
						.build();

		Aggregations collAggr = elasticsearchTemplate.query(searchQueryColl, new ResultsExtractor<Aggregations>() {
				@Override
				public Aggregations extract(SearchResponse response) {
					return response.getAggregations();
				}
		});

		Sum aggr = collAggr.get("collectiontotal");
		return BigDecimal.valueOf(aggr.getValue()).setScale(0, BigDecimal.ROUND_HALF_UP);
	}
	
	/**
	 * Builds query based on the input parameters sent
	 * @param collectionDetailsRequest
	 * @param indexName
	 * @param ulbCodeField
	 * @return BoolQueryBuilder
	 */
	private BoolQueryBuilder prepareWhereClause(CollectionDetailsRequest collectionDetailsRequest, String indexName, String ulbCodeField){
		BoolQueryBuilder boolQuery = null;
		if(indexName.equals(PROPERTY_TAX_INDEX_NAME))
			boolQuery = QueryBuilders.boolQuery().must(QueryBuilders.rangeQuery("annualdemand").from(0).to(null));
		else if(indexName.equals(COLLECTION_INDEX_NAME))
			boolQuery = QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("billingservice", PropertyTaxConstants.COLLECION_BILLING_SERVICE_PT));
		if(StringUtils.isNotBlank(collectionDetailsRequest.getRegionName()))
			boolQuery = boolQuery.must(QueryBuilders.matchQuery("regionname", collectionDetailsRequest.getRegionName()));
		if(StringUtils.isNotBlank(collectionDetailsRequest.getDistrictName()))
			boolQuery = boolQuery.must(QueryBuilders.matchQuery("districtname", collectionDetailsRequest.getDistrictName()));
		if(StringUtils.isNotBlank(collectionDetailsRequest.getUlbGrade()))
			boolQuery = boolQuery.must(QueryBuilders.matchQuery("citygrade", collectionDetailsRequest.getUlbGrade()));
		//To be enabled later
		/*if(StringUtils.isNotBlank(collectionDetailsRequest.getUlbCode()))
			boolQuery = boolQuery.must(QueryBuilders.matchQuery(ulbCodeField, collectionDetailsRequest.getUlbCode()));*/
		return boolQuery;
	}
	
	/**
	 * API sets the consolidated collections for single day and between the 2 dates 
	 * @param collectionDetailsRequest
	 * @param collectionIndexDetails
	 */
	public void getCompleteCollectionIndexDetails(CollectionDetailsRequest collectionDetailsRequest, 
			CollectionIndexDetails collectionIndexDetails){
		Date fromDate;
		Date toDate;
		BigDecimal todayColl = BigDecimal.ZERO;
		BigDecimal tillDateColl = BigDecimal.ZERO;
		LOGGER.info("---- Entered getCompleteCollectionIndexDetails ----");
		Long startTime = System.currentTimeMillis();
		/**
		 * As per Elastic Search functionality, to get the total collections between 2 dates, add a day to the endDate and fetch the results
		 *
		 * For Current day's collection
		 * if dates are sent in the request, consider the toDate, else take date range between current date +1 day
		 */
		if(StringUtils.isNotBlank(collectionDetailsRequest.getFromDate()) && StringUtils.isNotBlank(collectionDetailsRequest.getToDate())){
			fromDate = DateUtils.getDate(collectionDetailsRequest.getToDate(), "yyyy-MM-dd");
			toDate =  DateUtils.addDays(DateUtils.getDate(collectionDetailsRequest.getToDate(), "yyyy-MM-dd"), 1);
		} else {
			fromDate = new Date();
			toDate = DateUtils.addDays(fromDate, 1);
		}
		//Today’s collection
		todayColl = getTotalCollectionsForDates(collectionDetailsRequest, fromDate, toDate);
		collectionIndexDetails.setTodayColl(todayColl);

		//Last year Today’s day collection
		todayColl = getTotalCollectionsForDates(collectionDetailsRequest, DateUtils.addYears(fromDate, -1), DateUtils.addYears(toDate, -1));
		collectionIndexDetails.setLyTodayColl(todayColl);
		
		/**
		 * For collections between the date ranges
		 * if dates are sent in the request, consider the same, else calculate from current year start date till current date+1 day
		 */
		if(StringUtils.isNotBlank(collectionDetailsRequest.getFromDate()) && StringUtils.isNotBlank(collectionDetailsRequest.getToDate())){
			fromDate = DateUtils.getDate(collectionDetailsRequest.getFromDate(), "yyyy-MM-dd");
			toDate =  DateUtils.addDays(DateUtils.getDate(collectionDetailsRequest.getToDate(), "yyyy-MM-dd"), 1);
		} else {
			fromDate = new DateTime().withMonthOfYear(4).dayOfMonth().withMinimumValue().toDate();
			toDate = DateUtils.addDays(new Date(), 1);
		}
		//Current Year till today collection
		tillDateColl = getTotalCollectionsForDates(collectionDetailsRequest, fromDate, toDate);
		collectionIndexDetails.setCytdColl(tillDateColl);
		
		//Last year till same date of today’s date collection
		tillDateColl = getTotalCollectionsForDates(collectionDetailsRequest, DateUtils.addYears(fromDate, -1), DateUtils.addYears(toDate, -1));
		collectionIndexDetails.setLytdColl(tillDateColl);
		
		Long timeTaken = System.currentTimeMillis() - startTime;
		LOGGER.info("getCompleteCollectionIndexDetails ----> Total collection amounts fetched in " + timeTaken / 1000 + " (secs)");
	}
	
	/**
	 * Returns the consolidated collections for single day and between the 2 dates 
	 * @param collectionDetailsRequest
	 * @param fromDate
	 * @param toDate
	 * @return BigDecimal
	 */
	public BigDecimal getTotalCollectionsForDates(CollectionDetailsRequest collectionDetailsRequest, Date fromDate, Date toDate){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		BoolQueryBuilder boolQuery = prepareWhereClause(collectionDetailsRequest, COLLECTION_INDEX_NAME, "citycode");
		boolQuery = boolQuery.must(QueryBuilders.rangeQuery("receiptdate").gte(sdf.format(fromDate)).lte(sdf.format(toDate)));
		
		SearchQuery searchQueryColl = new NativeSearchQueryBuilder().withIndices(COLLECTION_INDEX_NAME)
				.withQuery(boolQuery)
				.addAggregation(AggregationBuilders.sum("collectiontotal").field("totalamount"))
				.build();
		
		Aggregations collAggr = elasticsearchTemplate.query(searchQueryColl, new ResultsExtractor<Aggregations>() {
			@Override
			public Aggregations extract(SearchResponse response) {
				return response.getAggregations();
			}
		});
		
		Sum aggr = collAggr.get("collectiontotal");
		return BigDecimal.valueOf(aggr.getValue()).setScale(0, BigDecimal.ROUND_HALF_UP);
	}
	
	/**
	 * Prepares Collection Index Table Data 
	 * @param collectionDetailsRequest
	 * @return List
	 */
	public List<CollIndexTableData> getResponseTableData(CollectionDetailsRequest collectionDetailsRequest){
		List<CollIndexTableData> collIndDataList = new ArrayList<>();
		LOGGER.info("---- Entered getResponseTableData ----");
		Long startTime = System.currentTimeMillis();
		Date fromDate;
		Date toDate;
		String name;
		/**
		 * For collection and demand between the date ranges
		 * if dates are sent in the request, consider fromDate and toDate+1 , else calculate from current year start date till current date+1 day
		 */
		if(StringUtils.isNotBlank(collectionDetailsRequest.getFromDate()) && StringUtils.isNotBlank(collectionDetailsRequest.getToDate())){
			fromDate = DateUtils.getDate(collectionDetailsRequest.getFromDate(), "yyyy-MM-dd");
			toDate =  DateUtils.addDays(DateUtils.getDate(collectionDetailsRequest.getToDate(), "yyyy-MM-dd"), 1);
		} else {
			fromDate = new DateTime().withMonthOfYear(4).dayOfMonth().withMinimumValue().toDate();
			toDate = DateUtils.addDays(new Date(), 1);
		}
		int noOfMonths = DateUtils.noOfMonths(fromDate, toDate);

		/**
		 * Select the grouping based on the inputs given, by default the grouping is done based on Regions
		 * If Region name is sent, group by Districts
		 * If District name is sent, group by ULBs in the district
		 * If ULB name is sent, group by wards in the ULB
		 * If ULB grade is sent, group by ULBs having the grade
		 */
		String aggregationField = "regionname";
		if(StringUtils.isNotBlank(collectionDetailsRequest.getRegionName()))
			aggregationField = "districtname";
		if(StringUtils.isNotBlank(collectionDetailsRequest.getDistrictName()))
			aggregationField = "cityname";
		//Wardwise group to be implemented later
		
		//For current year's till date collection
		Map<String, BigDecimal> cytdCollMap = getCollectionAndDemandResults(collectionDetailsRequest, fromDate, toDate, 
				COLLECTION_INDEX_NAME, "totalamount", "citycode", aggregationField);
		//For total demand
		Map<String, BigDecimal> totalDemandMap = getCollectionAndDemandResults(collectionDetailsRequest, fromDate, toDate, 
				PROPERTY_TAX_INDEX_NAME, "totaldemand", "ulbcode", aggregationField);
		//For current year demand
		Map<String, BigDecimal> currYrTotalDemandMap = getCollectionAndDemandResults(collectionDetailsRequest, fromDate, toDate, 
				PROPERTY_TAX_INDEX_NAME, "annualdemand", "ulbcode", aggregationField);
		//For last year's till today's date collections
		Map<String, BigDecimal> lytdCollMap = getCollectionAndDemandResults(collectionDetailsRequest, DateUtils.addYears(fromDate, -1), 
				DateUtils.addYears(toDate, -1), COLLECTION_INDEX_NAME, "totalamount", "citycode", aggregationField);

		CollIndexTableData collIndData;
		BigDecimal cytdDmd = BigDecimal.ZERO;
		BigDecimal performance = BigDecimal.ZERO;
		BigDecimal balance = BigDecimal.ZERO;
		for(Map.Entry<String, BigDecimal> entry : cytdCollMap.entrySet()){
			collIndData = new CollIndexTableData();
			name = entry.getKey();
			if(aggregationField.equals("regionname"))
				collIndData.setRegionName(name);
			else if(aggregationField.equals("districtname")){
				collIndData.setRegionName(collectionDetailsRequest.getRegionName());
				collIndData.setDistrictName(name);
			} else if(aggregationField.equals("cityname")){
				collIndData.setUlbName(name);
				collIndData.setDistrictName(collectionDetailsRequest.getDistrictName());
			}
				
			collIndData.setCytdColl(entry.getValue());
			//Proportional Demand = (totalDemand/12)*noOfmonths
			if(noOfMonths == 0)
				noOfMonths = 1;
			cytdDmd = (currYrTotalDemandMap.get(name).divide(BigDecimal.valueOf(12),BigDecimal.ROUND_HALF_UP)).multiply(BigDecimal.valueOf(noOfMonths));
			collIndData.setCytdDmd(cytdDmd);
			balance = cytdDmd.subtract(collIndData.getCytdColl());
			performance = (balance.multiply(PropertyTaxConstants.BIGDECIMAL_100)).divide(cytdDmd, BigDecimal.ROUND_HALF_UP);
			collIndData.setPerformance(performance);
			collIndData.setCytdBalDmd(balance);
			collIndData.setTotalDmd(totalDemandMap.get(name));
			collIndData.setLytdColl(lytdCollMap.get(name) == null ? BigDecimal.ZERO : lytdCollMap.get(name));
			collIndDataList.add(collIndData);
		}
		Long timeTaken = System.currentTimeMillis() - startTime;
		LOGGER.info("getResponseTableData ----> Total time taken is " + timeTaken / 1000 + " (secs)");
		return collIndDataList;
	}

	/**
	 * Provides collection and demand results 
	 * @param collectionDetailsRequest
	 * @param fromDate
	 * @param toDate
	 * @param indexName
	 * @param fieldName
	 * @param ulbCodeField
	 * @param aggregationField
	 * @return Map
	 */
	public Map<String, BigDecimal> getCollectionAndDemandResults(CollectionDetailsRequest collectionDetailsRequest, Date fromDate, Date toDate,
			String indexName, String fieldName, String ulbCodeField, String aggregationField) {
		BoolQueryBuilder boolQuery = prepareWhereClause(collectionDetailsRequest, indexName, ulbCodeField); 
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		if(indexName.equals(COLLECTION_INDEX_NAME))
			boolQuery = boolQuery.must(QueryBuilders.rangeQuery("receiptdate").gte(sdf.format(fromDate)).lte(sdf.format(toDate)));
		
		AggregationBuilder aggregation = AggregationBuilders.terms("by_city").field(aggregationField)
				.size(120)
				.subAggregation(AggregationBuilders.sum("total").field(fieldName));

		SearchResponse response = client.prepareSearch(indexName)
				  .setQuery(boolQuery)
				  .addAggregation(aggregation)
				  .execute().actionGet();
		
		StringTerms cityAggr = response.getAggregations().get("by_city");
		Map<String, BigDecimal> cytdCollMap = new HashMap<>();
		for (Terms.Bucket entry : cityAggr.getBuckets()) {
			Sum aggr = entry.getAggregations().get("total");
			cytdCollMap.put(String.valueOf(entry.getKey()), BigDecimal.valueOf(aggr.getValue()).setScale(0, BigDecimal.ROUND_HALF_UP));
        }
		return cytdCollMap;
	}
	
	/**
	 * Prepares month-wise collections for 3 consecutive years - from current year
	 * @param collectionDetailsRequest
	 * @return List
	 */
	public List<CollectionTrend> getMonthwiseCollectionDetails(CollectionDetailsRequest collectionDetailsRequest){
		LOGGER.info("---- Entered getMonthwiseCollectionDetails ----");
		Long startTime = System.currentTimeMillis();
		List<CollectionTrend> collTrendsList = new ArrayList<>();
		CollectionTrend collTrend;
		Date fromDate;
		Date toDate;
		SearchResponse response = null;
		Date dateForMonth;
		String[] dateArr;
		Integer month;
		Sum aggregateSum;
		Date finYearStartDate = cFinancialYearService.getFinancialYearByDate(new Date()).getStartingDate();
		Date finYearEndDate = cFinancialYearService.getFinancialYearByDate(new Date()).getEndingDate();
		Map<Integer, String> monthValuesMap = DateUtils.getAllMonthsWithFullNames();
		String monthName;
		List<Map<String, BigDecimal>> yearwiseMonthlyCollList = new ArrayList<>();
		Map<String, BigDecimal> monthwiseColl;
		/**
		 * For month-wise collections between the date ranges
		 * if dates are sent in the request, consider fromDate and toDate+1 , else calculate from current year start date till current date+1 day
		 */
		if(StringUtils.isNotBlank(collectionDetailsRequest.getFromDate()) && StringUtils.isNotBlank(collectionDetailsRequest.getToDate())){
			fromDate = DateUtils.getDate(collectionDetailsRequest.getFromDate(), "yyyy-MM-dd");
			toDate =  DateUtils.addDays(DateUtils.getDate(collectionDetailsRequest.getToDate(), "yyyy-MM-dd"), 1);
		} else {
			fromDate = new DateTime().withMonthOfYear(4).dayOfMonth().withMinimumValue().toDate();
			toDate = DateUtils.addDays(new Date(), 1);
		}
		for(int count = 0; count <= 2 ; count++){
			monthwiseColl = new LinkedHashMap<>();
			response = getCollectionsForConsecutiveYears(collectionDetailsRequest, fromDate, toDate);
			Histogram dateaggs = response.getAggregations().get("date_agg");
			
			for (Histogram.Bucket entry : dateaggs.getBuckets()) {
				dateArr = entry.getKeyAsString().split("T");
				dateForMonth = DateUtils.getDate(dateArr[0], "yyyy-MM-dd");
				month = Integer.valueOf(dateArr[0].split("-", 3)[1]);
				monthName = monthValuesMap.get(month);
				aggregateSum = entry.getAggregations().get("current_total");
				//If the total amount is greater than 0 and the month belongs to respective financial year, add values to the map
				if(DateUtils.between(dateForMonth, finYearStartDate, finYearEndDate) 
						&& BigDecimal.valueOf(aggregateSum.getValue()).setScale(0, BigDecimal.ROUND_HALF_UP).compareTo(BigDecimal.ZERO) > 0)
					monthwiseColl.put(monthName, BigDecimal.valueOf(aggregateSum.getValue()).setScale(0, BigDecimal.ROUND_HALF_UP));
	        }
			yearwiseMonthlyCollList.add(monthwiseColl);
			fromDate = DateUtils.addYears(fromDate, -1);
			toDate = DateUtils.addYears(toDate, -1);
			finYearStartDate = DateUtils.addYears(finYearStartDate, -1);
			finYearEndDate = DateUtils.addYears(finYearEndDate, -1);
		}
		
		for(Map.Entry<String, BigDecimal> entry :yearwiseMonthlyCollList.get(0).entrySet()){
			collTrend = new CollectionTrend();
			collTrend.setMonth(entry.getKey());
			collTrend.setCyColl(entry.getValue());
			collTrend.setLyColl(yearwiseMonthlyCollList.get(1).get(collTrend.getMonth()) == null ? BigDecimal.ZERO : yearwiseMonthlyCollList.get(1).get(collTrend.getMonth()));
			collTrend.setPyColl(yearwiseMonthlyCollList.get(2).get(collTrend.getMonth()) == null ? BigDecimal.ZERO : yearwiseMonthlyCollList.get(2).get(collTrend.getMonth()));
			collTrendsList.add(collTrend);
		}
		Long timeTaken = System.currentTimeMillis() - startTime;
		LOGGER.info("getMonthwiseCollectionDetails ----> Total time taken is " + timeTaken / 1000 + " (secs)");
		return collTrendsList;
	}
	
	/**
	 * Provides month-wise collections for consecutive years
	 * @param collectionDetailsRequest
	 * @param fromDate
	 * @param toDate
	 * @return SearchResponse
	 */
	private SearchResponse getCollectionsForConsecutiveYears(CollectionDetailsRequest collectionDetailsRequest, Date fromDate, Date toDate) {
		BoolQueryBuilder boolQuery = prepareWhereClause(collectionDetailsRequest, COLLECTION_INDEX_NAME, "citycode");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		AggregationBuilder monthAggrgation = AggregationBuilders.dateHistogram("date_agg")
											.field("receiptdate")
											.interval(DateHistogramInterval.MONTH)
											.subAggregation(AggregationBuilders.sum("current_total").field("totalamount"));
		
		SearchResponse response = client.prepareSearch(COLLECTION_INDEX_NAME)
								  .setQuery(boolQuery.must(QueryBuilders.rangeQuery("receiptdate").gte(sdf.format(fromDate)).lte(sdf.format(toDate))))
								  .addAggregation(monthAggrgation)
								  .execute().actionGet();
		return response;
	}
	
}
