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

import java.time.Month;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.samples.petclinic.cdphe.AnimalDiseaseCount.AnimalDisease;
import org.springframework.samples.petclinic.cdphe.AnimalDiseaseCount.CaseGrouping;
import org.springframework.samples.petclinic.cdphe.PositiveAnimal.SpecimenType;
import org.springframework.samples.petclinic.cdphe.PositiveAnimal.ZoonoticDisease;
import org.springframework.samples.petclinic.upstream.InvalidArgumentException;
import org.springframework.samples.petclinic.upstream.ResultPages;
import org.springframework.stereotype.Controller;

/**
 * Answers the <code>cdphe*</code> queries of the GraphQL schema from the animal disease
 * data of the Colorado Department of Public Health and Environment. Every query costs one
 * SOAP call, which applies the filters on the server. This class sorts the answer and
 * cuts it into pages.
 */
@Controller
class CdpheGraphQlController {

	private static final Comparator<PositiveAnimal> BY_COLLECTED_DATE = Comparator
		.comparing(PositiveAnimal::collectedDate, Comparator.nullsLast(Comparator.naturalOrder()))
		.thenComparing((animal) -> Integer.parseInt(animal.id()));

	private final CdpheClient cdphe;

	CdpheGraphQlController(CdpheClient cdphe) {
		this.cdphe = cdphe;
	}

	@QueryMapping
	PositiveAnimalPage cdpheAnimalsTestedPositive(@Argument ZoonoticDisease disease,
			@Argument SpecimenType specimenType, @Argument String species, @Argument String county, @Argument int page,
			@Argument int size) {
		List<PositiveAnimal> animals = this.cdphe.animalsTestedPositive(disease, specimenType, species, county)
			.stream()
			.sorted(BY_COLLECTED_DATE)
			.toList();
		return ResultPages.page(animals, page, size, PositiveAnimalPage::new);
	}

	@QueryMapping
	AnimalDiseaseCountPage cdpheAnimalDiseaseCases(@Argument AnimalDisease disease, @Argument CaseGrouping groupedBy,
			@Argument String county, @Argument Integer fromYear, @Argument Integer toYear,
			@Argument boolean onlyWithCases, @Argument int page, @Argument int size) {
		boolean countyGiven = county != null && !county.isBlank();
		if (countyGiven && groupedBy != CaseGrouping.COUNTY) {
			throw new InvalidArgumentException("county applies only to counts grouped by COUNTY");
		}
		if (fromYear != null && toYear != null && fromYear > toYear) {
			throw new InvalidArgumentException("fromYear must not be after toYear");
		}
		List<AnimalDiseaseCount> counts = this.cdphe.animalDiseaseCounts(disease, groupedBy, county, fromYear, toYear)
			.stream()
			.filter((count) -> !onlyWithCases || count.total() == null || count.total() > 0)
			.sorted(order(groupedBy))
			.toList();
		return ResultPages.page(counts, page, size, AnimalDiseaseCountPage::new);
	}

	/**
	 * Sorts the counts by year, newest first, then by disease. Counts per county follow
	 * with the highest count first, and counts per month in the order of the calendar.
	 */
	private static Comparator<AnimalDiseaseCount> order(CaseGrouping groupedBy) {
		Comparator<AnimalDiseaseCount> order = Comparator.comparingInt(AnimalDiseaseCount::year)
			.reversed()
			.thenComparing(AnimalDiseaseCount::disease, Comparator.nullsLast(Comparator.naturalOrder()));
		if (groupedBy == CaseGrouping.ONSET_MONTH) {
			return order.thenComparingInt((count) -> monthNumber(count.month()));
		}
		return order.thenComparing(AnimalDiseaseCount::total, Comparator.nullsLast(Comparator.<Integer>reverseOrder()))
			.thenComparing(AnimalDiseaseCount::county, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
	}

	/**
	 * The number of a month the dataset names in English, for example 8 for August, and
	 * 13 for a name this class does not know, so it sorts last.
	 */
	private static int monthNumber(String name) {
		for (Month month : Month.values()) {
			if (month.getDisplayName(TextStyle.FULL, Locale.ENGLISH).equalsIgnoreCase(name)) {
				return month.getValue();
			}
		}
		return 13;
	}

	/**
	 * One page of animals that tested positive, as the
	 * <code>CdpheAnimalTestedPositivePage</code> type of the schema.
	 */
	record PositiveAnimalPage(List<PositiveAnimal> animals, int page, int totalPages, int totalAnimals) {
	}

	/**
	 * One page of yearly counts, as the <code>CdpheAnimalDiseaseCountPage</code> type of
	 * the schema.
	 */
	record AnimalDiseaseCountPage(List<AnimalDiseaseCount> counts, int page, int totalPages, int totalCounts) {
	}

}
