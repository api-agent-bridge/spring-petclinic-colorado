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
package org.springframework.samples.petclinic.denver;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.samples.petclinic.upstream.InvalidArgumentException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests the {@link AnimalCaseFilter}, which writes the SQL condition of a query on
 * Denver's 311 animal cases.
 */
class AnimalCaseFilterTests {

	@Test
	void coversEveryAnimalCaseWithoutFilters() {
		String where = new AnimalCaseFilter(null, null, null, null, null).where();

		assertThat(where).startsWith("Case_Summary IN ('Animal Adoption/Hold Inquiry','Lost my Pet','Barking Dog',")
			.contains("'Denver Animal Protection 720-913-2080'")
			.endsWith("'Animal Complaint')");
	}

	@Test
	void includesTheLastDayByAskingForTheDaysBeforeTheNext() {
		String where = new AnimalCaseFilter(List.of(DenverAnimalCaseType.BARKING_DOG), "80204", 3,
				LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))
			.where();

		assertThat(where).isEqualTo("Case_Summary IN ('Barking Dog') AND Incident_Zip_Code LIKE '80204%'"
				+ " AND Council_District = 3 AND Case_Created_Date >= DATE '2026-09-01'"
				+ " AND Case_Created_Date < DATE '2026-10-01'");
	}

	@Test
	void rejectsAZipCodeThatIsNotFiveDigits() {
		assertThatExceptionOfType(InvalidArgumentException.class)
			.isThrownBy(() -> new AnimalCaseFilter(null, "80204' OR '1'='1", null, null, null))
			.withMessage("zipCode must be five digits");
	}

	@Test
	void rejectsAFirstDayAfterTheLastDay() {
		assertThatExceptionOfType(InvalidArgumentException.class)
			.isThrownBy(
					() -> new AnimalCaseFilter(null, null, null, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 9, 1)))
			.withMessage("createdFrom must not be after createdTo");
	}

	@Test
	void doublesTheSingleQuotesOfATextLiteral() {
		assertThat(ArcGisQuery.text("O'Brien")).isEqualTo("'O''Brien'");
	}

}
