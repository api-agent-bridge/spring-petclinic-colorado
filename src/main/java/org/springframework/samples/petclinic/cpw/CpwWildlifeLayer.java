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

import java.util.List;

/**
 * A layer of the Colorado Parks and Wildlife species data, as the
 * <code>CpwWildlifeLayer</code> type of the GraphQL schema. The service gives each layer
 * a name only, such as <code>Black Bear Human Conflict Area</code>, and
 * {@link #from(int, String)} splits it into the species and the kind of area.
 *
 * @param layerId the id of the layer in the service
 * @param layerName the name of the layer
 * @param species the species or group, for example <code>Black Bear</code>
 * @param kind the kind of area, for example <code>Human Conflict Area</code>
 */
record CpwWildlifeLayer(int layerId, String layerName, String species, String kind) {

	/**
	 * The kinds of area that the layer names end with, longest first, so that
	 * <code>Severe Winter Range</code> wins over <code>Winter Range</code>.
	 */
	static final List<String> KINDS = List.of("Priority and General Habitat", "Colony Potential Occurrence",
			"Brood Concentration Area", "Estimated Occupied Range", "Resident Population Area",
			"Summer Concentration Area", "Winter Concentration Area", "Historic Overall Range", "Human Conflict Area",
			"CHAT Priority Areas", "Migration Corridors", "Severe Winter Range", "Summer Concentration",
			"Winter Concentration", "Concentration Area", "Fall Concentration", "Historic Nest Area",
			"Migration Patterns", "Highway Crossings", "Potential Habitat", "Potential Nesting", "Highway Crossing",
			"Limited Use Area", "Occupied Habitat", "Peripheral Range", "Priority Habitat", "Historic Habitat",
			"HUC 12 Presence", "Breeding Range", "Communal Roosts", "Migration Range", "Perennial Water",
			"Production Area", "Historic Range", "Overall Range", "Release Sites", "Foraging Area", "Summer Forage",
			"Winter Forage", "Summer Range", "Winter Range", "Nesting Area", "Water Source", "Pinch Points",
			"Mineral Lick", "Molting Area", "Roost Sites", "Brood Area", "Nest Sites", "Linkages", "Habitat");

	/**
	 * Splits the name of a layer. A name without a known kind keeps its whole name as the
	 * species, and its kind is null.
	 */
	static CpwWildlifeLayer from(int id, String name) {
		String text = name.strip().replaceAll("\\s+", " ");
		for (String kind : KINDS) {
			if (text.endsWith(" " + kind)) {
				return new CpwWildlifeLayer(id, name, text.substring(0, text.length() - kind.length() - 1), kind);
			}
		}
		return new CpwWildlifeLayer(id, name, text, null);
	}

}
