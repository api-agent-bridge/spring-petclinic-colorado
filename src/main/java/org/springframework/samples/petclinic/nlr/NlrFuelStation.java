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
package org.springframework.samples.petclinic.nlr;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An alternative fuel station, as the <code>NlrFuelStation</code> type of the GraphQL
 * schema. The Jackson annotations map NLR's names to the names of the schema.
 *
 * @param id the id of the station
 * @param name the name, for example CCD CHARGING CITY HALL 1
 * @param fuelType the code of the fuel, for example ELEC
 * @param streetAddress the street address
 * @param city the city
 * @param state the state, for example CO
 * @param zip the ZIP code
 * @param latitude the latitude
 * @param longitude the longitude
 * @param distanceInMiles the distance from the point of the search
 * @param accessDaysTime when the station is open
 * @param phone the telephone number
 * @param evNetwork the charging network of an electric station
 * @param evLevel2Ports the number of level 2 charging ports
 * @param evDcFastPorts the number of DC fast charging ports
 * @param evConnectorTypes the connectors of an electric station, for example J1772
 * @param intersectionDirections how to find the station
 * @param nationalParkUnit the national park the station is in, or null
 * @param openDate the day the station opened
 * @param dateLastConfirmed the day NLR last confirmed the station's details
 */
record NlrFuelStation(int id, @JsonProperty("station_name") String name,
		@JsonProperty("fuel_type_code") String fuelType, @JsonProperty("street_address") String streetAddress,
		String city, String state, String zip, Double latitude, Double longitude,
		@JsonProperty("distance") Double distanceInMiles, @JsonProperty("access_days_time") String accessDaysTime,
		@JsonProperty("station_phone") String phone, @JsonProperty("ev_network") String evNetwork,
		@JsonProperty("ev_level2_evse_num") Integer evLevel2Ports,
		@JsonProperty("ev_dc_fast_num") Integer evDcFastPorts,
		@JsonProperty("ev_connector_types") List<String> evConnectorTypes,
		@JsonProperty("intersection_directions") String intersectionDirections,
		@JsonProperty("nps_unit_name") String nationalParkUnit, @JsonProperty("open_date") LocalDate openDate,
		@JsonProperty("date_last_confirmed") LocalDate dateLastConfirmed) {

	NlrFuelStation {
		evConnectorTypes = (evConnectorTypes != null) ? evConnectorTypes : List.of();
	}

	/**
	 * One page of stations, as the <code>NlrFuelStationPage</code> type.
	 */
	record Page(List<NlrFuelStation> stations, int page, int totalPages, int totalStations) {

	}

}
