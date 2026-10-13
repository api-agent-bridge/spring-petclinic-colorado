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
package org.springframework.samples.petclinic.usgs;

import org.junit.jupiter.api.Test;

import org.springframework.samples.petclinic.upstream.MockUpstreamTests;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Queries the USGS earthquake catalogue through GraphQL, against the recorded answers.
 */
class UsgsMockUpstreamTests extends MockUpstreamTests {

	@Test
	void earthquakesInColoradoThisYear() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  usgsEarthquakes(startTime: "2026-01-01", size: 2) {
				    totalEarthquakes totalPages earthquakes { title time depthInKilometres }
				  }
				}""")
			.execute()
			.path("usgsEarthquakes.totalEarthquakes")
			.entity(Integer.class)
			.isEqualTo(113)
			.path("usgsEarthquakes.totalPages")
			.entity(Integer.class)
			.isEqualTo(57)
			.path("usgsEarthquakes.earthquakes[0].title")
			.entity(String.class)
			.isEqualTo("M 1.8 - 28 km WSW of Yampa, Colorado")
			// the catalogue sends milliseconds since 1970
			.path("usgsEarthquakes.earthquakes[0].time")
			.entity(String.class)
			.isEqualTo("2026-09-16T06:46:53.706Z");

		// without a point or a box, the search covers the box around Colorado, and the
		// catalogue counts its offset from 1
		this.mockUpstreams.verify(getRequestedFor(urlPathEqualTo("/usgs/fdsnws/event/1/query"))
			.withQueryParam("minlatitude", equalTo("36.99"))
			.withQueryParam("maxlongitude", equalTo("-102.04"))
			.withQueryParam("limit", equalTo("2"))
			.withQueryParam("offset", equalTo("1")));
	}

	@Test
	void theStrongestEarthquakeSince2024() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  usgsEarthquakes(startTime: "2024-01-01", orderBy: MAGNITUDE, size: 1) {
				    earthquakes { magnitude place feltReports }
				  }
				}""")
			.execute()
			.path("usgsEarthquakes.earthquakes")
			.entityList(Object.class)
			.hasSize(1)
			.path("usgsEarthquakes.earthquakes[0].magnitude")
			.entity(Double.class)
			.isEqualTo(3.8)
			.path("usgsEarthquakes.earthquakes[0].place")
			.entity(String.class)
			.isEqualTo("16 km SSE of Stonewall Gap, Colorado");

		// the query does not select the totals, so the count is left out
		this.mockUpstreams.verify(0, getRequestedFor(urlPathEqualTo("/usgs/fdsnws/event/1/count")));
	}

	@Test
	void earthquakesNearDenver() {
		this.graphQl.document("""
				{ usgsEarthquakeCount(startTime: "2024-01-01", latitude: 39.7392, longitude: -104.9903) }""")
			.execute()
			.path("usgsEarthquakeCount")
			.entity(Integer.class)
			.isEqualTo(3);
	}

	@Test
	void aPointAndABoxTogetherAreRejected() {
		this.graphQl.document("""
				{
				  usgsEarthquakeCount(latitude: 39.7, longitude: -105.0,
				    box: { minLatitude: 36.99, maxLatitude: 41.0, minLongitude: -109.06, maxLongitude: -102.04 })
				}""")
			.execute()
			.errors()
			.satisfy((errors) -> assertThat(errors).singleElement()
				.satisfies((error) -> assertThat(error.getMessage())
					.isEqualTo("Give a point or a box, and leave the other out")));
	}

}
