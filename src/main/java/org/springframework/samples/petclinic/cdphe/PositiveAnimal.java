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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;

/**
 * An animal, or a pool of mosquitoes, that tested positive in Colorado in 2017, as the
 * <code>CdpheAnimalTestedPositive</code> type of the GraphQL schema.
 *
 * @param id the identifier of the record in the state's map
 * @param species the species, without the trailing spaces of the source
 * @param specimenType the group the animal belongs to, or null for a group this class
 * does not know
 * @param disease the disease, or null for a disease this class does not know
 * @param county the county the animal was found in
 * @param collectedDate the day the sample was collected, or null where the state left it
 * out
 * @param zipCode the ZIP code where the animal was found
 * @param latitude the latitude, rounded to two decimals
 * @param longitude the longitude, rounded to two decimals
 */
record PositiveAnimal(String id, String species, SpecimenType specimenType, ZoonoticDisease disease, String county,
		LocalDate collectedDate, String zipCode, Double latitude, Double longitude) {

	/**
	 * The point can be the home where a pet was found, so it is rounded to two decimals
	 * of a degree, about one kilometre.
	 */
	private static final int COORDINATE_DECIMALS = 2;

	/**
	 * Reads one record of the state's layer, keyed by its field names.
	 */
	static PositiveAnimal fromRecord(Map<String, String> record) {
		return new PositiveAnimal(record.get("OBJECTID"), strip(record.get("Species")),
				SpecimenType.from(record.get("Specimen_Type")), ZoonoticDisease.from(record.get("Disease")),
				strip(record.get("County")), date(record.get("Collected")), zipCode(record.get("Zipcode")),
				rounded(record.get("Latitude")), rounded(record.get("Longitude")));
	}

	private static String strip(String value) {
		return (value != null) ? value.strip() : null;
	}

	/**
	 * Reads a date of the form <code>2017-02-13T00:00:00</code>, which the server sends
	 * for midnight of the day the sample was collected.
	 */
	private static LocalDate date(String value) {
		return (value != null) ? LocalDate.parse(value.substring(0, 10)) : null;
	}

	/**
	 * The layer stores the ZIP code as a number, for example <code>80524</code>.
	 */
	private static String zipCode(String value) {
		return (value != null) ? String.valueOf(Math.round(Double.parseDouble(value))) : null;
	}

	private static Double rounded(String value) {
		return (value != null) ? new BigDecimal(value).setScale(COORDINATE_DECIMALS, RoundingMode.HALF_UP).doubleValue()
				: null;
	}

	/**
	 * The diseases on the state's map of animals that tested positive.
	 */
	enum ZoonoticDisease {

		RABIES("Rabies"), PLAGUE("Plague"), TULAREMIA("Tularemia"), WEST_NILE_VIRUS("West Nile Virus");

		private final String value;

		ZoonoticDisease(String value) {
			this.value = value;
		}

		/**
		 * The value of the <code>Disease</code> field in the state's layer.
		 */
		String value() {
			return this.value;
		}

		static ZoonoticDisease from(String value) {
			return Arrays.stream(values())
				.filter((disease) -> disease.value.equals(strip(value)))
				.findFirst()
				.orElse(null);
		}

	}

	/**
	 * The groups of animals on the state's map. The source writes some of them with a
	 * trailing space, such as <code>'Domestic Animal '</code>, and others without.
	 */
	enum SpecimenType {

		DOMESTIC_ANIMAL("Domestic Animal"), WILDLIFE("Wildlife"), BAT("Bat"), BIRD("Avian"), HORSE("Equine"),
		RODENT("Rodent"), MOSQUITO_POOL("Mosquito"), FLEA("Flea");

		private final String value;

		SpecimenType(String value) {
			this.value = value;
		}

		/**
		 * The value of the <code>Specimen_Type</code> field in the state's layer, without
		 * a trailing space.
		 */
		String value() {
			return this.value;
		}

		static SpecimenType from(String value) {
			return Arrays.stream(values()).filter((type) -> type.value.equals(strip(value))).findFirst().orElse(null);
		}

	}

}
