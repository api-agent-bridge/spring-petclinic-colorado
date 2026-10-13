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
import java.util.List;

import org.springframework.samples.petclinic.denver.DenverOpenDataApi.BusinessLicenseAttributes;

/**
 * A kennel license of the City and County of Denver, as the
 * <code>DenverKennelLicense</code> type of the GraphQL schema. The schema describes each
 * field.
 */
record DenverKennelLicense(String licenseNumber, String entityName, String tradeName, String status,
		LocalDate expirationDate, String address, String zipCode, Double latitude, Double longitude) {

	static DenverKennelLicense from(BusinessLicenseAttributes row) {
		GeoPoint position = (row.x() != null && row.y() != null) ? new StatePlanePoint(row.x(), row.y()).toGeoPoint()
				: null;
		return new DenverKennelLicense(row.licenseNumber(), row.entityName(), row.tradeName(), row.status(),
				DenverOpenDataApi.day(row.expirationDate()), (row.address() != null) ? row.address().strip() : null,
				row.zipCode(), (position != null) ? position.latitude() : null,
				(position != null) ? position.longitude() : null);
	}

	/**
	 * One page of kennel licenses, as the <code>DenverKennelLicensePage</code> type.
	 */
	record Page(List<DenverKennelLicense> licenses, int page, int totalPages, int totalLicenses) {

	}

}
