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

import org.junit.jupiter.api.Test;

import org.springframework.samples.petclinic.upstream.MockUpstreamTests;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Queries the Denver fields in mock mode, against responses recorded from Denver's open
 * data and the Census geocoder on 2026-10-12.
 */
class DenverMockUpstreamTests extends MockUpstreamTests {

	@Test
	void dogParksNearestToTheOwnerInDenver() {
		this.graphQl.document("""
				{
				  owners(lastName: "Next") {
				    owners { firstName nearestDogParks { name distanceInMetres shade water condition } }
				  }
				}""")
			.execute()
			.path("owners.owners[0].nearestDogParks[*].name")
			.entityList(String.class)
			.containsExactly("Skyline Park", "Zeckendorf Place", "Lawson (Sonny) Park")
			.path("owners.owners[0].nearestDogParks[0].distanceInMetres")
			.entity(Integer.class)
			.isEqualTo(1184)
			// Denver has not recorded shade or water for Skyline Park
			.path("owners.owners[0].nearestDogParks[0].water")
			.valueIsNull()
			// the city writes some conditions as a score
			.path("owners.owners[0].nearestDogParks[1].condition")
			.entity(String.class)
			.isEqualTo("80");
	}

	@Test
	void ownersInWisconsinCostNoCall() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("{ owners { owners { lastName nearestDogParks { name } } } }")
			.execute()
			.path("owners.owners[*].nearestDogParks[*]")
			.entityList(Object.class)
			.hasSize(0);

		this.mockUpstreams.verify(0, anyRequestedFor(urlPathMatching("/(census|denver)/.*")));
	}

	@Test
	void dogParksWithDrinkingWater() {
		this.graphQl.document("{ denverDogParks(water: true) { name } }")
			.execute()
			.path("denverDogParks[*].name")
			.entityList(String.class)
			.containsExactly("Green Valley Ranch East", "Greenway Park", "Parkfield", "Railyard Dog Park",
					"Sanderson Gulch", "Willow Bark Dog Park");
	}

	@Test
	void barkingDogsInSeptember() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  denverAnimalServiceRequests(caseTypes: [BARKING_DOG], createdFrom: "2026-09-01",
				      createdTo: "2026-09-30", size: 2) {
				    totalRequests
				    requests { caseType createdDate zipCode }
				  }
				}""")
			.execute()
			.path("denverAnimalServiceRequests.totalRequests")
			.entity(Integer.class)
			.isEqualTo(114)
			.path("denverAnimalServiceRequests.requests[*].caseType")
			.entityList(String.class)
			.containsExactly("BARKING_DOG", "BARKING_DOG")
			.path("denverAnimalServiceRequests.requests[0].createdDate")
			.entity(String.class)
			.isEqualTo("2026-09-30")
			// some cases hold a ZIP+4 code
			.path("denverAnimalServiceRequests.requests[0].zipCode")
			.entity(String.class)
			.isEqualTo("80236-2427");

		// the last day is included by asking for the days before the next one
		this.mockUpstreams.verify(getRequestedFor(
				urlPathEqualTo("/denver/arcgis/rest/services/ODC_service_requests_311/FeatureServer/53/query"))
			.withQueryParam("where",
					equalTo("Case_Summary IN ('Barking Dog') AND Case_Created_Date >= DATE '2026-09-01'"
							+ " AND Case_Created_Date < DATE '2026-10-01'"))
			.withQueryParam("resultRecordCount", equalTo("2")));
	}

	@Test
	void animalCasesByTypeInSeptember() {
		this.graphQl.document("""
				{
				  denverAnimalServiceRequestCounts(createdFrom: "2026-09-01", createdTo: "2026-09-30") {
				    caseType cases
				  }
				}""")
			.execute()
			.path("denverAnimalServiceRequestCounts[0].caseType")
			.entity(String.class)
			.isEqualTo("ADOPTION_OR_HOLD_INQUIRY")
			.path("denverAnimalServiceRequestCounts[?(@.caseType == 'BARKING_DOG')].cases")
			.entityList(Integer.class)
			.containsExactly(114);
	}

	@Test
	void kennelLicensesOfAnimalHospitals() {
		this.graphQl.document("""
				{
				  denverKennelLicenses(search: "Kalamath") {
				    totalLicenses
				    licenses { tradeName address latitude longitude }
				  }
				}""")
			.execute()
			.path("denverKennelLicenses.totalLicenses")
			.entity(Integer.class)
			.isEqualTo(3)
			.path("denverKennelLicenses.licenses[2].tradeName")
			.entity(String.class)
			.isEqualTo("TENDER TOUCH")
			// converted from the State Plane feet that Denver stores
			.path("denverKennelLicenses.licenses[2].latitude")
			.entity(Double.class)
			.satisfies((latitude) -> assertThat(latitude).isCloseTo(39.7218, within(0.0001)));
	}

}
