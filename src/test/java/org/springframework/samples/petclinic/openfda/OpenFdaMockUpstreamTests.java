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
package org.springframework.samples.petclinic.openfda;

import org.junit.jupiter.api.Test;

import org.springframework.samples.petclinic.upstream.MockUpstreamTests;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * Queries openFDA's animal and veterinary adverse events through GraphQL, against the
 * recorded answers.
 */
class OpenFdaMockUpstreamTests extends MockUpstreamTests {

	@Test
	void aDogReportNamesTheIngredientAndMasksTheBrand() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  openFdaAnimalAdverseEvents(species: "Dog", breed: "Labrador", drug: "Sarolaner", size: 1) {
				    totalEvents
				    events {
				      id receivedDate serious
				      animal { breed { components } }
				      drugs { brandName activeIngredients { name } }
				      reactions { name }
				    }
				  }
				}""")
			.execute()
			.path("openFdaAnimalAdverseEvents.totalEvents")
			.entity(Integer.class)
			.isEqualTo(8651)
			.path("openFdaAnimalAdverseEvents.events")
			.entityList(Object.class)
			.hasSize(1)
			.path("openFdaAnimalAdverseEvents.events[0].receivedDate")
			.entity(String.class)
			.isEqualTo("2026-06-26")
			// openFDA sends "true" as text
			.path("openFdaAnimalAdverseEvents.events[0].serious")
			.entity(Boolean.class)
			.isEqualTo(true)
			.path("openFdaAnimalAdverseEvents.events[0].animal.breed.components")
			.entityList(String.class)
			.containsExactly("Retriever - Labrador")
			.path("openFdaAnimalAdverseEvents.events[0].drugs[0].brandName")
			.entity(String.class)
			.isEqualTo("MSK")
			.path("openFdaAnimalAdverseEvents.events[0].drugs[0].activeIngredients[0].name")
			.entity(String.class)
			.isEqualTo("Pyrantel Pamoate;Sarolaner");

		this.mockUpstreams.verify(getRequestedFor(urlPathEqualTo("/openfda/animalandveterinary/event.json"))
			.withQueryParam("search", equalTo("animal.species:\"Dog\" AND animal.breed.breed_component:\"Labrador\""
					+ " AND drug.active_ingredients.name:\"Sarolaner\"")));
	}

	@Test
	void aSearchWithoutAMatchIsEmpty() {
		// openFDA answers 404 Not Found
		this.graphQl.document("{ openFdaAnimalAdverseEvents(species: \"Unicorn\") { totalEvents events { id } } }")
			.execute()
			.path("openFdaAnimalAdverseEvents.totalEvents")
			.entity(Integer.class)
			.isEqualTo(0)
			.path("openFdaAnimalAdverseEvents.events")
			.entityList(Object.class)
			.hasSize(0);
	}

	@Test
	void theMostReportedReactionsOfCats() {
		this.graphQl.document("""
				{ openFdaAnimalAdverseEventCounts(countBy: REACTION, species: "Cat", first: 2) { term count } }""")
			.execute()
			.path("openFdaAnimalAdverseEventCounts[*].term")
			.entityList(String.class)
			.containsExactly("Vomiting", "Lack of efficacy (ectoparasite) - flea")
			.path("openFdaAnimalAdverseEventCounts[0].count")
			.entity(Long.class)
			.isEqualTo(18708L);
	}

}
