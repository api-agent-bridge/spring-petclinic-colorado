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
package org.springframework.samples.petclinic.nlr;

import org.junit.jupiter.api.Test;

import org.springframework.samples.petclinic.upstream.MockUpstreamTests;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * Queries the NLR Alternative Fuel Stations API through GraphQL, against the recorded
 * answer.
 */
class NlrMockUpstreamTests extends MockUpstreamTests {

	@Test
	void chargersNearTheCityAndCountyBuilding() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  nlrFuelStationsNear(latitude: 39.73904, longitude: -104.990312, radiusInMiles: 1,
				      fuelType: ELECTRIC, size: 3) {
				    totalStations stations { name evNetwork evLevel2Ports distanceInMiles }
				  }
				}""")
			.execute()
			.path("nlrFuelStationsNear.totalStations")
			.entity(Integer.class)
			.isEqualTo(129)
			.path("nlrFuelStationsNear.stations")
			.entityList(Object.class)
			.hasSize(3)
			.path("nlrFuelStationsNear.stations[0].name")
			.entity(String.class)
			.isEqualTo("CCD CHARGING CITY HALL 1")
			.path("nlrFuelStationsNear.stations[0].evNetwork")
			.entity(String.class)
			.isEqualTo("ChargePoint Network");

		// the key travels in a header, and the URL holds the search only
		this.mockUpstreams.verify(getRequestedFor(urlPathEqualTo("/nlr/api/alt-fuel-stations/v1/nearest.json"))
			.withHeader("X-Api-Key", matching(".+"))
			.withQueryParam("fuel_type", equalTo("ELEC"))
			.withQueryParam("offset", equalTo("0")));
	}

}
