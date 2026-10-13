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

import org.junit.jupiter.api.Test;

import org.springframework.samples.petclinic.upstream.MockUpstreamTests;

/**
 * Queries the Census geocoder in mock mode, against responses recorded on 2026-10-12.
 */
class CensusMockUpstreamTests extends MockUpstreamTests {

	@Test
	void findsAnAddressInDenver() {
		this.graphQl.document(
				"""
						{ censusGeocodeAddress(address: "1437 Bannock St, Denver, CO") { matchedAddress latitude longitude } }""")
			.execute()
			.path("censusGeocodeAddress[0].matchedAddress")
			.entity(String.class)
			.isEqualTo("1437 BANNOCK ST, DENVER, CO, 80202")
			.path("censusGeocodeAddress[0].latitude")
			.entity(Double.class)
			.isEqualTo(39.739040016914)
			.path("censusGeocodeAddress[0].longitude")
			.entity(Double.class)
			.isEqualTo(-104.990311950936);
	}

	@Test
	void anAddressWithoutAStateCanMatchAnotherState() {
		this.graphQl.document("{ censusGeocodeAddress(address: \"110 W. Liberty St., Madison\") { matchedAddress } }")
			.execute()
			.path("censusGeocodeAddress[0].matchedAddress")
			.entity(String.class)
			.isEqualTo("110 LIBERTY ST, MADISON, CT, 06443");
	}

	@Test
	void anAddressTheGeocoderCannotFindHasNoMatches() {
		this.graphQl.document("{ censusGeocodeAddress(address: \"1 Nowhere Lane, Denver, CO\") { matchedAddress } }")
			.execute()
			.path("censusGeocodeAddress")
			.entityList(Object.class)
			.hasSize(0);
	}

	@Test
	void rejectsABlankAddress() {
		this.graphQl.document("{ censusGeocodeAddress(address: \"  \") { matchedAddress } }")
			.execute()
			.errors()
			.expect((error) -> "address must hold 1 to 100 characters".equals(error.getMessage()))
			.verify();
	}

}
