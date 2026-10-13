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

/**
 * An address the Census geocoder found, as the <code>CensusAddressMatch</code> type of
 * the GraphQL schema. The schema describes each field.
 *
 * @param matchedAddress the address as the geocoder understood it, in capitals
 * @param latitude the latitude of the point, in degrees
 * @param longitude the longitude of the point, in degrees
 * @param city the city
 * @param state the two-letter code of the state
 * @param zipCode the ZIP code
 * @param fromHouseNumber the first house number of the street segment the point was
 * interpolated along
 * @param toHouseNumber the last house number of that segment
 */
public record CensusAddressMatch(String matchedAddress, double latitude, double longitude, String city, String state,
		String zipCode, String fromHouseNumber, String toHouseNumber) {

	static CensusAddressMatch from(CensusGeocoderApi.AddressMatch match) {
		CensusGeocoderApi.AddressComponents parts = match.addressComponents();
		return new CensusAddressMatch(match.matchedAddress(), match.coordinates().y(), match.coordinates().x(),
				(parts != null) ? parts.city() : null, (parts != null) ? parts.state() : null,
				(parts != null) ? parts.zip() : null, (parts != null) ? parts.fromAddress() : null,
				(parts != null) ? parts.toAddress() : null);
	}

}
