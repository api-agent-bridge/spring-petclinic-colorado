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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * One row of the Colorado license register for a veterinary license, as the
 * <code>ColoradoVeterinaryLicense</code> type of the GraphQL schema. The register holds
 * one row for each disciplinary action, so a license with two actions comes back twice.
 *
 * @param licenseNumber the number of the license, unique within its type
 * @param licenseType the kind of license
 * @param firstName the first name of the license holder
 * @param middleName the middle name, or null
 * @param lastName the last name
 * @param city the city the license holder gave
 * @param state the state the license holder gave, for example CO
 * @param zipCode the ZIP code the license holder gave
 * @param status the status, for example <code>Active</code> or <code>Expired</code>
 * @param firstIssued the day the license was first issued
 * @param lastRenewed the day the latest renewal took effect
 * @param expires the day the license expires unless it is renewed
 * @param degrees the degrees the license holder declared
 * @param specialty the specialty, for the few licenses that name one
 * @param disciplinaryAction the public action of this row, or null for a row without one
 * @param verificationUrl the page of the state's license lookup for this license
 */
record ColoradoVeterinaryLicense(String licenseNumber, ColoradoVeterinaryLicenseType licenseType, String firstName,
		String middleName, String lastName, String city, String state, String zipCode, String status,
		LocalDate firstIssued, LocalDate lastRenewed, LocalDate expires, List<String> degrees, String specialty,
		DisciplinaryAction disciplinaryAction, String verificationUrl) {

	/**
	 * The timestamp pattern of the register's date columns. Every time is midnight.
	 */
	static final String TIMESTAMP = "yyyy-MM-dd'T'HH:mm:ss.SSS";

	/**
	 * The register writes the dates of disciplinary actions as text in the US order.
	 */
	private static final DateTimeFormatter US_DATE = DateTimeFormatter.ofPattern("M/d/yyyy");

	/**
	 * Builds the license from a row of the register, or returns null for a row of a type
	 * that is not veterinary.
	 */
	static ColoradoVeterinaryLicense from(Row row) {
		ColoradoVeterinaryLicenseType type = ColoradoVeterinaryLicenseType.fromCode(row.licensetype());
		if (type == null) {
			return null;
		}
		DisciplinaryAction action = (row.casenumber() != null || row.programaction() != null)
				? new DisciplinaryAction(row.casenumber(), row.programaction(), usDate(row.disciplineeffectivedate()),
						usDate(row.disciplinecompletedate()))
				: null;
		return new ColoradoVeterinaryLicense(row.licensenumber(), type, row.firstname(), row.middlename(),
				row.lastname(), row.city(), row.state(), row.mailzipcode(), row.licensestatusdescription(),
				row.licensefirstissuedate(), row.licenselastreneweddate(), row.licenseexpirationdate(),
				degrees(row.degrees()), row.specialty(), action,
				(row.linktoverifylicense() != null) ? row.linktoverifylicense().url() : null);
	}

	/**
	 * The register lists degrees after a tilde each, for example
	 * <code>~Doctor of Veterinary Medicine</code>.
	 */
	private static List<String> degrees(String degrees) {
		if (degrees == null) {
			return List.of();
		}
		return Arrays.stream(degrees.split("~")).map(String::strip).filter((degree) -> !degree.isEmpty()).toList();
	}

	/**
	 * Reads a date such as <code>12/31/2019</code>. A value in another form becomes null.
	 */
	private static LocalDate usDate(String text) {
		if (text == null || text.isBlank()) {
			return null;
		}
		try {
			return LocalDate.parse(text.strip(), US_DATE);
		}
		catch (DateTimeParseException ex) {
			return null;
		}
	}

	/**
	 * A public action that the state took on a license, as the
	 * <code>ColoradoDisciplinaryAction</code> type.
	 *
	 * @param caseNumber the number of the case, for example <code>2019-6811</code>
	 * @param action the kind of action, for example <code>CLS Letter of Admonition</code>
	 * @param effectiveDate the day the action took effect
	 * @param completeDate the day a limited action ended, or null
	 */
	record DisciplinaryAction(String caseNumber, String action, LocalDate effectiveDate, LocalDate completeDate) {

	}

	/**
	 * One page of licenses, as the <code>ColoradoVeterinaryLicensePage</code> type.
	 */
	record Page(List<ColoradoVeterinaryLicense> licenses, int page, int totalPages, int totalLicenses) {

	}

	/**
	 * A row of the register as Socrata sends it, with the register's column names.
	 */
	record Row(String licensenumber, String licensetype, String firstname, String middlename, String lastname,
			String city, String state, String mailzipcode, String licensestatusdescription,
			@JsonFormat(pattern = TIMESTAMP) LocalDate licensefirstissuedate,
			@JsonFormat(pattern = TIMESTAMP) LocalDate licenselastreneweddate,
			@JsonFormat(pattern = TIMESTAMP) LocalDate licenseexpirationdate, String degrees, String specialty,
			String casenumber, String programaction, String disciplineeffectivedate, String disciplinecompletedate,
			Link linktoverifylicense) {

	}

	/**
	 * A column of the URL type, which Socrata sends as an object.
	 */
	record Link(String url) {

	}

}
