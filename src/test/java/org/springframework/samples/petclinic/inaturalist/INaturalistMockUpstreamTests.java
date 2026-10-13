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
package org.springframework.samples.petclinic.inaturalist;

import org.junit.jupiter.api.Test;

import org.springframework.samples.petclinic.upstream.MockUpstreamTests;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * Queries the iNaturalist fields in mock mode, where the answers come from responses
 * recorded from the live API.
 */
class INaturalistMockUpstreamTests extends MockUpstreamTests {

	@Test
	void mammalsMostObservedInDenverCounty() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  inatObservationSpeciesCounts(place_id: [205], iconic_taxa: [Mammalia], size: 3) {
				    total_results
				    results { count taxon { name preferred_common_name } }
				  }
				}""")
			.execute()
			.path("inatObservationSpeciesCounts.total_results")
			.entity(Integer.class)
			.isEqualTo(138)
			.path("inatObservationSpeciesCounts.results[0].taxon.preferred_common_name")
			.entity(String.class)
			.isEqualTo("Eastern Fox Squirrel")
			.path("inatObservationSpeciesCounts.results[0].count")
			.entity(Integer.class)
			.isEqualTo(1343);

		// the page and the size of the schema become the page and per_page of
		// iNaturalist,
		// and a list repeats its parameter
		this.mockUpstreams.verify(getRequestedFor(urlPathEqualTo("/inat/v1/observations/species_counts"))
			.withQueryParam("place_id", equalTo("205"))
			.withQueryParam("iconic_taxa", equalTo("Mammalia"))
			.withQueryParam("page", equalTo("1"))
			.withQueryParam("per_page", equalTo("3")));
	}

	@Test
	void dogSightingsInDenverCountyByMonth() {
		this.graphQl.document("""
				{
				  inatObservationHistogram(taxon_id: ["47144"], place_id: [205], interval: month_of_year) {
				    results { month_of_year { key value } }
				  }
				}""")
			.execute()
			// iNaturalist answers with counts keyed by month, and the schema has a list
			// of
			// entries
			.path("inatObservationHistogram.results.month_of_year")
			.entityList(Object.class)
			.hasSize(12)
			.path("inatObservationHistogram.results.month_of_year[3].key")
			.entity(String.class)
			.isEqualTo("4")
			.path("inatObservationHistogram.results.month_of_year[3].value")
			.entity(Integer.class)
			.isEqualTo(45);
	}

	@Test
	void domesticDogByTaxonId() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("{ inatTaxon(id: \"47144\") { results { name rank preferred_common_name } } }")
			.execute()
			.path("inatTaxon.results[0].name")
			.entity(String.class)
			.isEqualTo("Canis familiaris")
			.path("inatTaxon.results[0].preferred_common_name")
			.entity(String.class)
			.isEqualTo("Domestic Dog");

		this.mockUpstreams.verify(getRequestedFor(urlPathEqualTo("/inat/v1/taxa/47144")));
	}

	@Test
	void speciesConfusedWithTheCoyote() {
		this.graphQl.document("""
				{
				  inatSimilarSpecies(taxon_id: 42051, place_id: [34]) {
				    results { count taxon { preferred_common_name } }
				  }
				}""")
			.execute()
			.path("inatSimilarSpecies.results[*].taxon.preferred_common_name")
			.entityList(String.class)
			.contains("Red Fox", "Mountain Lion", "Domestic Dog");
	}

}
