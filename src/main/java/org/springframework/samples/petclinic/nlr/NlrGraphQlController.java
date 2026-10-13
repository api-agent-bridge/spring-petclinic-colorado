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

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.samples.petclinic.upstream.InvalidArgumentException;
import org.springframework.samples.petclinic.upstream.RestUpstreams;
import org.springframework.samples.petclinic.upstream.ResultPages;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Answers the <code>nlr*</code> queries of the GraphQL schema from the NLR Alternative
 * Fuel Stations API. Each query costs one call.
 */
@Controller
class NlrGraphQlController {

	static final String NLR = "The NLR Alternative Fuel Stations API";

	private final NlrApi nlr;

	NlrGraphQlController(NlrApi nlr) {
		this.nlr = nlr;
	}

	@QueryMapping
	NlrFuelStation.Page nlrFuelStationsNear(@Argument double latitude, @Argument double longitude,
			@Argument double radiusInMiles, @Argument NlrFuelType fuelType, @Argument int page, @Argument int size) {
		if (radiusInMiles <= 0) {
			throw new InvalidArgumentException("radiusInMiles must be above 0");
		}
		int pageNumber = ResultPages.pageNumber(page);
		int pageSize = ResultPages.pageSize(size);
		MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
		query.add("latitude", String.valueOf(latitude));
		query.add("longitude", String.valueOf(longitude));
		query.add("radius", String.valueOf(radiusInMiles));
		if (fuelType != null) {
			query.add("fuel_type", fuelType.code());
		}
		query.add("limit", String.valueOf(pageSize));
		query.add("offset", String.valueOf((long) (pageNumber - 1) * pageSize));
		NlrApi.Nearest nearest = RestUpstreams.call(NLR, () -> this.nlr.nearest(query));
		// a recorded answer in mock mode can hold more than the page asked for
		List<NlrFuelStation> stations = (nearest.fuelStations() != null)
				? nearest.fuelStations().stream().limit(pageSize).toList() : List.of();
		int total = nearest.totalResults();
		return new NlrFuelStation.Page(stations, pageNumber, (total + pageSize - 1) / pageSize, total);
	}

}
