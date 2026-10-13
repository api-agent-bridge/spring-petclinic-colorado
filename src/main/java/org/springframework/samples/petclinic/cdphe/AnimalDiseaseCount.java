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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Map;

/**
 * The number of animals that tested positive for one disease in one year, in a county, in
 * the whole state or in one month, as the <code>CdpheAnimalDiseaseCount</code> type of
 * the GraphQL schema.
 * <p>
 * The dataset stores every number as text. A count the state suppressed is
 * <code>**</code>, and a value it does not have is <code>NA</code>. Both become null.
 *
 * @param disease the disease
 * @param year the year
 * @param groupedBy what the count covers
 * @param county the county, for a count grouped by county
 * @param month the month, for a count grouped by onset month
 * @param total the number of animals, or null when the dataset withholds or lacks it
 * @param suppressed whether the state suppressed the count
 * @param population the number of people living in the county
 * @param ratePer100kPeople the animals per 100,000 people, rounded to two decimals
 */
record AnimalDiseaseCount(AnimalDisease disease, int year, CaseGrouping groupedBy, String county, String month,
		Integer total, boolean suppressed, Integer population, Double ratePer100kPeople) {

	private static final String SUPPRESSED = "**";

	private static final String NOT_AVAILABLE = "NA";

	/**
	 * Reads one row of the state's table, keyed by its field names.
	 */
	static AnimalDiseaseCount fromRecord(Map<String, String> record) {
		CaseGrouping groupedBy = CaseGrouping.from(record.get("metric"));
		String category = (record.get("category") != null) ? record.get("category").strip() : null;
		String total = record.get("total");
		return new AnimalDiseaseCount(AnimalDisease.from(record.get("diagnosis")),
				Integer.parseInt(record.get("onset_year")), groupedBy,
				(groupedBy == CaseGrouping.COUNTY) ? category : null,
				(groupedBy == CaseGrouping.ONSET_MONTH) ? category : null, integer(total), SUPPRESSED.equals(total),
				integer(record.get("population")), rate(record.get("rate_per_100k")));
	}

	private static Integer integer(String value) {
		return isNumber(value) ? Integer.valueOf(value.strip()) : null;
	}

	private static Double rate(String value) {
		return isNumber(value) ? new BigDecimal(value.strip()).setScale(2, RoundingMode.HALF_UP).doubleValue() : null;
	}

	private static boolean isNumber(String value) {
		return value != null && !value.isBlank() && !SUPPRESSED.equals(value) && !NOT_AVAILABLE.equals(value);
	}

	/**
	 * The animal diseases in the state's reportable disease dataset.
	 */
	enum AnimalDisease {

		RABIES("Rabies, Animal"), PLAGUE("Plague, Animal"), TULAREMIA("Tularemia, Animal");

		private final String value;

		AnimalDisease(String value) {
			this.value = value;
		}

		/**
		 * The value of the <code>diagnosis</code> field in the state's table.
		 */
		String value() {
			return this.value;
		}

		static AnimalDisease from(String value) {
			return Arrays.stream(values()).filter((disease) -> disease.value.equals(value)).findFirst().orElse(null);
		}

	}

	/**
	 * What a count covers. The dataset calls it the metric.
	 */
	enum CaseGrouping {

		COUNTY("county"), STATE("state"), ONSET_MONTH("onset month");

		private final String value;

		CaseGrouping(String value) {
			this.value = value;
		}

		/**
		 * The value of the <code>metric</code> field in the state's table.
		 */
		String value() {
			return this.value;
		}

		static CaseGrouping from(String value) {
			return Arrays.stream(values()).filter((grouping) -> grouping.value.equals(value)).findFirst().orElse(null);
		}

	}

}
