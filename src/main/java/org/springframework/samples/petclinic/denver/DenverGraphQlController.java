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

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import graphql.schema.DataFetchingFieldSelectionSet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.samples.petclinic.census.CensusAddressMatch;
import org.springframework.samples.petclinic.census.CensusGeocoder;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.upstream.InvalidArgumentException;
import org.springframework.samples.petclinic.upstream.ResultPages;
import org.springframework.samples.petclinic.upstream.SearchText;
import org.springframework.stereotype.Controller;

/**
 * Answers the <code>denver*</code> queries of the GraphQL schema, and adds the Denver dog
 * parks nearest to an owner's address as a field of <code>Owner</code>.
 */
@Controller
class DenverGraphQlController {

	static final int MAX_PARKS = 10;

	private static final String DENVER = "Denver";

	private final DenverOpenData openData;

	private final CensusGeocoder geocoder;

	private final int searchRadiusInMetres;

	DenverGraphQlController(DenverOpenData openData, CensusGeocoder geocoder,
			@Value("${petclinic.upstream.denver.dog-park-search-radius-in-metres}") int searchRadiusInMetres) {
		this.openData = openData;
		this.geocoder = geocoder;
		this.searchRadiusInMetres = searchRadiusInMetres;
	}

	/**
	 * Resolves <code>Owner.nearestDogParks</code>. An owner in Denver costs one call to
	 * the Census geocoder, and one query on the dog parks when the geocoder finds the
	 * address. An owner in another city does not cost a call, because the layer covers
	 * Denver only.
	 */
	@SchemaMapping(typeName = "Owner")
	List<DenverDogPark> nearestDogParks(Owner owner, @Argument int first) {
		if (first < 1 || first > MAX_PARKS) {
			throw new InvalidArgumentException("first must be between 1 and " + MAX_PARKS);
		}
		if (owner.getCity() == null || !DENVER.equalsIgnoreCase(owner.getCity().strip())) {
			return List.of();
		}
		List<CensusAddressMatch> matches = this.geocoder.geocode(owner.getAddress().strip() + ", Denver, CO");
		if (matches.isEmpty()) {
			return List.of();
		}
		GeoPoint home = new GeoPoint(matches.get(0).latitude(), matches.get(0).longitude());
		return this.openData.dogParksWithin(home, this.searchRadiusInMetres)
			.stream()
			.map((park) -> park.withDistanceFrom(home))
			.sorted(Comparator.comparing(DenverDogPark::distanceInMetres,
					Comparator.nullsLast(Comparator.naturalOrder())))
			.limit(first)
			.toList();
	}

	@QueryMapping
	List<DenverDogPark> denverDogParks(@Argument Boolean fenced, @Argument Boolean shade, @Argument Boolean water) {
		return this.openData.dogParks()
			.stream()
			.filter((park) -> wanted(fenced, park.fenced()) && wanted(shade, park.shade())
					&& wanted(water, park.water()))
			.toList();
	}

	/**
	 * Resolves <code>denverAnimalServiceRequests</code>. A page costs one query, and the
	 * totals cost a second one, made only when the client selects them.
	 */
	@QueryMapping
	DenverAnimalServiceRequest.Page denverAnimalServiceRequests(@Argument List<DenverAnimalCaseType> caseTypes,
			@Argument String zipCode, @Argument Integer councilDistrict, @Argument LocalDate createdFrom,
			@Argument LocalDate createdTo, @Argument int page, @Argument int size,
			DataFetchingFieldSelectionSet selection) {
		AnimalCaseFilter filter = new AnimalCaseFilter(caseTypes, zipCode, councilDistrict, createdFrom, createdTo);
		int pageNumber = ResultPages.pageNumber(page);
		int pageSize = ResultPages.pageSize(size);
		List<DenverAnimalServiceRequest> requests = this.openData.animalCases(filter,
				(long) (pageNumber - 1) * pageSize, pageSize);
		// GraphQL resolves only the selected fields, so unselected totals stay at 0
		int total = (selection.contains("totalPages") || selection.contains("totalRequests"))
				? this.openData.countAnimalCases(filter) : 0;
		return new DenverAnimalServiceRequest.Page(requests, pageNumber, (total + pageSize - 1) / pageSize, total);
	}

	@QueryMapping
	List<DenverAnimalCaseCount> denverAnimalServiceRequestCounts(@Argument LocalDate createdFrom,
			@Argument LocalDate createdTo, @Argument String zipCode, @Argument Integer councilDistrict) {
		return this.openData
			.countAnimalCasesByType(new AnimalCaseFilter(null, zipCode, councilDistrict, createdFrom, createdTo));
	}

	@QueryMapping
	DenverKennelLicense.Page denverKennelLicenses(@Argument String search, @Argument int page, @Argument int size) {
		SearchText text = SearchText.of(search);
		List<DenverKennelLicense> licenses = this.openData.kennelLicenses()
			.stream()
			.filter((license) -> text.matches(license.entityName(), license.tradeName(), license.address()))
			.toList();
		return ResultPages.page(licenses, page, size, DenverKennelLicense.Page::new);
	}

	/**
	 * Whether a park passes a yes or no filter. A null filter passes every park, and a
	 * park the city has not recorded a value for passes only a null filter.
	 */
	private static boolean wanted(Boolean filter, Boolean value) {
		return filter == null || filter.equals(value);
	}

}
