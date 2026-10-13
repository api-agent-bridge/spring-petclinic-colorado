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

import java.time.Duration;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.samples.petclinic.upstream.RestUpstreams;
import org.springframework.stereotype.Component;

/**
 * Turns an address in the United States into points with the geocoder of the US Census
 * Bureau. The Denver dog parks near an owner use it, and so does the
 * <code>censusGeocodeAddress</code> query.
 */
@Component
public class CensusGeocoder {

	/**
	 * The name of the service in errors, in words a client can read.
	 */
	public static final String CENSUS = "The Census geocoder";

	private static final Log logger = LogFactory.getLog(CensusGeocoder.class);

	// the latest version of the address ranges
	private static final String BENCHMARK = "Public_AR_Current";

	private final CensusGeocoderApi api;

	CensusGeocoder(CensusGeocoderApi api) {
		this.api = api;
	}

	/**
	 * Returns the matches for an address, best first. The list is empty when the geocoder
	 * cannot find the address.
	 * @param address the address on one line, for example
	 * <code>1437 Bannock St, Denver, CO</code>
	 */
	public List<CensusAddressMatch> geocode(String address) {
		long started = System.nanoTime();
		CensusGeocoderApi.Answer answer = RestUpstreams.call(CENSUS,
				() -> this.api.findAddress(address, BENCHMARK, "json"));
		List<CensusAddressMatch> matches = (answer != null && answer.result() != null
				&& answer.result().addressMatches() != null)
						? answer.result().addressMatches().stream().map(CensusAddressMatch::from).toList() : List.of();
		logger.info("REST geocoder found %d matches for %s (%d ms)".formatted(matches.size(), address,
				Duration.ofNanos(System.nanoTime() - started).toMillis()));
		return matches;
	}

}
