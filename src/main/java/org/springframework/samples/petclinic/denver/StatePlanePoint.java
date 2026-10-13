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
 * A point in Colorado Central State Plane (EPSG:2877), the grid Denver's tables use for
 * the position of an address. Its coordinates are US survey feet.
 *
 * @param x feet east
 * @param y feet north
 */
record StatePlanePoint(double x, double y) {

	// NAD83 / Colorado Central (ftUS) is a Lambert conformal conic projection with two
	// standard parallels (EPSG method 9802) on the GRS 1980 ellipsoid
	private static final double SEMI_MAJOR_AXIS = 6_378_137.0;

	private static final double FLATTENING = 1 / 298.257222101;

	private static final double ECCENTRICITY = Math.sqrt(2 * FLATTENING - FLATTENING * FLATTENING);

	private static final double US_SURVEY_FOOT = 1200.0 / 3937.0;

	private static final double ORIGIN_LATITUDE = Math.toRadians(37 + 50.0 / 60);

	private static final double CENTRAL_MERIDIAN = Math.toRadians(-105.5);

	private static final double FIRST_PARALLEL = Math.toRadians(39.75);

	private static final double SECOND_PARALLEL = Math.toRadians(38 + 27.0 / 60);

	private static final double FALSE_EASTING = 3_000_000 * US_SURVEY_FOOT;

	private static final double FALSE_NORTHING = 1_000_000 * US_SURVEY_FOOT;

	private static final double N = (Math.log(m(FIRST_PARALLEL)) - Math.log(m(SECOND_PARALLEL)))
			/ (Math.log(t(FIRST_PARALLEL)) - Math.log(t(SECOND_PARALLEL)));

	private static final double F = m(FIRST_PARALLEL) / (N * Math.pow(t(FIRST_PARALLEL), N));

	private static final double ORIGIN_RADIUS = SEMI_MAJOR_AXIS * F * Math.pow(t(ORIGIN_LATITUDE), N);

	/**
	 * Converts the point to latitude and longitude on NAD83, which matches WGS84 within a
	 * metre or two in Colorado.
	 */
	GeoPoint toGeoPoint() {
		double east = this.x * US_SURVEY_FOOT - FALSE_EASTING;
		double north = ORIGIN_RADIUS - (this.y * US_SURVEY_FOOT - FALSE_NORTHING);
		double radius = Math.copySign(Math.hypot(east, north), N);
		double tee = Math.pow(radius / (SEMI_MAJOR_AXIS * F), 1 / N);
		double longitude = Math.atan2(east, north) / N + CENTRAL_MERIDIAN;
		double latitude = Math.PI / 2 - 2 * Math.atan(tee);
		// the latitude needs a few rounds to settle on the ellipsoid
		for (int round = 0; round < 6; round++) {
			double sine = ECCENTRICITY * Math.sin(latitude);
			latitude = Math.PI / 2 - 2 * Math.atan(tee * Math.pow((1 - sine) / (1 + sine), ECCENTRICITY / 2));
		}
		return new GeoPoint(Math.toDegrees(latitude), Math.toDegrees(longitude));
	}

	private static double m(double latitude) {
		double sine = ECCENTRICITY * Math.sin(latitude);
		return Math.cos(latitude) / Math.sqrt(1 - sine * sine);
	}

	private static double t(double latitude) {
		double sine = ECCENTRICITY * Math.sin(latitude);
		return Math.tan(Math.PI / 4 - latitude / 2) / Math.pow((1 - sine) / (1 + sine), ECCENTRICITY / 2);
	}

}
