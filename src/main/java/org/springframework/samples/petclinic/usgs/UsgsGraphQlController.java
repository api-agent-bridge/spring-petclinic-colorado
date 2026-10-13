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
package org.springframework.samples.petclinic.usgs;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

import graphql.schema.DataFetchingFieldSelectionSet;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.samples.petclinic.upstream.InvalidArgumentException;
import org.springframework.samples.petclinic.upstream.RestUpstreams;
import org.springframework.samples.petclinic.upstream.ResultPages;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Answers the <code>usgs*</code> queries of the GraphQL schema from the USGS earthquake
 * catalogue. A page of earthquakes costs one call, and a second one that counts the
 * matches when the query selects the totals. The catalogue took 10 to 42 seconds to
 * answer a query it had not seen before, and a fraction of a second for a repeated one,
 * so the count is worth leaving out. Without a point or a box, the queries search the box
 * around Colorado.
 */
@Controller
class UsgsGraphQlController {

	static final String USGS = "The USGS earthquake catalogue";

	private final UsgsApi usgs;

	UsgsGraphQlController(UsgsApi usgs) {
		this.usgs = usgs;
	}

	@QueryMapping
	UsgsEarthquake.Page usgsEarthquakes(@Argument LocalDate startTime, @Argument LocalDate endTime,
			@Argument Double minMagnitude, @Argument Double latitude, @Argument Double longitude,
			@Argument Double maxRadiusKm, @Argument UsgsBox box, @Argument UsgsEarthquakeOrder orderBy,
			@Argument int page, @Argument int size, DataFetchingFieldSelectionSet selection) {
		int pageNumber = ResultPages.pageNumber(page);
		int pageSize = ResultPages.pageSize(size);
		MultiValueMap<String, String> filters = filters(startTime, endTime, minMagnitude, latitude, longitude,
				maxRadiusKm, box);
		boolean counted = selection.contains("totalEarthquakes") || selection.contains("totalPages");
		// without the count, the totals stay 0, and the query did not select them
		int total = counted ? call(() -> this.usgs.count(filters)).count() : 0;
		long offset = (long) (pageNumber - 1) * pageSize;
		List<UsgsEarthquake> earthquakes = List.of();
		if (!counted || offset < total) {
			MultiValueMap<String, String> query = new LinkedMultiValueMap<>(filters);
			query.add("orderby", ((orderBy != null) ? orderBy : UsgsEarthquakeOrder.TIME).parameter());
			query.add("limit", String.valueOf(pageSize));
			// the service counts its offset from 1
			query.add("offset", String.valueOf(offset + 1));
			UsgsApi.FeatureCollection result = call(() -> this.usgs.query(query));
			// a recorded answer in mock mode can hold more than the page asked for
			earthquakes = (result.features() != null)
					? result.features().stream().limit(pageSize).map(UsgsEarthquake::from).toList() : List.of();
		}
		return new UsgsEarthquake.Page(earthquakes, pageNumber, (total + pageSize - 1) / pageSize, total);
	}

	@QueryMapping
	int usgsEarthquakeCount(@Argument LocalDate startTime, @Argument LocalDate endTime, @Argument Double minMagnitude,
			@Argument Double latitude, @Argument Double longitude, @Argument Double maxRadiusKm,
			@Argument UsgsBox box) {
		MultiValueMap<String, String> filters = filters(startTime, endTime, minMagnitude, latitude, longitude,
				maxRadiusKm, box);
		return call(() -> this.usgs.count(filters)).count();
	}

	private static MultiValueMap<String, String> filters(LocalDate startTime, LocalDate endTime, Double minMagnitude,
			Double latitude, Double longitude, Double maxRadiusKm, UsgsBox box) {
		if ((latitude == null) != (longitude == null)) {
			throw new InvalidArgumentException("Give latitude and longitude together, or leave both out");
		}
		boolean point = latitude != null;
		if (point && box != null) {
			throw new InvalidArgumentException("Give a point or a box, and leave the other out");
		}
		if (!point && maxRadiusKm != null) {
			throw new InvalidArgumentException("maxRadiusKm needs a point: give latitude and longitude as well");
		}
		MultiValueMap<String, String> filters = new LinkedMultiValueMap<>();
		if (startTime != null) {
			filters.add("starttime", startTime.toString());
		}
		if (endTime != null) {
			// the end day is included, so the search runs until the start of the next day
			filters.add("endtime", endTime.plusDays(1).toString());
		}
		if (minMagnitude != null) {
			filters.add("minmagnitude", String.valueOf(minMagnitude));
		}
		if (point) {
			filters.add("latitude", String.valueOf(latitude));
			filters.add("longitude", String.valueOf(longitude));
			filters.add("maxradiuskm", String.valueOf((maxRadiusKm != null) ? maxRadiusKm : 100.0));
		}
		else {
			UsgsBox area = (box != null) ? box : UsgsBox.COLORADO;
			filters.add("minlatitude", String.valueOf(area.minLatitude()));
			filters.add("maxlatitude", String.valueOf(area.maxLatitude()));
			filters.add("minlongitude", String.valueOf(area.minLongitude()));
			filters.add("maxlongitude", String.valueOf(area.maxLongitude()));
		}
		return filters;
	}

	/**
	 * Calls the catalogue, and turns its <code>400 Bad Request</code> into an error that
	 * repeats the reason it gave, such as <code>Bad minmagnitude value</code>.
	 */
	private static <T> T call(Supplier<T> call) {
		return RestUpstreams.call(USGS, () -> {
			try {
				return call.get();
			}
			catch (HttpClientErrorException.BadRequest ex) {
				String reason = ex.getResponseBodyAsString()
					.lines()
					.map(String::strip)
					.filter((line) -> !line.isEmpty() && !line.startsWith("Error 400"))
					.findFirst()
					.orElse("no reason given");
				throw new InvalidArgumentException("The USGS earthquake catalogue rejected the arguments: " + reason);
			}
		});
	}

}
