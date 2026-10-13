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
package org.springframework.samples.petclinic.census;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * The geocoder of the US Census Bureau, which turns an address in the United States into
 * a point. It places the point by interpolating along the range of house numbers of the
 * street segment, so the point is an estimate.
 */
@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE)
interface CensusGeocoderApi {

	/**
	 * Finds an address written on one line, for example
	 * <code>1437 Bannock St, Denver, CO</code>. An address the geocoder cannot find comes
	 * back with status <code>200</code> and an empty list of matches.
	 * @param address the address
	 * @param benchmark the version of the address ranges, <code>Public_AR_Current</code>
	 * for the latest
	 * @param format <code>json</code>
	 */
	@GetExchange("/locations/onelineaddress")
	Answer findAddress(@RequestParam String address, @RequestParam String benchmark, @RequestParam String format);

	record Answer(Result result) {

	}

	record Result(List<AddressMatch> addressMatches) {

	}

	/**
	 * One match, as the geocoder sends it.
	 *
	 * @param matchedAddress the address as the geocoder understood it, in capitals
	 * @param coordinates the point, with the longitude in <code>x</code> and the latitude
	 * in <code>y</code>
	 * @param addressComponents the parts of the address and the range of house numbers
	 * the point was interpolated along
	 */
	record AddressMatch(String matchedAddress, Coordinates coordinates, AddressComponents addressComponents) {

	}

	record Coordinates(double x, double y) {

	}

	record AddressComponents(String fromAddress, String toAddress, String city, String state, String zip) {

	}

	/**
	 * Registers the interface as an HTTP service in the group <code>census</code>, whose
	 * base URL and timeouts come from <code>spring.http.serviceclient.census.*</code>.
	 */
	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(group = "census", types = CensusGeocoderApi.class)
	class Registration {

	}

}
