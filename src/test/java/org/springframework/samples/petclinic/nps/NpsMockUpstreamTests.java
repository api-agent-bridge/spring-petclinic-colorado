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
package org.springframework.samples.petclinic.nps;

import org.junit.jupiter.api.Test;

import org.springframework.samples.petclinic.upstream.MockUpstreamTests;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * Queries the National Park Service fields in mock mode, where the answers come from
 * responses recorded from the live API.
 */
class NpsMockUpstreamTests extends MockUpstreamTests {

	@Test
	void petRulesOfThingsToDoInRockyMountainNationalPark() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  npsThingsToDo(parkCode: "romo") {
				    total
				    data { title arePetsPermitted petsDescription }
				  }
				}""")
			.execute()
			.path("npsThingsToDo.total")
			.entity(String.class)
			.isEqualTo("12")
			.path("npsThingsToDo.data[0].arePetsPermitted")
			.entity(String.class)
			.isEqualTo("false")
			.path("npsThingsToDo.data[1].title")
			.entity(String.class)
			.isEqualTo("Enjoy a Scenic Drive on Old Fall River Road")
			// the NPS sends yes and no as text
			.path("npsThingsToDo.data[1].arePetsPermitted")
			.entity(String.class)
			.isEqualTo("true");

		// the page and the size of the schema become the start and the limit of the NPS,
		// and every call carries the key
		this.mockUpstreams.verify(
				getRequestedFor(urlPathEqualTo("/nps/api/v1/thingstodo")).withQueryParam("parkCode", equalTo("romo"))
					.withQueryParam("start", equalTo("0"))
					.withQueryParam("limit", equalTo("10"))
					.withHeader("X-Api-Key", matching(".+")));
	}

	@Test
	void severalParkCodesTravelAsOneParameter() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("{ npsParks(parkCode: \"romo,grsa\", size: 2) { data { parkCode fullName } } }")
			.execute()
			.path("npsParks.data[*].parkCode")
			.entityList(String.class)
			.contains("romo", "grsa");

		// the NPS answers an empty list when a parameter repeats, so the codes stay in
		// one
		// value separated by commas
		this.mockUpstreams.verify(
				getRequestedFor(urlPathEqualTo("/nps/api/v1/parks")).withQueryParam("parkCode", equalTo("romo,grsa"))
					.withQueryParam("limit", equalTo("2")));
	}

	@Test
	void parkAlertsInColorado() {
		this.graphQl.document("{ npsAlerts(stateCode: \"CO\") { total data { parkCode category title } } }")
			.execute()
			.path("npsAlerts.total")
			.entity(String.class)
			.isEqualTo("25")
			.path("npsAlerts.data[0].category")
			.entity(String.class)
			.isEqualTo("Park Closure")
			.path("npsAlerts.data[0].title")
			.entity(String.class)
			.isEqualTo("Echo Park Road and Campground Closure");
	}

}
