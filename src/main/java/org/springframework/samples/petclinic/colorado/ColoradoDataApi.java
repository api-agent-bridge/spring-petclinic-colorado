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

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * The State of Colorado's open data portal, data.colorado.gov, which runs on Socrata.
 * Every dataset answers SoQL queries at <code>/resource/&lt;id&gt;.json</code>: the
 * parameters <code>$select</code>, <code>$where</code>, <code>$order</code>,
 * <code>$limit</code> and <code>$offset</code> work like the clauses of SQL. Socrata
 * sends every value as text, numbers included.
 */
@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE)
interface ColoradoDataApi {

	/**
	 * Returns the rows of a dataset that match a SoQL query, with the license register's
	 * columns.
	 * @param dataset the id of the dataset, for example <code>7s5z-vewr</code>
	 * @param query the SoQL parameters
	 */
	@GetExchange("/resource/{dataset}.json")
	List<ColoradoVeterinaryLicense.Row> licenses(@PathVariable String dataset,
			@RequestParam MultiValueMap<String, String> query);

	/**
	 * Returns the rows of a dataset that match a SoQL query, as text by column name. A
	 * query that selects <code>count(*)</code> answers with one row that holds the
	 * <code>count</code>.
	 * @param dataset the id of the dataset
	 * @param query the SoQL parameters
	 */
	@GetExchange("/resource/{dataset}.json")
	List<Map<String, String>> rows(@PathVariable String dataset, @RequestParam MultiValueMap<String, String> query);

	/**
	 * Returns the description of a dataset, with its columns in the order of the portal's
	 * table.
	 * @param dataset the id of the dataset
	 */
	@GetExchange("/api/views/{dataset}.json")
	Dataset dataset(@PathVariable String dataset);

	/**
	 * The description of a dataset.
	 *
	 * @param name the title on the portal
	 * @param columns the columns
	 */
	record Dataset(String name, List<Column> columns) {

	}

	/**
	 * A column of a dataset.
	 *
	 * @param fieldName the name a query uses, for example <code>adult_dogs_stray</code>
	 * @param name the name the portal shows, for example <code>Adult Dogs Stray</code>
	 * @param position the place of the column in the table, starting at 1
	 */
	record Column(String fieldName, String name, int position) {

	}

	/**
	 * Registers the interface as an HTTP service in the group <code>colorado</code>,
	 * whose base URL and timeouts come from
	 * <code>spring.http.serviceclient.colorado.*</code>.
	 */
	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(group = "colorado", types = ColoradoDataApi.class)
	@EnableConfigurationProperties(ColoradoDatasets.class)
	class Registration {

	}

}
