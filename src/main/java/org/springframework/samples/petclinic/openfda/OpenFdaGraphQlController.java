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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

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
 * Answers the <code>openFda*</code> queries of the GraphQL schema from openFDA's animal
 * and veterinary adverse events. Each query costs one call. The arguments become one
 * <code>search</code> expression, whose parts must all match.
 */
@Controller
class OpenFdaGraphQlController {

	static final String OPENFDA = "openFDA";

	/**
	 * openFDA skips at most this many reports, so a page cannot start later.
	 */
	static final int MAX_SKIP = 25_000;

	/**
	 * The most values a count may return here. openFDA allows 1,000.
	 */
	static final int MAX_COUNTS = 100;

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern(OpenFdaAnimalAdverseEvent.DATE);

	private final OpenFdaApi openFda;

	OpenFdaGraphQlController(OpenFdaApi openFda) {
		this.openFda = openFda;
	}

	@QueryMapping
	OpenFdaAnimalAdverseEvent.Page openFdaAnimalAdverseEvents(@Argument String species, @Argument String breed,
			@Argument String drug, @Argument String reaction, @Argument LocalDate from, @Argument LocalDate to,
			@Argument int page, @Argument int size) {
		int pageNumber = ResultPages.pageNumber(page);
		int pageSize = ResultPages.pageSize(size);
		long skip = (long) (pageNumber - 1) * pageSize;
		if (skip > MAX_SKIP) {
			throw new InvalidArgumentException("openFDA pages through the first " + MAX_SKIP
					+ " reports only. Narrow the search, or ask for an earlier page.");
		}
		MultiValueMap<String, String> query = search(species, breed, drug, reaction, from, to);
		query.add("sort", "original_receive_date:desc");
		query.add("limit", String.valueOf(pageSize));
		query.add("skip", String.valueOf(skip));
		OpenFdaApi.EventSearch result = orNull(() -> this.openFda.events(query));
		if (result == null || result.results() == null) {
			return new OpenFdaAnimalAdverseEvent.Page(List.of(), pageNumber, 0, 0);
		}
		int total = (result.meta() != null && result.meta().results() != null) ? result.meta().results().total() : 0;
		// a recorded answer in mock mode can hold more than the page asked for
		return new OpenFdaAnimalAdverseEvent.Page(result.results().stream().limit(pageSize).toList(), pageNumber,
				(total + pageSize - 1) / pageSize, total);
	}

	@QueryMapping
	List<OpenFdaCount> openFdaAnimalAdverseEventCounts(@Argument OpenFdaAnimalAdverseEventCountField countBy,
			@Argument String species, @Argument String breed, @Argument String drug, @Argument String reaction,
			@Argument LocalDate from, @Argument LocalDate to, @Argument int first) {
		if (first < 1 || first > MAX_COUNTS) {
			throw new InvalidArgumentException("first must be between 1 and " + MAX_COUNTS);
		}
		MultiValueMap<String, String> query = search(species, breed, drug, reaction, from, to);
		query.add("count", countBy.field());
		query.add("limit", String.valueOf(first));
		OpenFdaApi.CountResult result = orNull(() -> this.openFda.counts(query));
		return (result != null && result.results() != null) ? result.results().stream().limit(first).toList()
				: List.of();
	}

	/**
	 * Builds the <code>search</code> parameter. Each text becomes a phrase in double
	 * quotes, which openFDA matches against the words of the field, so
	 * <code>Labrador</code> matches <code>Retriever - Labrador</code>.
	 */
	private static MultiValueMap<String, String> search(String species, String breed, String drug, String reaction,
			LocalDate from, LocalDate to) {
		List<String> terms = new ArrayList<>();
		phrase(terms, "animal.species", species);
		phrase(terms, "animal.breed.breed_component", breed);
		phrase(terms, "drug.active_ingredients.name", drug);
		phrase(terms, "reaction.veddra_term_name", reaction);
		if (from != null || to != null) {
			String start = (from != null) ? from.format(DATE) : "19000101";
			String end = (to != null) ? to.format(DATE) : "29991231";
			terms.add("original_receive_date:[" + start + " TO " + end + "]");
		}
		MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
		if (!terms.isEmpty()) {
			query.add("search", String.join(" AND ", terms));
		}
		return query;
	}

	private static void phrase(List<String> terms, String field, String text) {
		if (text != null && !text.isBlank()) {
			// a quote or a backslash would end the phrase early
			terms.add(field + ":\"" + text.strip().replace("\"", "").replace("\\", "") + "\"");
		}
	}

	/**
	 * Calls openFDA, and returns null for its <code>404 Not Found</code>, which means
	 * that no report matches. openFDA answers a count of a field the matching reports
	 * lack the same way.
	 */
	private static <T> T orNull(Supplier<T> call) {
		return RestUpstreams.call(OPENFDA, () -> {
			try {
				return call.get();
			}
			catch (HttpClientErrorException.NotFound ex) {
				return null;
			}
		});
	}

}
