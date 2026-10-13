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
 * The orders of <code>usgsEarthquakes</code>, as the <code>UsgsEarthquakeOrder</code>
 * enum of the GraphQL schema.
 */
enum UsgsEarthquakeOrder {

	/**
	 * The most recent first.
	 */
	TIME("time"),

	/**
	 * The strongest first.
	 */
	MAGNITUDE("magnitude");

	private final String parameter;

	UsgsEarthquakeOrder(String parameter) {
		this.parameter = parameter;
	}

	/**
	 * The value of the <code>orderby</code> parameter.
	 */
	String parameter() {
		return this.parameter;
	}

}
