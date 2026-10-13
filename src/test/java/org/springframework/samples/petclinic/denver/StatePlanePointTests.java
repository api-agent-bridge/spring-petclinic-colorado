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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests the {@link StatePlanePoint} and the {@link GeoPoint} with positions in Denver,
 * read from Denver's open data and the Census geocoder on 2026-10-12.
 */
class StatePlanePointTests {

	@Test
	void convertsTheStatePlaneFeetOfAKennelLicenseToLatitudeAndLongitude() {
		// Tender Touch Animal Hospital at 350 Kalamath St, as Denver's license table
		// holds it
		GeoPoint kennel = new StatePlanePoint(3140732, 1688199).toGeoPoint();

		// the Census geocoder interpolates the same address along its street segment
		GeoPoint geocoded = new GeoPoint(39.721741700713, -105.000107506743);
		assertThat(kennel.distanceInMetresTo(geocoded)).isLessThan(60);
	}

	@Test
	void measuresTheDistanceFromDev2NextToSkylinePark() {
		GeoPoint home = new GeoPoint(39.739040016914, -104.990311950936);
		GeoPoint skylinePark = new GeoPoint(39.74924133996786, -104.99428383699461);

		assertThat(home.distanceInMetresTo(skylinePark)).isCloseTo(1184, within(1.0));
	}

}
