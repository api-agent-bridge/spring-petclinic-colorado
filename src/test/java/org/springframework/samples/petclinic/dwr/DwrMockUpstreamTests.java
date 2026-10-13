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
package org.springframework.samples.petclinic.dwr;

import org.junit.jupiter.api.Test;

import org.springframework.samples.petclinic.upstream.MockUpstreamTests;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Queries the Colorado Division of Water Resources through GraphQL, answered by the
 * recorded responses of the <code>mock</code> profile.
 */
class DwrMockUpstreamTests extends MockUpstreamTests {

	@Test
	void liveFlowOfTheSouthPlatteInDenver() {
		this.graphQl.document("""
				{
				  dwrTelemetryStations(county: "DENVER") {
				    ResultCount
				    ResultList { abbrev stationName parameter measValue units measDateTime }
				  }
				}""")
			.execute()
			.path("dwrTelemetryStations.ResultCount")
			.entity(Integer.class)
			.isEqualTo(8)
			.path("dwrTelemetryStations.ResultList[6].stationName")
			.entity(String.class)
			.isEqualTo("SOUTH PLATTE RIVER AT DENVER")
			.path("dwrTelemetryStations.ResultList[6].measValue")
			.entity(Double.class)
			.isEqualTo(73.2)
			.path("dwrTelemetryStations.ResultList[6].units")
			.entity(String.class)
			.isEqualTo("CFS");
	}

	@Test
	void pageAndSizeBecomePageIndexAndPageSizeWithTheJsonForcedFormat() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  dwrClimateStationFrostDates(stationNum: 62, minCalYear: 2023, page: 2, size: 4) {
				    ResultList { calYear l32s f32f }
				  }
				}""")
			.execute()
			.path("dwrClimateStationFrostDates.ResultList[3].calYear")
			.entity(Integer.class)
			.isEqualTo(2026)
			.path("dwrClimateStationFrostDates.ResultList[3].l32s")
			.entity(String.class)
			.isEqualTo("2026-05-06T00:00:00");

		// the argument minCalYear is DWR's min-calYear, and every request asks for the
		// format that answers an empty search with an empty page
		this.mockUpstreams
			.verify(getRequestedFor(urlPathEqualTo("/dwr/Rest/GET/api/v2/climatedata/climatestationfrostdates"))
				.withQueryParam("stationNum", equalTo("62"))
				.withQueryParam("min-calYear", equalTo("2023"))
				.withQueryParam("pageIndex", equalTo("2"))
				.withQueryParam("pageSize", equalTo("4"))
				.withQueryParam("format", equalTo("jsonforced")));
	}

	@Test
	void monthWrittenAsADateIsRefusedWithDwrsReason() {
		this.graphQl.document("""
				{ dwrMonthlyDiversionRecords(wdid: "0200802", minDataMeasDate: "2025-01") { ResultCount } }""")
			.execute()
			.errors()
			.satisfy((errors) -> assertThat(errors).singleElement()
				.satisfies((error) -> assertThat(error.getMessage()).contains("should be 'MM/yyyy'")));
	}

}
