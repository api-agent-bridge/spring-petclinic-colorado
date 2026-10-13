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
package org.springframework.samples.petclinic.nws;

import org.junit.jupiter.api.Test;

import org.springframework.samples.petclinic.upstream.MockUpstreamTests;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * Queries the National Weather Service through GraphQL in mock mode, where the answers
 * come from responses recorded from api.weather.gov.
 */
class NwsMockUpstreamTests extends MockUpstreamTests {

	@Test
	void forecastForDowntownDenverFromItsGridpoint() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  nwsPoint(latitude: 39.7392, longitude: -104.9903) {
				    properties { gridId gridX gridY relativeLocation { properties { city state } } }
				  }
				  nwsGridpointForecast(wfo: BOU, x: 63, y: 62) {
				    properties { periods { name temperature temperatureUnit shortForecast } }
				  }
				}""")
			.execute()
			.path("nwsPoint.properties.gridId")
			.entity(String.class)
			.isEqualTo("BOU")
			.path("nwsPoint.properties.gridX")
			.entity(Integer.class)
			.isEqualTo(63)
			.path("nwsPoint.properties.gridY")
			.entity(Integer.class)
			.isEqualTo(62)
			.path("nwsPoint.properties.relativeLocation.properties.city")
			.entity(String.class)
			.isEqualTo("Denver")
			.path("nwsGridpointForecast.properties.periods[0].name")
			.entity(String.class)
			.isEqualTo("Columbus Day")
			.path("nwsGridpointForecast.properties.periods[0].temperature")
			.entity(Integer.class)
			.isEqualTo(81);

		// the service answers 403 to a request without a User-Agent that names the
		// application
		this.mockUpstreams.verify(getRequestedFor(urlPathEqualTo("/nws/points/39.7392,-104.9903"))
			.withHeader("User-Agent", containing("spring-petclinic-colorado")));
	}

	@Test
	void activeAlertsOfColoradoWithTheSizeAsTheLimit() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  nwsActiveAlerts(area: CO) { features { properties { event parameters { key value } } } }
				  nwsAlerts(area: CO, zone: "COZ040,COC031", size: 2) { features { properties { event } } }
				}""")
			.execute()
			.path("nwsActiveAlerts.features[*].properties.event")
			.entityList(String.class)
			.containsExactly("Flood Advisory", "Special Weather Statement", "Hydrologic Outlook")
			// the parameters of an alert are an object whose keys are data, and the
			// schema has a list of entries
			.path("nwsActiveAlerts.features[0].properties.parameters[?(@.key == 'NWSheadline')].value[0]")
			.entityList(String.class)
			.containsExactly("FLOOD ADVISORY IN EFFECT UNTIL 7 AM MDT THIS MORNING");

		// the size becomes the limit, and a list the service reads comma-separated
		// travels as one value
		this.mockUpstreams.verify(getRequestedFor(urlPathEqualTo("/nws/alerts")).withQueryParam("area", equalTo("CO"))
			.withQueryParam("zone", equalTo("COZ040,COC031"))
			.withQueryParam("limit", equalTo("2")));
	}

	@Test
	void countOfActiveAlertsInColorado() {
		this.graphQl.document("{ nwsActiveAlertCounts { total areas { key value } } }")
			.execute()
			.path("nwsActiveAlertCounts.total")
			.entity(Integer.class)
			.isEqualTo(371)
			.path("nwsActiveAlertCounts.areas[?(@.key == 'CO')].value")
			.entityList(Integer.class)
			.containsExactly(3);
	}

	@Test
	void forecastDiscussionOfTheBoulderOfficeFromAJsonLdList() {
		this.graphQl.document("""
				{
				  nwsTextProductsOfTypeAtLocation(typeId: "AFD", locationId: "BOU") { items { productName } }
				  nwsLatestTextProduct(typeId: "AFD", locationId: "BOU") { issuingOffice productText }
				}""")
			.execute()
			// the service lists them under @graph, which the schema calls items
			.path("nwsTextProductsOfTypeAtLocation.items")
			.entityList(Object.class)
			.hasSize(40)
			.path("nwsTextProductsOfTypeAtLocation.items[0].productName")
			.entity(String.class)
			.isEqualTo("Area Forecast Discussion")
			.path("nwsLatestTextProduct.issuingOffice")
			.entity(String.class)
			.isEqualTo("KBOU")
			.path("nwsLatestTextProduct.productText")
			.entity(String.class)
			.satisfies((text) -> text.contains("National Weather Service Denver/Boulder CO"));
	}

}
