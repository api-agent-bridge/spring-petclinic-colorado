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
package org.springframework.samples.petclinic.cdphe;

import org.junit.jupiter.api.Test;

import org.springframework.samples.petclinic.upstream.MockUpstreamTests;

/**
 * Queries the animal disease data of the Colorado Department of Public Health and
 * Environment through GraphQL, answered by the SOAP responses recorded from the state's
 * ArcGIS Server.
 */
class CdpheMockUpstreamTests extends MockUpstreamTests {

	@Test
	void petsThatTestedPositiveIn2017() {
		this.graphQl.document("""
				{
				  cdpheAnimalsTestedPositive(specimenType: DOMESTIC_ANIMAL) {
				    totalAnimals
				    animals { species disease county collectedDate }
				  }
				}""")
			.execute()
			.path("cdpheAnimalsTestedPositive.totalAnimals")
			.entity(Integer.class)
			.isEqualTo(9)
			.path("cdpheAnimalsTestedPositive.animals[0].species")
			.entity(String.class)
			.isEqualTo("Cat")
			.path("cdpheAnimalsTestedPositive.animals[0].disease")
			.entity(String.class)
			.isEqualTo("PLAGUE")
			.path("cdpheAnimalsTestedPositive.animals[0].collectedDate")
			.entity(String.class)
			.isEqualTo("2017-02-13");
	}

	@Test
	void rabidAnimalsInDenverByYear() {
		this.graphQl.document("""
				{
				  cdpheAnimalDiseaseCases(county: "Denver", size: 3) {
				    totalCounts
				    counts { disease year total population ratePer100kPeople }
				  }
				}""")
			.execute()
			.path("cdpheAnimalDiseaseCases.totalCounts")
			.entity(Integer.class)
			.isEqualTo(30)
			// the newest year comes first, and rabies first within a year
			.path("cdpheAnimalDiseaseCases.counts[0].disease")
			.entity(String.class)
			.isEqualTo("RABIES")
			.path("cdpheAnimalDiseaseCases.counts[0].year")
			.entity(Integer.class)
			.isEqualTo(2025)
			.path("cdpheAnimalDiseaseCases.counts[0].total")
			.entity(Integer.class)
			.isEqualTo(5)
			// the state divides the animals by the people of the county
			.path("cdpheAnimalDiseaseCases.counts[0].ratePer100kPeople")
			.entity(Double.class)
			.isEqualTo(0.68);
	}

	@Test
	void countiesWithRabidAnimalsIn2025HighestFirst() {
		this.graphQl.document("""
				{
				  cdpheAnimalDiseaseCases(disease: RABIES, fromYear: 2025, toYear: 2025, onlyWithCases: true, size: 3) {
				    totalCounts
				    counts { county total }
				  }
				}""")
			.execute()
			.path("cdpheAnimalDiseaseCases.totalCounts")
			.entity(Integer.class)
			.isEqualTo(24)
			.path("cdpheAnimalDiseaseCases.counts[*].county")
			.entityList(String.class)
			.containsExactly("Jefferson", "Larimer", "Boulder");
	}

	@Test
	void countyAppliesToCountsPerCountyOnly() {
		this.graphQl.document("{ cdpheAnimalDiseaseCases(groupedBy: STATE, county: \"Denver\") { totalCounts } }")
			.execute()
			.errors()
			.expect((error) -> error.getMessage().equals("county applies only to counts grouped by COUNTY"))
			.verify();
	}

}
