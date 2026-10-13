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

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.samples.petclinic.cdphe.AnimalDiseaseCount.AnimalDisease;
import org.springframework.samples.petclinic.cdphe.AnimalDiseaseCount.CaseGrouping;
import org.springframework.samples.petclinic.cdphe.PositiveAnimal.SpecimenType;
import org.springframework.samples.petclinic.cdphe.PositiveAnimal.ZoonoticDisease;
import org.springframework.samples.petclinic.upstream.UpstreamException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingXPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests the {@link CdpheClient} against the responses recorded from the Colorado
 * Department of Public Health and Environment, served by the same mock server the
 * <code>mock</code> profile uses.
 */
class CdpheClientTests {

	private static final String ANIMALS = "/cdphe/arcgis/services/DCEED/ZOONOTIC_SUMMARY_POSITIVE_ANIMALS_2017/MapServer";

	private static final String CASES = "/cdphe/arcgis/services/OPEN_DATA/cdphe_reportable_disease_dataset/MapServer";

	private static WireMockServer cdphe;

	private CdpheClient client;

	@BeforeAll
	static void startServer() {
		cdphe = new WireMockServer(options().dynamicPort().usingFilesUnderClasspath("mock-upstreams"));
		cdphe.start();
	}

	@AfterAll
	static void stopServer() {
		cdphe.stop();
	}

	@BeforeEach
	void setup() {
		cdphe.resetAll();
		this.client = client("Layers", Duration.ofSeconds(5));
	}

	private CdpheClient client(String mapName, Duration readTimeout) {
		return new CdpheClient(new WebServiceTemplateBuilder(), cdphe.baseUrl() + ANIMALS, 0, cdphe.baseUrl() + CASES,
				1, mapName, Duration.ofSeconds(2), readTimeout);
	}

	@Test
	void findsTheCatsAndDogsWithAndWithoutTheTrailingSpace() {
		List<PositiveAnimal> pets = this.client.animalsTestedPositive(null, SpecimenType.DOMESTIC_ANIMAL, null, null);

		// 7 of them are 'Domestic Animal ' with a trailing space, and 2 are without
		assertThat(pets).hasSize(9).allMatch((pet) -> pet.specimenType() == SpecimenType.DOMESTIC_ANIMAL);
		assertThat(pets).extracting(PositiveAnimal::species).containsOnly("Cat", "Dog");
		cdphe.verify(1, postRequestedFor(urlPathEqualTo(ANIMALS)).withRequestBody(matchingXPath(
				"//*[local-name()='QueryFeatureData'][MapName='Layers'][LayerID='0']/QueryFilter[@*[local-name()='type']='e:QueryFilter']"
						+ "[WhereClause=\"Specimen_Type LIKE 'Domestic Animal%' ESCAPE '\\'\"]")));
	}

	@Test
	void readsTheDateTheZipCodeAndARoundedPoint() {
		PositiveAnimal cat = this.client.animalsTestedPositive(null, SpecimenType.DOMESTIC_ANIMAL, null, null).get(0);

		assertThat(cat.id()).isEqualTo("155");
		assertThat(cat.disease()).isEqualTo(ZoonoticDisease.PLAGUE);
		assertThat(cat.county()).isEqualTo("Larimer");
		assertThat(cat.collectedDate()).isEqualTo(LocalDate.of(2017, 2, 13));
		assertThat(cat.zipCode()).isEqualTo("80524");
		// the source gives -105.0575 and 40.57656
		assertThat(cat.latitude()).isEqualTo(40.58);
		assertThat(cat.longitude()).isEqualTo(-105.06);
	}

	@Test
	void writesQuotesAndWildcardsOfTheSpeciesAsPlainText() {
		this.client.animalsTestedPositive(null, null, " O'Brien 50%_ ", "la plata");

		cdphe.verify(1, postRequestedFor(urlPathEqualTo(ANIMALS)).withRequestBody(matchingXPath(
				"//WhereClause[.=\"UPPER(Species) LIKE '%O''BRIEN 50\\%\\_%' ESCAPE '\\' AND UPPER(County) = 'LA PLATA'\"]")));
	}

	@Test
	void readsTheCountsOfTheStandaloneTableWithQueryData() {
		List<AnimalDiseaseCount> denver = this.client.animalDiseaseCounts(null, CaseGrouping.COUNTY, "Denver", null,
				null);

		assertThat(denver).hasSize(30);
		AnimalDiseaseCount rabies2025 = denver.stream()
			.filter((count) -> count.year() == 2025 && count.disease() == AnimalDisease.RABIES)
			.findFirst()
			.orElseThrow();
		assertThat(rabies2025.county()).isEqualTo("Denver");
		assertThat(rabies2025.total()).isEqualTo(5);
		assertThat(rabies2025.population()).isEqualTo(737680);
		assertThat(rabies2025.ratePer100kPeople()).isEqualTo(0.68);
		cdphe.verify(1,
				postRequestedFor(urlPathEqualTo(CASES)).withRequestBody(
						matchingXPath("//*[local-name()='QueryData'][MapName='Layers']/MapTableDescription[ID='1']"
								+ "[@*[local-name()='type']='e:StandaloneTableDescription']")));
	}

	@Test
	void namesTheMonthOfACountPerMonth() {
		List<AnimalDiseaseCount> months = this.client.animalDiseaseCounts(AnimalDisease.RABIES,
				CaseGrouping.ONSET_MONTH, null, 2025, 2025);

		assertThat(months).hasSize(12).allMatch((count) -> count.county() == null);
		assertThat(months).filteredOn((count) -> "August".equals(count.month()))
			.extracting(AnimalDiseaseCount::total)
			.containsExactly(23);
	}

	@Test
	void turnsASuppressedOrMissingCountIntoNull() {
		AnimalDiseaseCount suppressed = AnimalDiseaseCount
			.fromRecord(Map.of("diagnosis", "Plague, Animal", "onset_year", "2025", "metric", "county", "category",
					"Bent", "total", "**", "population", "NA", "rate_per_100k", "NA"));

		assertThat(suppressed.total()).isNull();
		assertThat(suppressed.suppressed()).isTrue();
		assertThat(suppressed.population()).isNull();
		assertThat(suppressed.ratePer100kPeople()).isNull();
	}

	@Test
	void reportsASoapFaultThatArrivesWithStatus200() {
		CdpheClient wrongMap = client("Unknown", Duration.ofSeconds(5));

		assertThatExceptionOfType(UpstreamException.class)
			.isThrownBy(() -> wrongMap.animalsTestedPositive(null, null, null, null))
			.withMessageContaining("Error processing server request")
			.satisfies((ex) -> assertThat(ex.getUpstream()).isEqualTo(CdpheClient.ANIMALS));
	}

	@Test
	void reportsATableThatAnswersTooSlowly() {
		cdphe.stubFor(post(urlPathEqualTo(CASES)).atPriority(1)
			.willReturn(aResponse().withFixedDelay(1500).withBodyFile("cdphe/cases-none.xml")));
		CdpheClient impatient = client("Layers", Duration.ofMillis(300));

		assertThatExceptionOfType(UpstreamException.class)
			.isThrownBy(() -> impatient.animalDiseaseCounts(null, CaseGrouping.STATE, null, null, null))
			.satisfies((ex) -> assertThat(ex.getUpstream()).isEqualTo(CdpheClient.CASES));
	}

}
