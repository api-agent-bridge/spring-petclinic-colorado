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
package org.springframework.samples.petclinic.nps;

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
 * Exposes the read operations of the National Park Service API as root fields of the
 * schema, one field for each operation: the parks, their alerts, campgrounds, things to
 * do and more. A generator wrote the fields, their types and
 * <code>nps/operations.json</code> from the NPS Swagger document and from answers
 * recorded from the live API. One {@link RestOperationFetcher} for each field calls the
 * NPS.
 * <p>
 * Every call carries a key from api.data.gov in the <code>X-Api-Key</code> header. The
 * default, <code>DEMO_KEY</code>, answered about ten calls on 12 October 2026 and then
 * asked to retry ten hours later, so a demo against the live API needs a key of its own
 * in the <code>NPS_API_KEY</code> environment variable.
 */
@Configuration(proxyBeanMethods = false)
class NpsConfiguration {

	/**
	 * The name of the service in errors, in words a client can read.
	 */
	static final String UPSTREAM = "The National Park Service API";

	@Bean
	RuntimeWiringConfigurer npsOperations(RestClient.Builder builder, JsonMapper mapper,
			@Value("${petclinic.upstream.nps.url}") String url,
			@Value("${petclinic.upstream.nps.api-key}") String apiKey,
			@Value("${petclinic.upstream.connect-timeout}") Duration connectTimeout,
			@Value("${petclinic.upstream.read-timeout}") Duration readTimeout) {
		HttpClientSettings timeouts = HttpClientSettings.defaults().withTimeouts(connectTimeout, readTimeout);
		RestClient client = builder.requestFactory(ClientHttpRequestFactoryBuilder.detect().build(timeouts))
			.defaultHeader("X-Api-Key", apiKey)
			.build();
		RestOperationTable table = RestOperationTable.read(new ClassPathResource("nps/operations.json"), mapper);
		return (wiring) -> table.register(wiring, UPSTREAM, url, client, mapper);
	}

}
