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
package org.springframework.samples.petclinic.denver;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * The parameters of one query on a layer or table of Denver's ArcGIS Online services.
 * Every query asks for JSON. The texts that reach the <code>where</code> clause go
 * through {@link #text(String)}, which doubles their single quotes.
 */
final class ArcGisQuery {

	private static final int LATITUDE_AND_LONGITUDE = 4326;

	private final MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

	private ArcGisQuery(String where) {
		this.parameters.set("where", where);
	}

	/**
	 * Starts a query for the rows that match an SQL condition, for example
	 * <code>1=1</code> for every row.
	 */
	static ArcGisQuery where(String condition) {
		return new ArcGisQuery(condition);
	}

	ArcGisQuery outFields(String fields) {
		this.parameters.set("outFields", fields);
		return this;
	}

	ArcGisQuery orderBy(String fields) {
		this.parameters.set("orderByFields", fields);
		return this;
	}

	/**
	 * Asks for one page of rows.
	 * @param offset the number of rows to skip
	 * @param count the number of rows to return
	 */
	ArcGisQuery page(long offset, int count) {
		this.parameters.set("resultOffset", String.valueOf(offset));
		this.parameters.set("resultRecordCount", String.valueOf(count));
		return this;
	}

	/**
	 * Asks for the number of matching rows only.
	 */
	ArcGisQuery countOnly() {
		this.parameters.set("returnCountOnly", "true");
		return this;
	}

	/**
	 * Asks for the centre of each polygon in latitude and longitude, without the shape.
	 */
	ArcGisQuery centroids() {
		this.parameters.set("returnGeometry", "false");
		this.parameters.set("returnCentroid", "true");
		this.parameters.set("outSR", String.valueOf(LATITUDE_AND_LONGITUDE));
		return this;
	}

	/**
	 * Keeps the features within a distance of a point.
	 */
	ArcGisQuery within(GeoPoint point, int metres) {
		this.parameters.set("geometry", String.format(Locale.ROOT, "%.6f,%.6f", point.longitude(), point.latitude()));
		this.parameters.set("geometryType", "esriGeometryPoint");
		this.parameters.set("inSR", String.valueOf(LATITUDE_AND_LONGITUDE));
		this.parameters.set("spatialRel", "esriSpatialRelIntersects");
		this.parameters.set("distance", String.valueOf(metres));
		this.parameters.set("units", "esriSRUnit_Meter");
		return this;
	}

	/**
	 * Counts the matching rows by the values of one field, largest count first. Each
	 * answer row holds the value and its count under the given name.
	 */
	ArcGisQuery countBy(String field, String countName) {
		this.parameters.set("outStatistics", """
				[{"statisticType":"count","onStatisticField":"OBJECTID","outStatisticFieldName":"%s"}]"""
			.formatted(countName));
		this.parameters.set("groupByFieldsForStatistics", field);
		this.parameters.set("orderByFields", countName + " DESC");
		return this;
	}

	MultiValueMap<String, String> parameters() {
		MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>(this.parameters);
		parameters.set("f", "json");
		return parameters;
	}

	/**
	 * Writes a text as an SQL literal.
	 */
	static String text(String value) {
		return "'" + value.replace("'", "''") + "'";
	}

	/**
	 * Writes a list of texts for an SQL <code>IN</code>.
	 */
	static String texts(Collection<String> values) {
		return values.stream().map(ArcGisQuery::text).collect(Collectors.joining(",", "(", ")"));
	}

	/**
	 * Writes a day as an SQL date literal.
	 */
	static String date(LocalDate day) {
		return "DATE '" + day + "'";
	}

}
