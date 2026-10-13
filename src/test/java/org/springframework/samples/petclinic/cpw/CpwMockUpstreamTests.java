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
package org.springframework.samples.petclinic.cpw;

import org.junit.jupiter.api.Test;

import org.springframework.samples.petclinic.upstream.MockUpstreamTests;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.notContaining;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * Queries the species data of Colorado Parks and Wildlife through GraphQL, against the
 * recorded answers.
 */
class CpwMockUpstreamTests extends MockUpstreamTests {

	@Test
	void bearsAndMountainLionsMeetPeopleInBoulder() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  cpwWildlifeAt(latitude: 40.015, longitude: -105.2705, species: "human conflict") {
				    species kind activityCode
				  }
				}""")
			.execute()
			.path("cpwWildlifeAt[*].species")
			.entityList(String.class)
			.containsExactly("Black Bear", "Mountain Lion")
			.path("cpwWildlifeAt[*].kind")
			.entityList(String.class)
			.containsExactly("Human Conflict Area", "Human Conflict Area")
			.path("cpwWildlifeAt[0].activityCode")
			.entity(String.class)
			.isEqualTo("BBHCA");

		// the species narrows the layers the point query asks, here to the two conflict
		// areas, and ArcGIS takes the point as longitude first
		this.mockUpstreams
			.verify(postRequestedFor(urlPathEqualTo("/cpw/arcgis/rest/services/CPWSpeciesData/FeatureServer/query"))
				.withRequestBody(containing("geometry=-105.2705%2C40.015"))
				.withRequestBody(containing("%22layerId%22%3A20%2C"))
				.withRequestBody(notContaining("%22layerId%22%3A22%2C")));
	}

	@Test
	void everyLayerAtAPointInBoulder() {
		this.graphQl.document("{ cpwWildlifeAt(latitude: 40.015, longitude: -105.2705) { layerName } }")
			.execute()
			.path("cpwWildlifeAt")
			.entityList(Object.class)
			.hasSize(60);
	}

	@Test
	void layerNamesSplitIntoSpeciesAndKind() {
		this.graphQl.document("{ cpwWildlifeLayers(species: \"mountain lion\") { layerName species kind } }")
			.execute()
			.path("cpwWildlifeLayers[*].kind")
			.entityList(String.class)
			.containsExactly("Human Conflict Area", "Overall Range", "Peripheral Range")
			.path("cpwWildlifeLayers[0].species")
			.entity(String.class)
			.isEqualTo("Mountain Lion");
	}

}
