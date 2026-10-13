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
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.samples.petclinic.census.CensusAddressMatch;
import org.springframework.samples.petclinic.census.CensusGeocoder;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.PetRepository;
import org.springframework.samples.petclinic.owner.PetTypeRepository;
import org.springframework.samples.petclinic.upstream.UpstreamException;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test class for the {@link DenverGraphQlController}. The owner queries go through the
 * Petclinic controller, so the slice keeps the controllers of the Petclinic data and
 * leaves out those of the other external services.
 */
@GraphQlTest(excludeFilters = @Filter(type = FilterType.REGEX,
		pattern = "org\\.springframework\\.samples\\.petclinic\\.(?!graphql\\.|denver\\.|owner\\.|vet\\.|system\\.)\\w+\\..*Controller"))
@DisabledInNativeImage
@DisabledInAotMode
class DenverGraphQlControllerTests {

	private static final CensusAddressMatch BANNOCK_ST = new CensusAddressMatch("1437 BANNOCK ST, DENVER, CO, 80202",
			39.739040016914, -104.990311950936, "DENVER", "CO", "80202", "1401", "1499");

	@Autowired
	private GraphQlTester graphQl;

	@MockitoBean
	private DenverOpenData openData;

	@MockitoBean
	private CensusGeocoder geocoder;

	@MockitoBean
	private OwnerRepository owners;

	@MockitoBean
	private PetRepository pets;

	@MockitoBean
	private VetRepository vets;

	@MockitoBean
	private PetTypeRepository petTypes;

	private static DenverDogPark park(String name, double latitude, double longitude, Boolean water) {
		return new DenverDogPark("1", name, "001", true, true, water, 0.5, 2016, "Good", LocalDate.of(2024, 4, 9),
				latitude, longitude, null);
	}

	private void owner(String address, String city) {
		Owner owner = new Owner();
		owner.setId(11);
		owner.setFirstName("Dev2");
		owner.setLastName("Next");
		owner.setAddress(address);
		owner.setCity(city);
		owner.setTelephone("1234567890");
		given(this.owners.findById(11)).willReturn(Optional.of(owner));
	}

	@Test
	void nearestDogParksOfAnOwnerInDenverNearestFirst() {
		owner("1437 Bannock St", "Denver");
		given(this.geocoder.geocode("1437 Bannock St, Denver, CO")).willReturn(List.of(BANNOCK_ST));
		given(this.openData.dogParksWithin(any(GeoPoint.class), eq(5000)))
			.willReturn(List.of(park("Railyard Dog Park", 39.75821135711727, -105.00130526145398, true),
					park("Skyline Park", 39.74924133996786, -104.99428383699461, null),
					park("Zeckendorf Place", 39.7270421957392, -104.98653566273461, false)));

		this.graphQl.document("{ owner(id: 11) { nearestDogParks(first: 2) { name distanceInMetres water } } }")
			.execute()
			.path("owner.nearestDogParks[*].name")
			.entityList(String.class)
			.containsExactly("Skyline Park", "Zeckendorf Place")
			.path("owner.nearestDogParks[0].distanceInMetres")
			.entity(Integer.class)
			.isEqualTo(1184)
			.path("owner.nearestDogParks[0].water")
			.valueIsNull();
	}

	@Test
	void ownerOutsideDenverCostsNoCall() {
		owner("110 W. Liberty St.", "Madison");

		this.graphQl.document("{ owner(id: 11) { nearestDogParks { name } } }")
			.execute()
			.path("owner.nearestDogParks")
			.entityList(Object.class)
			.hasSize(0);

		verifyNoInteractions(this.geocoder, this.openData);
	}

	@Test
	void addressTheGeocoderCannotFindHasNoParks() {
		owner("1 Nowhere Lane", "Denver");
		given(this.geocoder.geocode("1 Nowhere Lane, Denver, CO")).willReturn(List.of());

		this.graphQl.document("{ owner(id: 11) { nearestDogParks { name } } }")
			.execute()
			.path("owner.nearestDogParks")
			.entityList(Object.class)
			.hasSize(0);

		verifyNoInteractions(this.openData);
	}

