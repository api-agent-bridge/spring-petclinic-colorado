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
package org.springframework.samples.petclinic.nws;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Map;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.http.HttpHeaders;
import org.springframework.samples.petclinic.upstream.RestOperationFetcher;
import org.springframework.samples.petclinic.upstream.RestOperationTable;
import org.springframework.web.client.RestClient;

/**
 * Exposes the read operations of the National Weather Service, api.weather.gov, as root
 * fields of the schema, one field for each operation that answers JSON. A generator wrote
 * the fields, their types and <code>nws/operations.json</code> from the service's OpenAPI
 * document. One {@link RestOperationFetcher} for each field calls the service.
 * <p>
 * The service answers <code>403 Forbidden</code> to a request without a
 * <code>User-Agent</code> that names the application, so the client sends one.
 */
@Configuration(proxyBeanMethods = false)
class NwsConfiguration {

	/**
	 * The name of the service in errors, in words a client can read.
	 */
	static final String UPSTREAM = "The National Weather Service";

	/**
	 * The key under which a JSON-LD answer holds its list.
	 */
	private static final String GRAPH = "@graph";

	@Bean
	RuntimeWiringConfigurer nwsOperations(RestClient.Builder builder, JsonMapper mapper,
			@Value("${petclinic.upstream.nws.url}") String url,
			@Value("${petclinic.upstream.nws.user-agent}") String userAgent,
			@Value("${petclinic.upstream.connect-timeout}") Duration connectTimeout,
			@Value("${petclinic.upstream.read-timeout}") Duration readTimeout) {
		HttpClientSettings timeouts = HttpClientSettings.defaults().withTimeouts(connectTimeout, readTimeout);
		RestClient client = builder.requestFactory(ClientHttpRequestFactoryBuilder.detect().build(timeouts))
			.defaultHeader(HttpHeaders.USER_AGENT, userAgent)
			.build();
		RestOperationTable table = RestOperationTable.read(new ClassPathResource("nws/operations.json"), mapper);
		Map<String, String> jsonLdLists = jsonLdLists(mapper);
		return (wiring) -> {
			table.register(wiring, UPSTREAM, url, client, mapper);
			// GraphQL does not allow the name @graph, so the schema reads that list
			// under another name
			jsonLdLists.forEach((typeName, field) -> wiring.type(typeName, (type) -> type.dataFetcher(field,
					(environment) -> ((Map<?, ?>) environment.getSource()).get(GRAPH))));
		};
	}

	/**
	 * Reads the types whose answers hold their list under <code>@graph</code>, as the
	 * JSON-LD answers of the service do, and the field of the schema that holds the list.
	 */
	private static Map<String, String> jsonLdLists(JsonMapper mapper) {
		ClassPathResource resource = new ClassPathResource("nws/json-ld-lists.json");
		try (InputStream json = resource.getInputStream()) {
			return mapper.readValue(json, new TypeReference<Map<String, String>>() {
			});
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Cannot read " + resource, ex);
		}
	}

}
