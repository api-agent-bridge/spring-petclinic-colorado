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
package org.springframework.samples.petclinic.upstream;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import graphql.schema.idl.RuntimeWiring;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.core.io.Resource;
import org.springframework.web.client.RestClient;

/**
 * The read operations of one REST service, as a generator wrote them from the service's
 * OpenAPI document into a JSON file next to the schema.
 *
 * @param operations the operations, one for each root field
 * @param mapFields the fields that hold a map in the service's answers, by type. The
 * schema has a list of entries with a key and a value for each of them.
 */
public record RestOperationTable(List<RestOperation> operations, Map<String, List<String>> mapFields) {

	public RestOperationTable {
		mapFields = (mapFields != null) ? mapFields : Map.of();
	}

	/**
	 * Reads the operations of one service from the JSON file that the generator wrote.
	 */
	public static RestOperationTable read(Resource resource, JsonMapper mapper) {
		try (InputStream json = resource.getInputStream()) {
			return mapper.readValue(json, RestOperationTable.class);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Cannot read the operations in " + resource, ex);
		}
	}

	/**
	 * Registers a {@link RestOperationFetcher} for each root field, and a fetcher that
	 * turns a map into entries for each map field.
	 * @param wiring the runtime wiring of the schema
	 * @param upstream the name of the service, in words a client can read
	 * @param baseUrl the URL the paths of the operations start from
	 * @param client the client that calls the service
	 * @param mapper the mapper that reads the JSON of the answers
	 */
	public void register(RuntimeWiring.Builder wiring, String upstream, String baseUrl, RestClient client,
			JsonMapper mapper) {
		wiring.type("Query", (type) -> {
			this.operations.forEach((operation) -> type.dataFetcher(operation.field(),
					new RestOperationFetcher(upstream, baseUrl, client, operation, mapper)));
			return type;
		});
		this.mapFields.forEach((typeName, fields) -> wiring.type(typeName, (type) -> {
			fields.forEach((field) -> type.dataFetcher(field,
					(environment) -> RestOperationFetcher.entries(((Map<?, ?>) environment.getSource()).get(field))));
			return type;
		}));
	}

}