	@Test
	void rejectsAFirstOutsideOneToTen() {
		owner("1437 Bannock St", "Denver");

		this.graphQl.document("{ owner(id: 11) { nearestDogParks(first: 11) { name } } }")
			.execute()
			.errors()
			.satisfy((errors) -> assertThat(errors).singleElement().satisfies((error) -> {
				assertThat(error.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
				assertThat(error.getMessage()).isEqualTo("first must be between 1 and 10");
			}));

		verifyNoInteractions(this.geocoder);
	}

	@Test
	void keepsTheOwnerWhenTheGeocoderCannotBeReached() {
		owner("1437 Bannock St", "Denver");
		given(this.geocoder.geocode("1437 Bannock St, Denver, CO"))
			.willThrow(new UpstreamException(CensusGeocoder.CENSUS, "Read timed out"));

		this.graphQl.document("{ owner(id: 11) { lastName nearestDogParks { name } } }")
			.execute()
			.errors()
			.satisfy((errors) -> assertThat(errors).singleElement().satisfies((error) -> {
				assertThat(error.getMessage()).isEqualTo("The Census geocoder is unavailable");
				assertThat(error.getErrorType().toString()).isEqualTo("UPSTREAM_UNAVAILABLE");
				assertThat(error.getPath()).isEqualTo("owner.nearestDogParks");
			}))
			.path("owner.lastName")
			.entity(String.class)
			.isEqualTo("Next")
			.path("owner.nearestDogParks")
			.valueIsNull();
	}

	@Test
	void filtersTheDogParksOnWater() {
		given(this.openData.dogParks()).willReturn(List.of(park("Railyard Dog Park", 39.758, -105.001, true),
				park("Skyline Park", 39.749, -104.994, null), park("Zeckendorf Place", 39.727, -104.987, false)));

		this.graphQl.document("{ withWater: denverDogParks(water: true) { name } all: denverDogParks { name } }")
			.execute()
			.path("withWater[*].name")
			.entityList(String.class)
			.containsExactly("Railyard Dog Park")
			.path("all[*].name")
			.entityList(String.class)
			.hasSize(3);
	}

	@Test
	void countsTheAnimalCasesOnlyWhenTheTotalsAreSelected() {
		given(this.openData.animalCases(any(AnimalCaseFilter.class), anyLong(), anyInt())).willReturn(List.of());
		given(this.openData.countAnimalCases(any(AnimalCaseFilter.class))).willReturn(114);

		this.graphQl.document("{ denverAnimalServiceRequests(caseTypes: [BARKING_DOG]) { requests { id } } }")
			.execute();
		verify(this.openData, never()).countAnimalCases(any(AnimalCaseFilter.class));

		this.graphQl
			.document("{ denverAnimalServiceRequests(caseTypes: [BARKING_DOG], page: 2, size: 50) "
					+ "{ page totalPages totalRequests } }")
			.execute()
			.path("denverAnimalServiceRequests.totalRequests")
			.entity(Integer.class)
			.isEqualTo(114)
			.path("denverAnimalServiceRequests.totalPages")
			.entity(Integer.class)
			.isEqualTo(3);
		verify(this.openData).animalCases(any(AnimalCaseFilter.class), eq(50L), eq(50));
	}

	@Test
	void searchesTheKennelLicensesByNameAndAddress() {
		given(this.openData.kennelLicenses()).willReturn(List.of(
				new DenverKennelLicense("2001-BFN-63777", "TENDER TOUCH ANIMAL HOSPITAL", "TENDER TOUCH",
						"License Issued - Active", LocalDate.of(2027, 10, 15), "350 KALAMATH ST", "80223", 39.72,
						-105.0),
				new DenverKennelLicense("2015-BFN-0003671", "BROCKER LLC", "EARTHDOG DENVER", "License Issued - Active",
						LocalDate.of(2027, 7, 1), "370 N KALAMATH ST", "80223", 39.72, -105.0),
				new DenverKennelLicense("2026-BFN-0010103", "Cat People Company", "Cat People",
						"License Issued - Active", LocalDate.of(2027, 6, 7), "5075 LEETSDALE Dr", "80246", 39.71,
						-104.93)));

		this.graphQl
			.document("{ denverKennelLicenses(search: \"kalamath\", size: 1) "
					+ "{ totalLicenses totalPages licenses { tradeName } } }")
			.execute()
			.path("denverKennelLicenses.totalLicenses")
			.entity(Integer.class)
			.isEqualTo(2)
			.path("denverKennelLicenses.totalPages")
			.entity(Integer.class)
			.isEqualTo(2)
			.path("denverKennelLicenses.licenses[0].tradeName")
			.entity(String.class)
			.isEqualTo("TENDER TOUCH");
	}

}
