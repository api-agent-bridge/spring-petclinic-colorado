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
package org.springframework.samples.petclinic.usgs;

/**
 * A box of latitudes and longitudes, as the <code>UsgsBox</code> input type of the
 * GraphQL schema.
 *
 * @param minLatitude the southern edge
 * @param maxLatitude the northern edge
 * @param minLongitude the western edge
 * @param maxLongitude the eastern edge
 */
record UsgsBox(double minLatitude, double maxLatitude, double minLongitude, double maxLongitude) {

	/**
	 * The box around Colorado, whose borders follow lines of latitude and longitude.
	 */
	static final UsgsBox COLORADO = new UsgsBox(36.99, 41.0, -109.06, -102.04);

}
