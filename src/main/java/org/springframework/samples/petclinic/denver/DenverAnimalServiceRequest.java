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

import org.springframework.samples.petclinic.denver.DenverOpenDataApi.ServiceRequestAttributes;

/**
 * An animal case of Denver's 311 service requests, as the
 * <code>DenverAnimalServiceRequest</code> type of the GraphQL schema. The schema
 * describes each field.
 */
record DenverAnimalServiceRequest(String id, DenverAnimalCaseType caseType, String summary, String status,
		String source, LocalDate createdDate, String createdAt, LocalDate closedDate, String closedAt, String agency,
		String address, String intersection, String zipCode, Integer councilDistrict, Integer policeDistrict,
		Double latitude, Double longitude) {

	static DenverAnimalServiceRequest from(ServiceRequestAttributes row) {
		return new DenverAnimalServiceRequest(String.valueOf(row.id()),
				DenverAnimalCaseType.of(row.summary()).orElse(null), row.summary(), row.status(), row.source(),
				DenverOpenDataApi.day(row.createdDate()), row.createdAt(), DenverOpenDataApi.day(row.closedDate()),
				row.closedAt(), row.agency(), row.address(), intersection(row.intersection(), row.crossStreet()),
				zipCode(row.zipCode()), row.councilDistrict(), row.policeDistrict(), row.latitude(), row.longitude());
	}

	/**
	 * Joins the two streets of an intersection, or returns the one that is there.
	 */
	private static String intersection(String street, String crossStreet) {
		if (street == null || crossStreet == null) {
			return (street != null) ? street : crossStreet;
		}
		return street + " and " + crossStreet;
	}

	/**
	 * Reads the ZIP code, which some cases hold as a dash.
	 */
	private static String zipCode(String zipCode) {
		return (zipCode == null || zipCode.isBlank() || "-".equals(zipCode.strip())) ? null : zipCode.strip();
	}

	/**
	 * One page of animal cases, as the <code>DenverAnimalServiceRequestPage</code> type.
	 */
	record Page(List<DenverAnimalServiceRequest> requests, int page, int totalPages, int totalRequests) {

	}

}
