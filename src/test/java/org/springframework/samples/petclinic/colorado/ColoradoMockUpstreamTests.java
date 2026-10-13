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
package org.springframework.samples.petclinic.colorado;

import org.junit.jupiter.api.Test;

import org.springframework.samples.petclinic.upstream.MockUpstreamTests;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Queries the license register and the shelter reports of data.colorado.gov through
 * GraphQL, against the recorded answers. The recorded licenses carry placeholder names.
 */
class ColoradoMockUpstreamTests extends MockUpstreamTests {

	private static final String LICENSES = "/colorado/resource/7s5z-vewr.json";

	@Test
	void veterinaryLicensesByLastName() {
		this.graphQl.document("""
				{
				  coloradoVeterinaryLicenses(lastName: "Leary") {
				    totalLicenses
				    licenses { lastName licenseType status degrees disciplinaryAction { action effectiveDate } }
				  }
				}""")
			.execute()
			.path("coloradoVeterinaryLicenses.totalLicenses")
			.entity(Integer.class)
			.isEqualTo(2)
			.path("coloradoVeterinaryLicenses.licenses[0].lastName")
			.entity(String.class)
			.isEqualTo("Leary")
			.path("coloradoVeterinaryLicenses.licenses[0].licenseType")
			.entity(String.class)
			.isEqualTo("VETERINARY_TECHNICIAN")
			// the register writes this date as 08/19/2025
			.path("coloradoVeterinaryLicenses.licenses[0].disciplinaryAction.effectiveDate")
			.entity(String.class)
			.isEqualTo("2025-08-19")
			.path("coloradoVeterinaryLicenses.licenses[1].disciplinaryAction")
			.valueIsNull();
	}

	@Test
	void aNameWithoutALicenseCostsOnlyTheCount() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  coloradoVeterinaryLicenses(firstName: "Helen", lastName: "Leary") {
				    totalLicenses licenses { lastName }
				  }
				}""")
			.execute()
			.path("coloradoVeterinaryLicenses.totalLicenses")
			.entity(Integer.class)
			.isEqualTo(0)
			.path("coloradoVeterinaryLicenses.licenses")
			.entityList(Object.class)
			.hasSize(0);

		this.mockUpstreams.verify(1, getRequestedFor(urlPathEqualTo(LICENSES)));
	}

	@Test
	void activeVeterinariansInDenver() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  coloradoVeterinaryLicenses(city: "denver", licenseType: VETERINARIAN, status: "Active", size: 3) {
				    totalLicenses totalPages licenses { city status }
				  }
				}""")
			.execute()
			.path("coloradoVeterinaryLicenses.totalLicenses")
			.entity(Integer.class)
			.isEqualTo(462)
			.path("coloradoVeterinaryLicenses.totalPages")
			.entity(Integer.class)
			.isEqualTo(154)
			.path("coloradoVeterinaryLicenses.licenses")
			.entityList(Object.class)
			.hasSize(3);

		// the filters become one SoQL where clause, which matches in upper case
		this.mockUpstreams.verify(getRequestedFor(urlPathEqualTo(LICENSES)).withQueryParam("$where", equalTo(
				"licensetype = 'VET' AND upper(city) = 'DENVER' AND upper(licensestatusdescription) = 'ACTIVE'")));
	}

	@Test
	void shelterReportOfDenverAnimalProtection() {
		this.graphQl.document("""
				{
				  coloradoShelterReports(name: "Denver Animal Protection") {
				    reports { facilityName year adultDogs { stray adopted returnedToOwner euthanized inShelterAtEnd } }
				  }
				}""")
			.execute()
			.path("coloradoShelterReports.reports[0].facilityName")
			.entity(String.class)
			.isEqualTo("Denver Animal Protection")
			.path("coloradoShelterReports.reports[0].year")
			.entity(Integer.class)
			.isEqualTo(2025)
			.path("coloradoShelterReports.reports[0].adultDogs.stray")
			.entity(Integer.class)
			.isEqualTo(2886)
			.path("coloradoShelterReports.reports[0].adultDogs.adopted")
			.entity(Integer.class)
			.isEqualTo(1928)
			.path("coloradoShelterReports.reports[0].adultDogs.euthanized")
			.entity(Integer.class)
			.isEqualTo(1214)
			.path("coloradoShelterReports.reports[0].adultDogs.inShelterAtEnd")
			.entity(Integer.class)
			.isEqualTo(101);
	}

	@Test
	void reportsOfAnotherYearAreReadByPosition() {
		// the 2024 dataset names its columns _2024_adult_dogs_adoption and so on
		this.graphQl.document("""
				{
				  coloradoShelterReports(year: 2024, name: "denver") {
				    totalReports reports { facilityName adultDogs { adopted } }
				  }
				}""")
			.execute()
			.path("coloradoShelterReports.totalReports")
			.entity(Integer.class)
			.isEqualTo(6)
			.path("coloradoShelterReports.reports[0].facilityName")
			.entity(String.class)
			.isEqualTo("Denver Animal Shelter")
			.path("coloradoShelterReports.reports[0].adultDogs.adopted")
			.entity(Integer.class)
			.isEqualTo(1896);
	}

	@Test
	void statewideTotalsOfTwoYears() {
		this.graphQl.document("""
				{
				  latest: coloradoShelterTotals { facilityName year adultDogs { adopted euthanized } }
				  before: coloradoShelterTotals(year: 2024) { adultDogs { adopted euthanized } }
				}""")
			.execute()
			.path("latest.facilityName")
			.valueIsNull()
			.path("latest.year")
			.entity(Integer.class)
			.isEqualTo(2025)
			.path("latest.adultDogs.adopted")
			.entity(Integer.class)
			.isEqualTo(32978)
			.path("before.adultDogs.adopted")
			.entity(Integer.class)
			.isEqualTo(33341)
			.path("before.adultDogs.euthanized")
			.entity(Integer.class)
			.isEqualTo(5519);
	}

	@Test
	void aYearWithoutReportsIsRejected() {
		this.graphQl.document("{ coloradoShelterTotals(year: 2019) { year } }")
			.execute()
			.errors()
			.satisfy((errors) -> assertThat(errors).singleElement()
				.satisfies((error) -> assertThat(error.getMessage()).endsWith("2019 is not one of them.")));
	}

}
