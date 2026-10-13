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
package org.springframework.samples.petclinic.awdb;

import org.junit.jupiter.api.Test;

import org.springframework.samples.petclinic.upstream.MockUpstreamTests;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * Queries the NRCS Air and Water Database through GraphQL in mock mode, where the answers
 * come from responses recorded from its REST API.
 */
class AwdbMockUpstreamTests extends MockUpstreamTests {

	@Test
	void snowDepthAtBerthoudSummit() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  awdbData(stationTriplets: "335:CO:SNTL", elements: "WTEQ,SNWD", duration: DAILY,
				           beginDate: "2026-04-01", endDate: "2026-04-07") {
				    stationTriplet
				    data { stationElement { elementCode storedUnitCode } values { date value } }
				  }
				}""")
			.execute()
			.path("awdbData[0].stationTriplet")
			.entity(String.class)
			.isEqualTo("335:CO:SNTL")
			.path("awdbData[0].data[0].stationElement.elementCode")
			.entity(String.class)
			.isEqualTo("SNWD")
			.path("awdbData[0].data[0].values[0].date")
			.entity(String.class)
			.isEqualTo("2026-04-01")
			.path("awdbData[0].data[0].values[0].value")
			.entity(Double.class)
			.isEqualTo(21.0);

		this.mockUpstreams.verify(getRequestedFor(urlPathEqualTo("/awdb/services/v1/data"))
			.withQueryParam("stationTriplets", equalTo("335:CO:SNTL"))
			.withQueryParam("elements", equalTo("WTEQ,SNWD"))
			.withQueryParam("duration", equalTo("DAILY")));
	}

	@Test
	void streamflowForecastOfTheBigThompsonByExceedanceProbability() {
		this.graphQl.document("""
				{
				  awdbForecasts(stationTriplets: "06738000:CO:USGS") {
				    forecastPointName
				    data { forecastPeriod forecastValues { key value } }
				  }
				}""")
			.execute()
			.path("awdbForecasts[0].forecastPointName")
			.entity(String.class)
			.isEqualTo("Big Thompson R at Canyon Mouth")
			// the service answers with an object keyed by probability, and the schema
			// has a list of entries
			.path("awdbForecasts[0].data[0].forecastValues[?(@.key == '50')].value")
			.entityList(Double.class)
			.containsExactly(84.0);
	}

}
