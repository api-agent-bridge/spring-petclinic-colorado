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

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * The earthquake catalogue of the US Geological Survey, through the standard FDSN event
 * web service. The National Earthquake Information Center, in Golden, Colorado, runs it.
 * The service answers a bad argument with <code>400 Bad Request</code> and a text that
 * names the argument.
 */
@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE)
interface UsgsApi {

	/**
	 * Returns the earthquakes that match the filters, as GeoJSON.
	 * @param query the filters and the paging: <code>starttime</code>,
	 * <code>minmagnitude</code>, a point with <code>latitude</code>,
	 * <code>longitude</code> and <code>maxradiuskm</code>, or a box with
	 * <code>minlatitude</code> and its three siblings, <code>orderby</code>,
	 * <code>limit</code>, and <code>offset</code>, which starts at 1
	 */
	@GetExchange("/query?format=geojson")
	FeatureCollection query(@RequestParam MultiValueMap<String, String> query);

	/**
	 * Returns the number of earthquakes that match the filters.
	 * @param query the same filters as for {@link #query(MultiValueMap)}
	 */
	@GetExchange("/count?format=geojson")
	Count count(@RequestParam MultiValueMap<String, String> query);

	record FeatureCollection(List<Feature> features) {

	}

	/**
	 * One earthquake.
	 *
	 * @param id the id of the event, for example <code>us7000thsk</code>
	 * @param properties what the catalogue knows about it
	 * @param geometry the epicentre as longitude, latitude and depth in kilometres
	 */
	record Feature(String id, Properties properties, Geometry geometry) {

	}

	/**
	 * The properties of an earthquake. The times are milliseconds since 1970.
	 */
	record Properties(Double mag, String magType, String place, Long time, Long updated, String url, Integer felt,
			String alert, String status, Integer sig, String type, String title) {

	}

	record Geometry(List<Double> coordinates) {

	}

	/**
	 * The number of matches.
	 *
	 * @param count the number of earthquakes that match
	 * @param maxAllowed the most earthquakes one query may return
	 */
	record Count(int count, int maxAllowed) {

	}

	/**
	 * Registers the interface as an HTTP service in the group <code>usgs</code>, whose
	 * base URL and timeouts come from <code>spring.http.serviceclient.usgs.*</code>.
	 */
	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(group = "usgs", types = UsgsApi.class)
	class Registration {

	}

}
