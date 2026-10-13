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

import java.time.Instant;
import java.util.List;

/**
 * An earthquake from the USGS catalogue, as the <code>UsgsEarthquake</code> type of the
 * GraphQL schema.
 *
 * @param id the id of the event, for example <code>us7000thsk</code>
 * @param title the magnitude and the place, for example <code>M 1.8 - 28 km WSW of Yampa,
 * Colorado</code>
 * @param magnitude the magnitude
 * @param magnitudeType how the magnitude was measured, for example <code>ml</code>
 * @param place the place, described by its distance from a town
 * @param time when the earthquake happened, in UTC as ISO-8601
 * @param updated when the catalogue last updated the event, in UTC as ISO-8601
 * @param latitude the latitude of the epicentre
 * @param longitude the longitude of the epicentre
 * @param depthInKilometres the depth of the focus
 * @param type the kind of event, for example <code>earthquake</code> or
 * <code>mining explosion</code>
 * @param status <code>automatic</code> or <code>reviewed</code>
 * @param significance how significant the event is, from 0 to about 1,000
 * @param feltReports the number of people who reported to the USGS that they felt it
 * @param alert the alert level of the PAGER system, for example <code>green</code>
 * @param url the page of the event on the USGS website
 */
record UsgsEarthquake(String id, String title, Double magnitude, String magnitudeType, String place, String time,
		String updated, Double latitude, Double longitude, Double depthInKilometres, String type, String status,
		Integer significance, Integer feltReports, String alert, String url) {

	static UsgsEarthquake from(UsgsApi.Feature feature) {
		UsgsApi.Properties properties = (feature.properties() != null) ? feature.properties()
				: new UsgsApi.Properties(null, null, null, null, null, null, null, null, null, null, null, null);
		List<Double> coordinates = (feature.geometry() != null && feature.geometry().coordinates() != null)
				? feature.geometry().coordinates() : List.of();
		return new UsgsEarthquake(feature.id(), properties.title(), properties.mag(), properties.magType(),
				properties.place(), instant(properties.time()), instant(properties.updated()),
				coordinate(coordinates, 1), coordinate(coordinates, 0), coordinate(coordinates, 2), properties.type(),
				properties.status(), properties.sig(), properties.felt(), properties.alert(), properties.url());
	}

	private static String instant(Long millis) {
		return (millis != null) ? Instant.ofEpochMilli(millis).toString() : null;
	}

	private static Double coordinate(List<Double> coordinates, int index) {
		return (coordinates.size() > index) ? coordinates.get(index) : null;
	}

	/**
	 * One page of earthquakes, as the <code>UsgsEarthquakePage</code> type.
	 */
	record Page(List<UsgsEarthquake> earthquakes, int page, int totalPages, int totalEarthquakes) {

	}

}
