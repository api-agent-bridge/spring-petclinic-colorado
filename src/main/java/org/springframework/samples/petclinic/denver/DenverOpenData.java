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

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.samples.petclinic.denver.DenverOpenDataApi.BusinessLicenseAttributes;
import org.springframework.samples.petclinic.denver.DenverOpenDataApi.DogParkAttributes;
import org.springframework.samples.petclinic.denver.DenverOpenDataApi.FeatureSet;
import org.springframework.samples.petclinic.denver.DenverOpenDataApi.ServiceRequestAttributes;
import org.springframework.samples.petclinic.upstream.RestUpstreams;
import org.springframework.samples.petclinic.upstream.UpstreamException;
import org.springframework.stereotype.Component;

/**
 * Reads the dog parks, the 311 animal cases and the kennel licenses from Denver's open
 * data. Each method costs one query, and each query is logged with what came back.
 */
@Component
class DenverOpenData {

	/**
	 * The name of the service in errors, in words a client can read.
	 */
	static final String DENVER = "Denver's open data service";

	private static final Log logger = LogFactory.getLog(DenverOpenData.class);

	private final DenverOpenDataApi api;

	DenverOpenData(DenverOpenDataApi api) {
		this.api = api;
	}

	/**
	 * Returns every dog park, sorted by name.
	 */
	List<DenverDogPark> dogParks() {
		ArcGisQuery query = ArcGisQuery.where("1=1")
			.outFields(DogParkAttributes.FIELDS)
			.centroids()
			.orderBy("LOCATION");
		return read("the dog parks", () -> this.api.dogParks(query.parameters())).featuresOrEmpty()
			.stream()
			.map(DenverDogPark::from)
			.toList();
	}

	/**
	 * Returns the dog parks whose shape lies within a distance of a point, in the order
	 * the service sends them.
	 */
	List<DenverDogPark> dogParksWithin(GeoPoint point, int metres) {
		ArcGisQuery query = ArcGisQuery.where("1=1")
			.outFields(DogParkAttributes.FIELDS)
			.centroids()
			.within(point, metres);
		return read("the dog parks near a point", () -> this.api.dogParks(query.parameters())).featuresOrEmpty()
			.stream()
			.map(DenverDogPark::from)
			.toList();
	}

	/**
	 * Returns one page of the animal cases that match a filter, the most recent first.
	 * @param offset the number of cases to skip
	 * @param count the number of cases to return
	 */
	List<DenverAnimalServiceRequest> animalCases(AnimalCaseFilter filter, long offset, int count) {
		ArcGisQuery query = ArcGisQuery.where(filter.where())
			.outFields(ServiceRequestAttributes.FIELDS)
			.orderBy("Case_Created_Date DESC,OBJECTID DESC")
			.page(offset, count);
		// the recordings of mock mode hold 10 cases whatever the size, so the page is cut
		// here as well
		return read("the 311 animal cases", () -> this.api.serviceRequests(query.parameters())).featuresOrEmpty()
			.stream()
			.limit(count)
			.map((feature) -> DenverAnimalServiceRequest.from(feature.attributes()))
			.toList();
	}

	/**
	 * Counts the animal cases that match a filter.
	 */
	int countAnimalCases(AnimalCaseFilter filter) {
		ArcGisQuery query = ArcGisQuery.where(filter.where()).countOnly();
		FeatureSet<ServiceRequestAttributes> answer = read("the number of 311 animal cases",
				() -> this.api.serviceRequests(query.parameters()));
		if (answer.count() == null) {
			throw new UpstreamException(DENVER, "the count of 311 animal cases came back without a number");
		}
		return answer.count();
	}

	/**
	 * Counts the animal cases that match a filter by type, the largest count first.
	 */
	List<DenverAnimalCaseCount> countAnimalCasesByType(AnimalCaseFilter filter) {
		ArcGisQuery query = ArcGisQuery.where(filter.where()).countBy("Case_Summary", "cases");
		return read("the 311 animal cases by type", () -> this.api.serviceRequestCounts(query.parameters()))
			.featuresOrEmpty()
			.stream()
			.map((feature) -> feature.attributes())
			.flatMap((row) -> DenverAnimalCaseType.of(row.summary())
				.map((type) -> new DenverAnimalCaseCount(type, row.cases()))
				.stream())
			.sorted(Comparator.comparingInt(DenverAnimalCaseCount::cases).reversed())
			.toList();
	}

	/**
	 * Returns every active kennel license, sorted by the name of the business.
	 */
	List<DenverKennelLicense> kennelLicenses() {
		ArcGisQuery query = ArcGisQuery.where("LICENSE_TYPE = 'Kennel'")
			.outFields(BusinessLicenseAttributes.FIELDS)
			.orderBy("ENTITY_NAME");
		return read("the kennel licenses", () -> this.api.businessLicenses(query.parameters())).featuresOrEmpty()
			.stream()
			.map((feature) -> DenverKennelLicense.from(feature.attributes()))
			.toList();
	}

	private <A> FeatureSet<A> read(String what, Supplier<FeatureSet<A>> query) {
		long started = System.nanoTime();
		FeatureSet<A> answer = RestUpstreams.call(DENVER, query);
		if (answer == null) {
			throw new UpstreamException(DENVER, "the query on " + what + " came back without a body");
		}
		// ArcGIS answers an invalid query with status 200 and the error in the body
		if (answer.error() != null) {
			throw new UpstreamException(DENVER, "the query on %s failed with %d %s %s".formatted(what,
					answer.error().code(), answer.error().message(), answer.error().details()));
		}
		logger.info("REST query on %s returned %s (%d ms)".formatted(what,
				(answer.count() != null) ? "the count " + answer.count() : answer.featuresOrEmpty().size() + " rows",
				Duration.ofNanos(System.nanoTime() - started).toMillis()));
		return answer;
	}

}
