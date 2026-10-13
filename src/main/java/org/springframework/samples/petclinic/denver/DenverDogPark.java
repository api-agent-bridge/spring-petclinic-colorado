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

import org.springframework.samples.petclinic.denver.DenverOpenDataApi.DogParkAttributes;
import org.springframework.samples.petclinic.denver.DenverOpenDataApi.Feature;

/**
 * An off-leash dog park of the City and County of Denver, as the
 * <code>DenverDogPark</code> type of the GraphQL schema. The schema describes each field.
 *
 * @param distanceInMetres the distance from the address the parks were searched around,
 * or null when the parks were listed without an address
 */
record DenverDogPark(String id, String name, String locationCode, Boolean fenced, Boolean shade, Boolean water,
		Double acres, Integer yearBuilt, String condition, LocalDate conditionAssessedDate, Double latitude,
		Double longitude, Integer distanceInMetres) {

	static DenverDogPark from(Feature<DogParkAttributes> feature) {
		DogParkAttributes park = feature.attributes();
		DenverOpenDataApi.Point centre = feature.centroid();
		return new DenverDogPark(String.valueOf(park.id()), park.name(), park.locationCode(), yes(park.fenced()),
				yes(park.shade()), yes(park.water()), park.acres(), park.yearBuilt(), park.condition(),
				DenverOpenDataApi.day(park.conditionAssessedDate()), (centre != null) ? centre.y() : null,
				(centre != null) ? centre.x() : null, null);
	}

	/**
	 * Returns the same park with its distance from a point, measured to the centre of the
	 * park. A park without a centre keeps a null distance.
	 */
	DenverDogPark withDistanceFrom(GeoPoint point) {
		if (this.latitude == null || this.longitude == null) {
			return this;
		}
		int distance = (int) Math.round(point.distanceInMetresTo(new GeoPoint(this.latitude, this.longitude)));
		return new DenverDogPark(this.id, this.name, this.locationCode, this.fenced, this.shade, this.water, this.acres,
				this.yearBuilt, this.condition, this.conditionAssessedDate, this.latitude, this.longitude, distance);
	}

	/**
	 * Reads a field that holds <code>Yes</code> or <code>No</code>. Any other value, null
	 * among them, becomes null.
	 */
	private static Boolean yes(String value) {
		if ("Yes".equalsIgnoreCase(value)) {
			return true;
		}
		return "No".equalsIgnoreCase(value) ? false : null;
	}

}
