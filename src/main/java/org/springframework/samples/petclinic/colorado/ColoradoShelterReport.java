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
package org.springframework.samples.petclinic.colorado;

import java.util.List;
import java.util.Map;

/**
 * The yearly report of one Colorado animal shelter or rescue, or the totals of every
 * facility, as the <code>ColoradoShelterReport</code> type of the GraphQL schema. The Pet
 * Animal Care Facilities Act (PACFA) program of the Colorado Department of Agriculture
 * publishes one dataset a year, with one row for each facility.
 *
 * @param facilityName the name of the facility, or null for the totals
 * @param year the calendar year the report covers
 * @param adultDogs the counts for adult dogs
 * @param juvenileDogs the counts for puppies
 * @param adultCats the counts for adult cats
 * @param juvenileCats the counts for kittens
 * @param birds the counts for birds
 * @param smallMammals the counts for small mammals
 * @param reptilesAndAmphibians the counts for reptiles and amphibians
 * @param rabbits the counts for rabbits
 * @param otherAnimals the counts for other animals, such as fish and livestock
 */
record ColoradoShelterReport(String facilityName, int year, ColoradoShelterAnimalCounts adultDogs,
		ColoradoShelterAnimalCounts juvenileDogs, ColoradoShelterAnimalCounts adultCats,
		ColoradoShelterAnimalCounts juvenileCats, ColoradoShelterAnimalCounts birds,
		ColoradoShelterAnimalCounts smallMammals, ColoradoShelterAnimalCounts reptilesAndAmphibians,
		ColoradoShelterAnimalCounts rabbits, ColoradoShelterAnimalCounts otherAnimals) {

	/**
	 * The name of the first column of every yearly dataset.
	 */
	static final String FACILITY_NAME = "facility_name";

	/**
	 * The number of animal groups in a row, each with
	 * {@value ColoradoShelterAnimalCounts#COLUMNS} columns.
	 */
	static final int GROUPS = 9;

	/**
	 * Reads one row of a yearly dataset. The datasets of different years name their
	 * columns differently, for example <code>adult_dogs_stray</code> in 2025 and
	 * <code>_2024_adult_dogs_stray</code> in 2024, while the order of the columns stays
	 * the same. So the values are read by position.
	 * @param row the values of the row by column name
	 * @param columns the names of the columns after the facility name, in the order of
	 * the table
	 * @param year the year of the dataset
	 */
	static ColoradoShelterReport from(Map<String, String> row, List<String> columns, int year) {
		ColoradoShelterAnimalCounts[] groups = new ColoradoShelterAnimalCounts[GROUPS];
		for (int group = 0; group < GROUPS; group++) {
			int first = group * ColoradoShelterAnimalCounts.COLUMNS;
			groups[group] = ColoradoShelterAnimalCounts.from(row,
					columns.subList(first, first + ColoradoShelterAnimalCounts.COLUMNS));
		}
		return new ColoradoShelterReport(row.get(FACILITY_NAME), year, groups[0], groups[1], groups[2], groups[3],
				groups[4], groups[5], groups[6], groups[7], groups[8]);
	}

	/**
	 * One page of reports, as the <code>ColoradoShelterReportPage</code> type.
	 */
	record Page(List<ColoradoShelterReport> reports, int page, int totalPages, int totalReports) {

	}

}
