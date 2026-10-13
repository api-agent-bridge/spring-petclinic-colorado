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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.samples.petclinic.upstream.InvalidArgumentException;

/**
 * The filters of a query on Denver's 311 animal cases, written as the SQL condition of an
 * ArcGIS query. A null filter is left out.
 *
 * @param caseTypes the types of case, or null or empty for every animal case
 * @param zipCode the ZIP code of the incident
 * @param councilDistrict the city council district of the incident
 * @param createdFrom the first day a case was created on
 * @param createdTo the last day a case was created on
 */
record AnimalCaseFilter(List<DenverAnimalCaseType> caseTypes, String zipCode, Integer councilDistrict,
		LocalDate createdFrom, LocalDate createdTo) {

	private static final Pattern FIVE_DIGITS = Pattern.compile("\\d{5}");

	AnimalCaseFilter {
		if (zipCode != null && !zipCode.isBlank() && !FIVE_DIGITS.matcher(zipCode.strip()).matches()) {
			throw new InvalidArgumentException("zipCode must be five digits");
		}
		if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
			throw new InvalidArgumentException("createdFrom must not be after createdTo");
		}
	}

	/**
	 * Writes the condition. The 311 table stores the day a case was created at midnight
	 * UTC, so the last day is included by asking for the days before the next one.
	 */
	String where() {
		List<DenverAnimalCaseType> types = (this.caseTypes != null && !this.caseTypes.isEmpty()) ? this.caseTypes
				: List.of(DenverAnimalCaseType.values());
		List<String> conditions = new ArrayList<>();
		conditions
			.add("Case_Summary IN " + ArcGisQuery.texts(types.stream().map(DenverAnimalCaseType::summary).toList()));
		if (this.zipCode != null && !this.zipCode.isBlank()) {
			// some cases hold a ZIP+4 code, such as 80239-4053
			conditions.add("Incident_Zip_Code LIKE " + ArcGisQuery.text(this.zipCode.strip() + "%"));
		}
		if (this.councilDistrict != null) {
			conditions.add("Council_District = " + this.councilDistrict);
		}
		if (this.createdFrom != null) {
			conditions.add("Case_Created_Date >= " + ArcGisQuery.date(this.createdFrom));
		}
		if (this.createdTo != null) {
			conditions.add("Case_Created_Date < " + ArcGisQuery.date(this.createdTo.plusDays(1)));
		}
		return String.join(" AND ", conditions);
	}

}
