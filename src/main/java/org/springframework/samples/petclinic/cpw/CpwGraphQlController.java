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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.samples.petclinic.upstream.InvalidArgumentException;
import org.springframework.samples.petclinic.upstream.RestUpstreams;
import org.springframework.samples.petclinic.upstream.SearchText;
import org.springframework.samples.petclinic.upstream.UpstreamException;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Answers the <code>cpw*</code> queries of the GraphQL schema from the species data of
 * Colorado Parks and Wildlife. The layer list is read once and kept. A point query costs
 * one call, which asks all 313 layers whether they contain the point.
 */
@Controller
class CpwGraphQlController {

	static final String CPW = "The Colorado Parks and Wildlife species data";

	private static final Log logger = LogFactory.getLog(CpwGraphQlController.class);

	/**
	 * The only field the point query asks each layer for. The other fields are dates and
	 * shape measures.
	 */
	private static final String ACTIVITY_CODE = "ACTIVITYCO";

	private static final Comparator<CpwWildlifeLayer> BY_SPECIES = Comparator
		.comparing(CpwWildlifeLayer::species, String.CASE_INSENSITIVE_ORDER)
		.thenComparing(CpwWildlifeLayer::layerName, String.CASE_INSENSITIVE_ORDER);

	private final CpwApi cpw;

	private volatile List<CpwWildlifeLayer> layers;

	CpwGraphQlController(CpwApi cpw) {
		this.cpw = cpw;
	}

	@QueryMapping
	List<CpwWildlifeArea> cpwWildlifeAt(@Argument double latitude, @Argument double longitude,
			@Argument String species) {
		if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
			throw new InvalidArgumentException(
					"latitude must be between -90 and 90, and longitude between -180 and 180");
		}
		SearchText text = SearchText.of(species);
		Map<Integer, CpwWildlifeLayer> byId = layers().stream()
			.filter((layer) -> text.matches(layer.species(), layer.layerName()))
			.collect(Collectors.toMap(CpwWildlifeLayer::layerId, Function.identity()));
		if (byId.isEmpty()) {
			return List.of();
		}
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("layerDefs",
				byId.keySet()
					.stream()
					.sorted()
					.map((id) -> "{\"layerId\":%d,\"where\":\"1=1\",\"outFields\":\"%s\"}".formatted(id, ACTIVITY_CODE))
					.collect(Collectors.joining(",", "[", "]")));
		// ArcGIS takes a point as longitude first
		form.add("geometry", longitude + "," + latitude);
		form.add("geometryType", "esriGeometryPoint");
		form.add("inSR", "4326");
		form.add("spatialRel", "esriSpatialRelIntersects");
		form.add("returnGeometry", "false");
		form.add("f", "json");
		long started = System.nanoTime();
		CpwApi.QueryResult result = RestUpstreams.call(CPW, () -> this.cpw.query(form));
		if (result.error() != null || result.layers() == null) {
			throw new UpstreamException(CPW, (result.error() != null) ? result.error().message()
					: "The answer of the point query does not list any layers");
		}
		List<CpwWildlifeArea> areas = new ArrayList<>();
		for (CpwApi.LayerFeatures layer : result.layers()) {
			CpwWildlifeLayer described = byId.get(layer.id());
			if (described == null || layer.features() == null) {
				continue;
			}
			// a layer can hold several areas at the point, which share one meaning
			layer.features()
				.stream()
				.map((feature) -> (feature.attributes() != null) ? feature.attributes().get(ACTIVITY_CODE) : null)
				.map((code) -> (code != null) ? code.toString() : null)
				.distinct()
				.forEach((code) -> areas.add(CpwWildlifeArea.of(described, code)));
		}
		logger.info("CPW point query at %s,%s asked %d layers, and %d of them contain the point (%d ms)".formatted(
				latitude, longitude, byId.size(), areas.stream().map(CpwWildlifeArea::layerId).distinct().count(),
				Duration.ofNanos(System.nanoTime() - started).toMillis()));
		return areas.stream()
			.sorted(Comparator.comparing(CpwWildlifeArea::species, String.CASE_INSENSITIVE_ORDER)
				.thenComparing(CpwWildlifeArea::layerName, String.CASE_INSENSITIVE_ORDER))
			.toList();
	}

	@QueryMapping
	List<CpwWildlifeLayer> cpwWildlifeLayers(@Argument String species) {
		SearchText text = SearchText.of(species);
		return layers().stream().filter((layer) -> text.matches(layer.species(), layer.layerName())).toList();
	}

	/**
	 * Returns every layer of the service, sorted by species. The first call reads them
	 * from the service, and later calls reuse them.
	 */
	private List<CpwWildlifeLayer> layers() {
		List<CpwWildlifeLayer> known = this.layers;
		if (known == null) {
			CpwApi.Service service = RestUpstreams.call(CPW, this.cpw::service);
			if (service.error() != null || service.layers() == null) {
				throw new UpstreamException(CPW, (service.error() != null) ? service.error().message()
						: "The description of the service does not list any layers");
			}
			known = service.layers()
				.stream()
				.map((layer) -> CpwWildlifeLayer.from(layer.id(), layer.name()))
				.sorted(BY_SPECIES)
				.toList();
			this.layers = known;
		}
		return known;
	}

}
