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
 * What happened to one group of animals at a facility over a year, as the
 * <code>ColoradoShelterAnimalCounts</code> type of the GraphQL schema. Each group has the
 * same 17 columns in the same order: the animals at the start of the year, how they came
 * in, how they left, and the animals at the end of the year.
 *
 * @param inShelterAtStart animals in the shelter on 1 January
 * @param inFosterCareAtStart animals in foster care on 1 January
 * @param stray strays taken in
 * @param ownerRelinquished animals their owners gave up
 * @param transferredInFromColorado animals taken over from another Colorado organisation
 * @param transferredInFromOutOfState animals taken over from outside Colorado
 * @param otherIntake animals taken in another way
 * @param adopted animals adopted
 * @param returnedToOwner animals returned to their owners
 * @param transferredOutInColorado animals handed to another Colorado organisation
 * @param transferredOutOfState animals handed to an organisation outside Colorado
 * @param otherOutcome animals that left in another way
 * @param died animals that died
 * @param euthanized animals euthanized
 * @param missingOrStolen animals missing or stolen
 * @param inShelterAtEnd animals in the shelter on 31 December
 * @param inFosterCareAtEnd animals in foster care on 31 December
 */
record ColoradoShelterAnimalCounts(Integer inShelterAtStart, Integer inFosterCareAtStart, Integer stray,
		Integer ownerRelinquished, Integer transferredInFromColorado, Integer transferredInFromOutOfState,
		Integer otherIntake, Integer adopted, Integer returnedToOwner, Integer transferredOutInColorado,
		Integer transferredOutOfState, Integer otherOutcome, Integer died, Integer euthanized, Integer missingOrStolen,
		Integer inShelterAtEnd, Integer inFosterCareAtEnd) {

	/**
	 * The number of columns of each animal group.
	 */
	static final int COLUMNS = 17;

	/**
	 * Reads the 17 columns of one group. Socrata sends the counts as text, and a facility
	 * that left a count empty gets null.
	 */
	static ColoradoShelterAnimalCounts from(Map<String, String> row, List<String> columns) {
		Integer[] counts = columns.stream().map((column) -> count(row.get(column))).toArray(Integer[]::new);
		return new ColoradoShelterAnimalCounts(counts[0], counts[1], counts[2], counts[3], counts[4], counts[5],
				counts[6], counts[7], counts[8], counts[9], counts[10], counts[11], counts[12], counts[13], counts[14],
				counts[15], counts[16]);
	}

	private static Integer count(String text) {
		if (text == null || text.isBlank()) {
			return null;
		}
		try {
			// read as a decimal number, so that 3 and 3.0 both count 3
			return (int) Math.round(Double.parseDouble(text.strip()));
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}

}
