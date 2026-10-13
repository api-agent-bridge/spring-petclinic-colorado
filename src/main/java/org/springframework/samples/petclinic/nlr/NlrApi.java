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

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * The Alternative Fuel Stations API of the National Laboratory of the Rockies (NLR), the
 * national laboratory in Golden, Colorado, formerly the National Renewable Energy
 * Laboratory. It lists electric chargers and other alternative fuel stations across the
 * United States. The <code>location</code> parameter, which took an address, now answers
 * <code>422</code>, so a search takes a latitude and a longitude.
 */
@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE)
interface NlrApi {

	/**
	 * Returns the open, public stations nearest to a point, nearest first.
	 * @param query <code>latitude</code>, <code>longitude</code>, <code>radius</code> in
	 * miles, <code>fuel_type</code>, <code>limit</code> and <code>offset</code>
	 */
	@GetExchange("/api/alt-fuel-stations/v1/nearest.json")
	Nearest nearest(@RequestParam MultiValueMap<String, String> query);

	/**
	 * The answer of a search.
	 *
	 * @param totalResults the number of stations within the radius
	 * @param fuelStations the stations of this page
	 */
	record Nearest(@JsonProperty("total_results") int totalResults,
			@JsonProperty("fuel_stations") List<NlrFuelStation> fuelStations) {

	}

	/**
	 * Registers the interface as an HTTP service in the group <code>nlr</code>, whose
	 * base URL and timeouts come from <code>spring.http.serviceclient.nlr.*</code>.
	 */
	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(group = "nlr", types = NlrApi.class)
	class Registration {

		/**
		 * Sends the key of api.data.gov in the <code>X-Api-Key</code> header, so that it
		 * stays out of the URLs that the request log shows.
		 */
		@Bean
		RestClientHttpServiceGroupConfigurer nlrApiKey(@Value("${petclinic.upstream.nlr.api-key}") String key) {
			return (groups) -> groups.filterByName("nlr")
				.forEachClient((group, builder) -> builder.defaultHeader("X-Api-Key", key));
		}

	}

}
