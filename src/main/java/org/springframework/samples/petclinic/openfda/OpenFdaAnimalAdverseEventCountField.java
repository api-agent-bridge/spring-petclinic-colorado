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

/**
 * The fields of a report that <code>openFdaAnimalAdverseEventCounts</code> can count, as
 * the <code>OpenFdaAnimalAdverseEventCountField</code> enum of the GraphQL schema. Some
 * fields count their whole text with the <code>.exact</code> suffix, and the others take
 * the field as it is, which is what openFDA accepts for each.
 */
enum OpenFdaAnimalAdverseEventCountField {

	REACTION("reaction.veddra_term_name.exact"), BREED("animal.breed.breed_component.exact"),
	ACTIVE_INGREDIENT("drug.active_ingredients.name.exact"), SPECIES("animal.species"), ROUTE("drug.route"),
	OUTCOME("outcome.medical_status.exact"), GENDER("animal.gender"), REPORTER("primary_reporter.exact");

	private final String field;

	OpenFdaAnimalAdverseEventCountField(String field) {
		this.field = field;
	}

	/**
	 * The field as the <code>count</code> parameter names it.
	 */
	String field() {
		return this.field;
	}

}
