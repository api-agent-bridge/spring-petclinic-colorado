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

/**
 * A point on the Earth in latitude and longitude, in degrees on WGS84.
 *
 * @param latitude degrees north
 * @param longitude degrees east, negative in Colorado
 */
record GeoPoint(double latitude, double longitude) {

	// the mean radius of the Earth
	private static final double EARTH_RADIUS_IN_METRES = 6_371_008.8;

	/**
	 * Measures the distance to another point along the surface of a sphere, which is
	 * close enough for the few kilometres across a city.
	 */
	double distanceInMetresTo(GeoPoint other) {
		double latitude1 = Math.toRadians(this.latitude);
		double latitude2 = Math.toRadians(other.latitude);
		double sinLatitude = Math.sin((latitude2 - latitude1) / 2);
		double sinLongitude = Math.sin(Math.toRadians(other.longitude - this.longitude) / 2);
		double a = sinLatitude * sinLatitude + Math.cos(latitude1) * Math.cos(latitude2) * sinLongitude * sinLongitude;
		return 2 * EARTH_RADIUS_IN_METRES * Math.asin(Math.min(1, Math.sqrt(a)));
	}

}
