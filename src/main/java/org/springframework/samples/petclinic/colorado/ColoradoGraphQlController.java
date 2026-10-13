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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.samples.petclinic.upstream.InvalidArgumentException;
import org.springframework.samples.petclinic.upstream.RestUpstreams;
import org.springframework.samples.petclinic.upstream.ResultPages;
import org.springframework.samples.petclinic.upstream.UpstreamException;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Answers the <code>colorado*</code> queries of the GraphQL schema from
 * data.colorado.gov. A page of licenses or reports costs two calls, one for the page and
 * one that counts the matches. The filters become a SoQL <code>$where</code> clause, so
 * Socrata does the filtering and the paging.
 */
@Controller
class ColoradoGraphQlController {

	static final String PORTAL = "The Colorado open data portal";

	/**
	 * The name Socrata gives the result of <code>count(*)</code>.
	 */
	private static final String COUNT = "count";

	private final ColoradoDataApi portal;

	private final ColoradoDatasets datasets;

	/**
	 * The columns of each yearly shelter dataset after the facility name, by dataset id.
	 * A dataset's columns change only when the state publishes it again.
	 */
	private final Map<String, List<String>> shelterColumns = new ConcurrentHashMap<>();

	ColoradoGraphQlController(ColoradoDataApi portal, ColoradoDatasets datasets) {
		this.portal = portal;
		this.datasets = datasets;
	}

	@QueryMapping
	ColoradoVeterinaryLicense.Page coloradoVeterinaryLicenses(@Argument String lastName, @Argument String firstName,
			@Argument String city, @Argument ColoradoVeterinaryLicenseType licenseType, @Argument String status,
			@Argument int page, @Argument int size) {
		int pageNumber = ResultPages.pageNumber(page);
		int pageSize = ResultPages.pageSize(size);
		List<String> conditions = new ArrayList<>();
		conditions.add((licenseType != null) ? "licensetype = " + literal(licenseType.code())
				: "licensetype in (" + ColoradoVeterinaryLicenseType.codes()
					.stream()
					.map(ColoradoGraphQlController::literal)
					.collect(Collectors.joining(", ")) + ")");
		startsWith(conditions, "lastname", lastName);
		startsWith(conditions, "firstname", firstName);
		equalsIgnoringCase(conditions, "city", city);
		equalsIgnoringCase(conditions, "licensestatusdescription", status);
		String where = String.join(" AND ", conditions);
		String dataset = this.datasets.veterinaryLicenses();
		int total = count(dataset, where);
		long offset = (long) (pageNumber - 1) * pageSize;
		List<ColoradoVeterinaryLicense> licenses = List.of();
		if (offset < total) {
			MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
			query.add("$where", where);
			// the row id last, so that the pages split the same rows the same way
			query.add("$order", "lastname, firstname, licensenumber, :id");
			query.add("$limit", String.valueOf(pageSize));
			query.add("$offset", String.valueOf(offset));
			licenses = RestUpstreams.call(PORTAL, () -> this.portal.licenses(dataset, query))
				.stream()
				.map(ColoradoVeterinaryLicense::from)
				.filter(Objects::nonNull)
				// a recorded answer in mock mode can hold more than the page asked for
				.limit(pageSize)
				.toList();
		}
		return new ColoradoVeterinaryLicense.Page(licenses, pageNumber, totalPages(total, pageSize), total);
	}

	@QueryMapping
	ColoradoShelterReport.Page coloradoShelterReports(@Argument Integer year, @Argument String name, @Argument int page,
			@Argument int size) {
		int reportYear = year(year);
		String dataset = this.datasets.shelterReports().get(reportYear);
		int pageNumber = ResultPages.pageNumber(page);
		int pageSize = ResultPages.pageSize(size);
		List<String> conditions = new ArrayList<>();
		if (name != null && !name.isBlank()) {
			// the wildcards of LIKE are left out of the text a client sends
			String text = name.strip().toUpperCase(Locale.ROOT).replace("%", "").replace("_", "");
			conditions.add("upper(" + ColoradoShelterReport.FACILITY_NAME + ") like " + literal("%" + text + "%"));
		}
		String where = String.join(" AND ", conditions);
		int total = count(dataset, where);
		long offset = (long) (pageNumber - 1) * pageSize;
		List<ColoradoShelterReport> reports = List.of();
		if (offset < total) {
			List<String> columns = shelterColumns(dataset, reportYear);
			MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
			if (!where.isEmpty()) {
				query.add("$where", where);
			}
			query.add("$order", ColoradoShelterReport.FACILITY_NAME + ", :id");
			query.add("$limit", String.valueOf(pageSize));
			query.add("$offset", String.valueOf(offset));
			reports = RestUpstreams.call(PORTAL, () -> this.portal.rows(dataset, query))
				.stream()
				.map((row) -> ColoradoShelterReport.from(row, columns, reportYear))
				.limit(pageSize)
				.toList();
		}
		return new ColoradoShelterReport.Page(reports, pageNumber, totalPages(total, pageSize), total);
	}

