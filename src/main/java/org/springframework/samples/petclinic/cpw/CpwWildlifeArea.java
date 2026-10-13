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

/**
 * An area of a Colorado Parks and Wildlife layer that contains a point, as the
 * <code>CpwWildlifeArea</code> type of the GraphQL schema.
 *
 * @param species the species or group, for example <code>Black Bear</code>
 * @param kind the kind of area, for example <code>Human Conflict Area</code>
 * @param layerName the name of the layer
 * @param layerId the id of the layer in the service
 * @param activityCode the code of the area in the layer, for example <code>BBHCA</code>
 */
record CpwWildlifeArea(String species, String kind, String layerName, int layerId, String activityCode) {

	static CpwWildlifeArea of(CpwWildlifeLayer layer, String activityCode) {
		return new CpwWildlifeArea(layer.species(), layer.kind(), layer.layerName(), layer.layerId(), activityCode);
	}

}
