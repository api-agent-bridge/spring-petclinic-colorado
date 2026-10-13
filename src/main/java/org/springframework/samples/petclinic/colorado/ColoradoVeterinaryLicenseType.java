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

import java.util.Arrays;
import java.util.List;

/**
 * The veterinary license types of the Colorado license register, as the
 * <code>ColoradoVeterinaryLicenseType</code> enum of the GraphQL schema. The register
 * names each type with a short code.
 */
enum ColoradoVeterinaryLicenseType {

	VETERINARIAN("VET"), VETERINARY_TECHNICIAN("VT"), ACADEMIC_VETERINARIAN("AV"),
	PROVISIONAL_VETERINARY_TECHNICIAN("PVT");

	private final String code;

	ColoradoVeterinaryLicenseType(String code) {
		this.code = code;
	}

	/**
	 * The code of the type in the register, for example <code>VET</code>.
	 */
	String code() {
		return this.code;
	}

	/**
	 * The codes of every veterinary type.
	 */
	static List<String> codes() {
		return Arrays.stream(values()).map(ColoradoVeterinaryLicenseType::code).toList();
	}

	/**
	 * Returns the type with this code, or null for a code of another profession.
	 */
	static ColoradoVeterinaryLicenseType fromCode(String code) {
		return Arrays.stream(values()).filter((type) -> type.code.equals(code)).findFirst().orElse(null);
	}

}