	@QueryMapping
	ColoradoShelterReport coloradoShelterTotals(@Argument Integer year) {
		int reportYear = year(year);
		String dataset = this.datasets.shelterReports().get(reportYear);
		List<String> columns = shelterColumns(dataset, reportYear);
		MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
		query.add("$select",
				columns.stream().map((column) -> "sum(" + column + ") as " + column).collect(Collectors.joining(",")));
		List<Map<String, String>> sums = RestUpstreams.call(PORTAL, () -> this.portal.rows(dataset, query));
		if (sums.isEmpty()) {
			throw new UpstreamException(PORTAL, "The sums of the shelter reports of " + reportYear + " are empty");
		}
		return ColoradoShelterReport.from(sums.get(0), columns, reportYear);
	}

	/**
	 * Returns the year a client asked for, or the latest year when it left the year out.
	 */
	private int year(Integer year) {
		Map<Integer, String> years = this.datasets.shelterReports();
		if (year == null) {
			return years.keySet().stream().max(Comparator.naturalOrder()).orElseThrow();
		}
		if (!years.containsKey(year)) {
			throw new InvalidArgumentException("The shelter reports cover the years "
					+ years.keySet().stream().map(String::valueOf).collect(Collectors.joining(", ")) + ". " + year
					+ " is not one of them.");
		}
		return year;
	}

	/**
	 * Returns the names of the count columns of a yearly dataset, in the order of the
	 * table. Each dataset is described once and kept.
	 */
	private List<String> shelterColumns(String dataset, int year) {
		return this.shelterColumns.computeIfAbsent(dataset, (id) -> {
			List<String> columns = RestUpstreams.call(PORTAL, () -> this.portal.dataset(id))
				.columns()
				.stream()
				.sorted(Comparator.comparingInt(ColoradoDataApi.Column::position))
				.map(ColoradoDataApi.Column::fieldName)
				.toList();
			int expected = 1 + ColoradoShelterReport.GROUPS * ColoradoShelterAnimalCounts.COLUMNS;
			if (columns.size() != expected || !ColoradoShelterReport.FACILITY_NAME.equals(columns.get(0))) {
				throw new UpstreamException(PORTAL, "The shelter dataset of " + year + " has " + columns.size()
						+ " columns, and the reports expect " + expected + " starting with the facility name");
			}
			return columns.subList(1, columns.size());
		});
	}

	private int count(String dataset, String where) {
		MultiValueMap<String, String> query = new LinkedMultiValueMap<>();
		query.add("$select", "count(*)");
		if (!where.isEmpty()) {
			query.add("$where", where);
		}
		List<Map<String, String>> rows = RestUpstreams.call(PORTAL, () -> this.portal.rows(dataset, query));
		if (rows.isEmpty() || rows.get(0).get(COUNT) == null) {
			throw new UpstreamException(PORTAL, "The count of dataset " + dataset + " came back empty");
		}
		return Integer.parseInt(rows.get(0).get(COUNT));
	}

	private static int totalPages(int total, int pageSize) {
		return (total + pageSize - 1) / pageSize;
	}

	private static void startsWith(List<String> conditions, String column, String text) {
		if (text != null && !text.isBlank()) {
			conditions
				.add("starts_with(upper(" + column + "), " + literal(text.strip().toUpperCase(Locale.ROOT)) + ")");
		}
	}

	private static void equalsIgnoringCase(List<String> conditions, String column, String text) {
		if (text != null && !text.isBlank()) {
			conditions.add("upper(" + column + ") = " + literal(text.strip().toUpperCase(Locale.ROOT)));
		}
	}

	/**
	 * Writes a text as a SoQL literal. SoQL doubles a quote inside a literal, as SQL
	 * does.
	 */
	static String literal(String text) {
		return "'" + text.replace("'", "''") + "'";
	}

}
