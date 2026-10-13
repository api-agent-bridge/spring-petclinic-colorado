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
package org.springframework.samples.petclinic.colorado;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The datasets of data.colorado.gov that the <code>colorado*</code> queries read, from
 * the properties <code>petclinic.upstream.colorado.*</code>.
 *
 * @param veterinaryLicenses the id of the license register of the Department of
 * Regulatory Agencies
 * @param shelterReports the id of the yearly shelter and rescue dataset, by year, oldest
 * year first
 */
@ConfigurationProperties("petclinic.upstream.colorado")
record ColoradoDatasets(String veterinaryLicenses, Map<Integer, String> shelterReports) {

	ColoradoDatasets {
		shelterReports = Collections
			.unmodifiableSortedMap((shelterReports != null) ? new TreeMap<>(shelterReports) : new TreeMap<>());
	}

}
