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

import java.util.Arrays;
import java.util.Optional;

/**
 * The kinds of animal case in Denver's 311 service requests, as the
 * <code>DenverAnimalCaseType</code> enum of the GraphQL schema. Each value stands for one
 * case summary that 311 uses, which the table stores as free text.
 */
enum DenverAnimalCaseType {

	ADOPTION_OR_HOLD_INQUIRY("Animal Adoption/Hold Inquiry"), LOST_PET("Lost my Pet"), BARKING_DOG("Barking Dog"),
	PET_SURRENDER("Pet Surrender"), LOOSE_ANIMAL("Loose Animal"), LOOSE_DOG_WITH_OWNER("Loose Dog (With owner)"),
	HOLDING_STRAY_ANIMAL("Holding Stray Animal"), ANIMAL_PROTECTION_CALL("Denver Animal Protection 720-913-2080"),
	INJURED_WILDLIFE("Injured Wildlife"), WILDLIFE("Wildlife"), PET_REDEMPTION("Pet Redemption"),
	ANIMAL_EXCREMENT("Animal Excrement"), PET_LICENSE_OR_MICROCHIP("Pet License/Micro Chip"),
	DANGEROUS_OR_AGGRESSIVE_ANIMAL("Dangerous/Aggressive (Not a Bite)"),
	PET_FOOD_MANUFACTURING("Pet Food Manufacturing"), INJURED_DOMESTIC_ANIMAL("Injured Domestic Animal"),
	DEAD_ANIMAL("Dead Animal"), BITE_OR_ATTACK("Bite/Attack (Broken the skin)"),
	ANIMAL_BITE_NON_HUMAN("Animal Bite (non human)"), DOG_PARK("Dog Parks"), TOO_MANY_ANIMALS("Too many animals"),
	BREEDING_OR_SERVICE_ANIMALS("Breeding Animals / Service Animals"), ANIMAL_IN_VEHICLE("Animal in Vehicle"),
	NON_SERVICE_ANIMAL_COMPLAINT("Non-Service Animal Complaint"), ANIMAL_COMPLAINT("Animal Complaint");

	private final String summary;

	DenverAnimalCaseType(String summary) {
		this.summary = summary;
	}

	/**
	 * The case summary as the 311 table stores it.
	 */
	String summary() {
		return this.summary;
	}

	/**
	 * Finds the type of a case summary, or returns an empty optional for a summary of
	 * another kind of case.
	 */
	static Optional<DenverAnimalCaseType> of(String summary) {
		return Arrays.stream(values()).filter((type) -> type.summary.equals(summary)).findFirst();
	}

}
