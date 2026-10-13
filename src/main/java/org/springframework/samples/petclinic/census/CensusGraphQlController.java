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

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.samples.petclinic.upstream.InvalidArgumentException;
import org.springframework.stereotype.Controller;

/**
 * Answers the <code>censusGeocodeAddress</code> query of the GraphQL schema. Every query
 * costs one call to the Census geocoder.
 */
@Controller
class CensusGraphQlController {

	static final int MAX_ADDRESS_LENGTH = 100;

	private final CensusGeocoder geocoder;

	CensusGraphQlController(CensusGeocoder geocoder) {
		this.geocoder = geocoder;
	}

	@QueryMapping
	List<CensusAddressMatch> censusGeocodeAddress(@Argument String address) {
		String line = address.strip();
		// the geocoder refuses both with 400 Bad Request
		if (line.isEmpty() || line.length() > MAX_ADDRESS_LENGTH) {
			throw new InvalidArgumentException("address must hold 1 to " + MAX_ADDRESS_LENGTH + " characters");
		}
		return this.geocoder.geocode(line);
	}

}
