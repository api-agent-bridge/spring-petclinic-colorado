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
package org.springframework.samples.petclinic.nlr;

/**
 * The fuels of the NLR station search, as the <code>NlrFuelType</code> enum of the
 * GraphQL schema, with the code that NLR uses for each.
 */
enum NlrFuelType {

	ELECTRIC("ELEC"), ETHANOL_E85("E85"), COMPRESSED_NATURAL_GAS("CNG"), LIQUEFIED_NATURAL_GAS("LNG"), PROPANE("LPG"),
	HYDROGEN("HY"), BIODIESEL("BD"), RENEWABLE_DIESEL("RD");

	private final String code;

	NlrFuelType(String code) {
		this.code = code;
	}

	/**
	 * The code of the fuel, for example <code>ELEC</code>.
	 */
	String code() {
		return this.code;
	}

}
