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
package org.springframework.samples.petclinic.awdb;

import java.time.Duration;

import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.samples.petclinic.upstream.RestOperationFetcher;
import org.springframework.samples.petclinic.upstream.RestOperationTable;
import org.springframework.web.client.RestClient;

/**
 * Exposes the read operations of the REST API of the Air and Water Database (AWDB) of the
 * Natural Resources Conservation Service as root fields of the schema: its stations,
 * among them the SNOTEL snow stations, their readings, the water supply forecasts and the
 * reference data. A generator wrote the fields, their types and
 * <code>awdb/operations.json</code> from the OpenAPI document of the API. One
 * {@link RestOperationFetcher} for each field calls the service.
 */
@Configuration(proxyBeanMethods = false)
class AwdbConfiguration {

	/**
	 * The name of the service in errors, in words a client can read.
	 */
	static final String UPSTREAM = "The NRCS Air and Water Database";

	@Bean
	RuntimeWiringConfigurer awdbOperations(RestClient.Builder builder, JsonMapper mapper,
			@Value("${petclinic.upstream.awdb.url}") String url,
			@Value("${petclinic.upstream.connect-timeout}") Duration connectTimeout,
			@Value("${petclinic.upstream.read-timeout}") Duration readTimeout) {
		HttpClientSettings timeouts = HttpClientSettings.defaults().withTimeouts(connectTimeout, readTimeout);
		RestClient client = builder.requestFactory(ClientHttpRequestFactoryBuilder.detect().build(timeouts)).build();
		RestOperationTable table = RestOperationTable.read(new ClassPathResource("awdb/operations.json"), mapper);
		return (wiring) -> table.register(wiring, UPSTREAM, url, client, mapper);
	}

}
