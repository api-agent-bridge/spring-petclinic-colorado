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
package org.springframework.samples.petclinic.openfda;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A report of an adverse event after an animal drug, as openFDA publishes it, as the
 * <code>OpenFdaAnimalAdverseEvent</code> type of the GraphQL schema. The Jackson
 * annotations map openFDA's names to the names of the schema. openFDA sends numbers and
 * yes-or-no values as text, and Jackson reads them into numbers and booleans.
 *
 * @param id the id of the report, for example USA-USFDACVM-2026-US-035636
 * @param receivedDate the day the FDA first received the report
 * @param onsetDate the day the adverse event began
 * @param serious whether the event was serious
 * @param typeOfInformation what the report is about, for example a safety issue or a lack
 * of expected effectiveness
 * @param primaryReporter who reported, for example a veterinarian or the animal's owner
 * @param animalsAffected the number of animals affected
 * @param animalsTreated the number of animals treated with the drug
 * @param animal the animal
 * @param drugs the drugs the animal received
 * @param reactions the adverse reactions
 * @param outcomes what happened to the animals afterwards
 * @param healthBeforeExposure the animal's health before the drug
 */
record OpenFdaAnimalAdverseEvent(@JsonProperty("unique_aer_id_number") String id,
		@JsonProperty("original_receive_date") @JsonFormat(pattern = DATE) LocalDate receivedDate,
		@JsonProperty("onset_date") @JsonFormat(pattern = DATE) LocalDate onsetDate,
		@JsonProperty("serious_ae") Boolean serious, @JsonProperty("type_of_information") String typeOfInformation,
		@JsonProperty("primary_reporter") String primaryReporter,
		@JsonProperty("number_of_animals_affected") Integer animalsAffected,
		@JsonProperty("number_of_animals_treated") Integer animalsTreated, Animal animal,
		@JsonProperty("drug") List<Drug> drugs, @JsonProperty("reaction") List<Reaction> reactions,
		@JsonProperty("outcome") List<Outcome> outcomes,
		@JsonProperty("health_assessment_prior_to_exposure") HealthAssessment healthBeforeExposure) {

	/**
	 * The pattern of openFDA's dates, written without separators, for example 20260626.
	 */
	static final String DATE = "yyyyMMdd";

	OpenFdaAnimalAdverseEvent {
		drugs = (drugs != null) ? drugs : List.of();
		reactions = (reactions != null) ? reactions : List.of();
		outcomes = (outcomes != null) ? outcomes : List.of();
	}

	/**
	 * The animal of a report.
	 *
	 * @param species the species, for example Dog or Cat
	 * @param gender the gender
	 * @param reproductiveStatus whether the animal was neutered
	 * @param age the age
	 * @param weight the weight
	 * @param breed the breed
	 */
	record Animal(String species, String gender, @JsonProperty("reproductive_status") String reproductiveStatus,
			Measure age, Measure weight, Breed breed) {

	}

	/**
	 * An age or a weight, which a report gives as one value or as a range.
	 *
	 * @param min the value, or the lower end of the range
	 * @param max the upper end of the range, or null for one value
	 * @param unit the unit, for example Year, Month or Kilogram
	 * @param qualifier whether the value was measured or estimated
	 */
	record Measure(Double min, Double max, String unit, String qualifier) {

	}

	/**
	 * The breed of the animal. openFDA sends one breed as a text and the breeds of a
	 * crossbreed as a list.
	 *
	 * @param crossbred whether the animal is a crossbreed
	 * @param components the breeds, for example Retriever - Labrador
	 */
	record Breed(@JsonProperty("is_crossbred") Boolean crossbred, @JsonProperty("breed_component") @JsonFormat(
			with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY) List<String> components) {

		Breed {
			components = (components != null) ? components : List.of();
		}

	}

	/**
	 * A drug the animal received.
	 *
	 * @param brandName the brand name, which openFDA masks as MSK in the reports about
	 * dogs
	 * @param activeIngredients the active ingredients
	 * @param atcVetCode the ATCvet code of the drug, for example QP54AB52
	 * @param dosageForm the form, for example Tablet, chewable
	 * @param route how it was given, for example Oral or Topical
	 * @param administeredBy who gave it, for example the animal's owner
	 * @param usedAccordingToLabel whether it was used as the label says
	 */
	record Drug(@JsonProperty("brand_name") String brandName,
			@JsonProperty("active_ingredients") List<ActiveIngredient> activeIngredients,
			@JsonProperty("atc_vet_code") String atcVetCode, @JsonProperty("dosage_form") String dosageForm,
			String route, @JsonProperty("administered_by") String administeredBy,
			@JsonProperty("used_according_to_label") Boolean usedAccordingToLabel) {

		Drug {
			activeIngredients = (activeIngredients != null) ? activeIngredients : List.of();
		}

	}

	/**
	 * An active ingredient of a drug.
	 *
	 * @param name the name, for example Pyrantel Pamoate;Sarolaner
	 */
	record ActiveIngredient(String name) {

	}

	/**
	 * An adverse reaction, in the Veterinary Dictionary for Drug Regulatory Activities
	 * (VeDDRA) or in an older vocabulary.
	 *
	 * @param name the term, for example Vomiting
	 * @param code the VeDDRA code of the term
	 * @param animalsAffected the number of animals with the reaction
	 */
	record Reaction(@JsonProperty("veddra_term_name") String name, @JsonProperty("veddra_term_code") String code,
			@JsonProperty("number_of_animals_affected") Integer animalsAffected) {

	}

	/**
	 * What happened to the animals.
	 *
	 * @param medicalStatus the outcome, for example Recovered/Normal, Ongoing or Died
	 * @param animalsAffected the number of animals with that outcome
	 */
	record Outcome(@JsonProperty("medical_status") String medicalStatus,
			@JsonProperty("number_of_animals_affected") Integer animalsAffected) {

	}

	/**
	 * The animal's health before it received the drug.
	 *
	 * @param condition the condition, for example Good or Fair
	 * @param assessedBy who assessed it, for example a veterinarian
	 */
	record HealthAssessment(String condition, @JsonProperty("assessed_by") String assessedBy) {

	}

	/**
	 * One page of reports, as the <code>OpenFdaAnimalAdverseEventPage</code> type.
	 */
	record Page(List<OpenFdaAnimalAdverseEvent> events, int page, int totalPages, int totalEvents) {

	}

}
