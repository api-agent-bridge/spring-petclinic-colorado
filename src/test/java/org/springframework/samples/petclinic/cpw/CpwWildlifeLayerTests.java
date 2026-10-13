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
package org.springframework.samples.petclinic.cpw;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests how {@link CpwWildlifeLayer} splits the name of a layer.
 */
class CpwWildlifeLayerTests {

	@Test
	void theLongestKindWins() {
		CpwWildlifeLayer layer = CpwWildlifeLayer.from(5, "Bighorn Severe Winter Range");

		assertThat(layer.species()).isEqualTo("Bighorn");
		assertThat(layer.kind()).isEqualTo("Severe Winter Range");
	}

	@Test
	void aSpeciesOfSeveralWordsStaysWhole() {
		CpwWildlifeLayer layer = CpwWildlifeLayer.from(1, "Prairie Rattlesnake And Western Rattlesnake Overall Range");

		assertThat(layer.species()).isEqualTo("Prairie Rattlesnake And Western Rattlesnake");
		assertThat(layer.kind()).isEqualTo("Overall Range");
	}

	@Test
	void doubleSpacesAreClosed() {
		CpwWildlifeLayer layer = CpwWildlifeLayer.from(2, "Ring-necked  Pheasant Concentration Area");

		assertThat(layer.species()).isEqualTo("Ring-necked Pheasant");
		assertThat(layer.layerName()).isEqualTo("Ring-necked  Pheasant Concentration Area");
	}

	@Test
	void aNameWithoutAKnownKindKeepsItsWholeName() {
		CpwWildlifeLayer layer = CpwWildlifeLayer.from(3, "Wolf Packs");

		assertThat(layer.species()).isEqualTo("Wolf Packs");
		assertThat(layer.kind()).isNull();
	}

}
