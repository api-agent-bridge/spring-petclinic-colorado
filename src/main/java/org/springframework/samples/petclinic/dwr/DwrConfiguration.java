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
package org.springframework.samples.petclinic.dwr;

import java.net.URI;
import java.time.Duration;

import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.samples.petclinic.upstream.RestOperationFetcher;
import org.springframework.samples.petclinic.upstream.RestOperationTable;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Exposes the read operations of the Colorado Division of Water Resources (DWR) as root
 * fields of the schema, one field for each operation. DWR documents its REST services
 * with HTML help pages only. A generator read those pages into an OpenAPI document, and
 * wrote the fields, their types and <code>dwr/operations.json</code> from it. One
 * {@link RestOperationFetcher} for each field calls DWR.
 * <p>
 * Every request asks for <code>format=jsonforced</code>. DWR answers an empty search with
 * <code>404 Not Found</code> and a sentence of plain text, and with this format it sends
 * an empty page.
 */
@Configuration(proxyBeanMethods = false)
class DwrConfiguration {

	/**
	 * The name of the service in errors, in words a client can read.
	 */
	static final String UPSTREAM = "The Colorado Division of Water Resources";

	@Bean
	RuntimeWiringConfigurer dwrOperations(RestClient.Builder builder, JsonMapper mapper,
			@Value("${petclinic.upstream.dwr.url}") String url,
			@Value("${petclinic.upstream.dwr.api-key}") String apiKey,
			@Value("${petclinic.upstream.connect-timeout}") Duration connectTimeout,
			@Value("${petclinic.upstream.read-timeout}") Duration readTimeout) {
		HttpClientSettings timeouts = HttpClientSettings.defaults().withTimeouts(connectTimeout, readTimeout);
		RestClient client = builder.requestFactory(ClientHttpRequestFactoryBuilder.detect().build(timeouts))
			.requestInterceptor(jsonForced())
			.defaultHeaders((headers) -> {
				// without a key DWR allows 1,000 calls a day, and a key raises the limit
				if (StringUtils.hasText(apiKey)) {
					headers.set("Token", apiKey);
				}
			})
			.build();
		RestOperationTable table = RestOperationTable.read(new ClassPathResource("dwr/operations.json"), mapper);
		return (wiring) -> table.register(wiring, UPSTREAM, url, client, mapper);
	}

	/**
	 * Adds <code>format=jsonforced</code> to every request, so a search without results
	 * returns an empty page.
	 */
	private static ClientHttpRequestInterceptor jsonForced() {
		return (request, body, execution) -> {
			URI uri = UriComponentsBuilder.fromUri(request.getURI())
				.queryParam("format", "jsonforced")
				.build(true)
				.toUri();
			HttpRequest jsonRequest = new HttpRequestWrapper(request) {

				@Override
				public URI getURI() {
					return uri;
				}

			};
			return execution.execute(jsonRequest, body);
		};
	}

}
