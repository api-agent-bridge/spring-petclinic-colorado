/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.denver;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * The open data of the City and County of Denver on ArcGIS Online, as three query
 * operations of the ArcGIS REST API: the dog parks layer, the table of 311 service
 * requests, and the table of active business licenses. Each operation takes the
 * parameters an {@link ArcGisQuery} writes.
 * <p>
 * ArcGIS answers an invalid query with status <code>200</code> and an error object in the
 * body, so every answer is checked by {@link DenverOpenData}.
 */
@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE)
interface DenverOpenDataApi {

	/**
	 * Queries the dog parks layer, which holds the 18 off-leash dog parks as polygons.
	 */
	@GetExchange("/ODC_PARK_DOGPARKS_A/FeatureServer/84/query")
	FeatureSet<DogParkAttributes> dogParks(@RequestParam MultiValueMap<String, String> query);

	/**
	 * Queries the table of 311 service requests of the last 12 months.
	 */
	@GetExchange("/ODC_service_requests_311/FeatureServer/53/query")
	FeatureSet<ServiceRequestAttributes> serviceRequests(@RequestParam MultiValueMap<String, String> query);

	/**
	 * Counts the 311 service requests by case summary, with <code>outStatistics</code>.
	 */
	@GetExchange("/ODC_service_requests_311/FeatureServer/53/query")
	FeatureSet<CaseCountAttributes> serviceRequestCounts(@RequestParam MultiValueMap<String, String> query);

	/**
	 * Queries the table of active business licenses.
	 */
	@GetExchange("/ODC_active_business_licenses/FeatureServer/42/query")
	FeatureSet<BusinessLicenseAttributes> businessLicenses(@RequestParam MultiValueMap<String, String> query);

	/**
	 * Reads a date of an answer, which ArcGIS sends as milliseconds since 1970. Denver
	 * stores the day at midnight UTC.
	 */
	static LocalDate day(Long epochMillis) {
		return (epochMillis != null) ? Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate() : null;
	}

	/**
	 * The answer of a query.
	 *
	 * @param <A> the attributes of one feature
	 * @param features the features, each with its attributes
	 * @param count the number of matches, for a query that asked for the count only
	 * @param error the error of an invalid query, sent with status <code>200</code>
	 */
	record FeatureSet<A>(List<Feature<A>> features, Integer count, Error error) {

		List<Feature<A>> featuresOrEmpty() {
			return (this.features != null) ? this.features : List.of();
		}

	}

	/**
	 * One feature or row.
	 *
	 * @param <A> the type of the attributes
	 * @param attributes the fields the query asked for
	 * @param centroid the centre of a polygon, for a query with
	 * <code>returnCentroid</code>
	 */
	record Feature<A>(A attributes, Point centroid) {

	}

	/**
	 * A point, with the longitude in <code>x</code> and the latitude in <code>y</code>
	 * for a query with <code>outSR=4326</code>.
	 */
	record Point(double x, double y) {

	}

	record Error(int code, String message, List<String> details) {

	}

	/**
	 * A dog park. The yes and no fields hold <code>Yes</code>, <code>No</code> or null,
	 * and the condition holds a word or a score as text.
	 */
	record DogParkAttributes(@JsonProperty("OBJECTID") long id, @JsonProperty("LOCATION") String name,
			@JsonProperty("LOC_CODE") String locationCode, @JsonProperty("FENCED") String fenced,
			@JsonProperty("SHADE") String shade, @JsonProperty("WATER") String water,
			@JsonProperty("GIS_ACRES") Double acres, @JsonProperty("YEAR_BUILT") Integer yearBuilt,
			@JsonProperty("FCA_DATE") Long conditionAssessedDate, @JsonProperty("ASSET_CONDITION") String condition) {

		static final String FIELDS = "OBJECTID,LOCATION,LOC_CODE,FENCED,SHADE,WATER,GIS_ACRES,YEAR_BUILT,FCA_DATE,"
				+ "ASSET_CONDITION";

	}

	/**
	 * A 311 service request. The dates hold the day at midnight UTC, in milliseconds, and
	 * the texts beside them hold the local time in US format.
	 */
	record ServiceRequestAttributes(@JsonProperty("OBJECTID") long id, @JsonProperty("Case_Summary") String summary,
			@JsonProperty("Case_Status") String status, @JsonProperty("Case_Source") String source,
			@JsonProperty("Case_Created_Date") Long createdDate, @JsonProperty("Case_Created_dttm") String createdAt,
			@JsonProperty("Case_Closed_Date") Long closedDate, @JsonProperty("Case_Closed_dttm") String closedAt,
			@JsonProperty("Agency") String agency, @JsonProperty("Incident_Address_1") String address,
			@JsonProperty("Incident_Intersection_1") String intersection,
			@JsonProperty("Incident_Intersection_2") String crossStreet,
			@JsonProperty("Incident_Zip_Code") String zipCode,
			@JsonProperty("Council_District") Integer councilDistrict,
			@JsonProperty("Police_District") Integer policeDistrict, @JsonProperty("Latitude") Double latitude,
			@JsonProperty("Longitude") Double longitude) {

		static final String FIELDS = "OBJECTID,Case_Summary,Case_Status,Case_Source,Case_Created_Date,"
				+ "Case_Created_dttm,Case_Closed_Date,Case_Closed_dttm,Agency,Incident_Address_1,"
				+ "Incident_Intersection_1,Incident_Intersection_2,Incident_Zip_Code,Council_District,"
				+ "Police_District,Latitude,Longitude";

	}

	/**
	 * The number of 311 service requests with one case summary.
	 */
	record CaseCountAttributes(@JsonProperty("Case_Summary") String summary, @JsonProperty("cases") int cases) {

	}

	/**
	 * An active business license. The position is in Colorado Central State Plane, in US
	 * survey feet, and null for some licenses.
	 */
	record BusinessLicenseAttributes(@JsonProperty("BFN") String licenseNumber,
			@JsonProperty("LICENSE_STATUS") String status, @JsonProperty("ENTITY_NAME") String entityName,
			@JsonProperty("TRADE_NAME") String tradeName, @JsonProperty("Expiration_Date") Long expirationDate,
			@JsonProperty("AddressNoUnit") String address, @JsonProperty("ZIP") String zipCode,
			@JsonProperty("X_COORD") Double x, @JsonProperty("Y_COORD") Double y) {

		static final String FIELDS = "BFN,LICENSE_STATUS,ENTITY_NAME,TRADE_NAME,Expiration_Date,AddressNoUnit,ZIP,"
				+ "X_COORD,Y_COORD";

	}

	/**
	 * Registers the interface as an HTTP service in the group <code>denver</code>, whose
	 * base URL and timeouts come from <code>spring.http.serviceclient.denver.*</code>.
	 */
	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(group = "denver", types = DenverOpenDataApi.class)
	class Registration {

	}

}
