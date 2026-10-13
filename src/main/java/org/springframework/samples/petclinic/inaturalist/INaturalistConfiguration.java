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
package org.springframework.samples.petclinic.inaturalist;

import java.time.Duration;

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
 * Exposes the read operations of iNaturalist, where people record sightings of animals,
 * plants and fungi, as root fields of the schema, one field for each operation. A
 * generator wrote the fields, their types and <code>inaturalist/operations.json</code>
 * from iNaturalist's Swagger document and from answers recorded from the live API,
 * because the document leaves many answers without a schema. One
 * {@link RestOperationFetcher} for each field calls iNaturalist.
 */
@Configuration(proxyBeanMethods = false)
class INaturalistConfiguration {

	/**
	 * The name of the service in errors, in words a client can read.
	 */
	static final String UPSTREAM = "iNaturalist";

	@Bean
	RuntimeWiringConfigurer inaturalistOperations(RestClient.Builder builder, JsonMapper mapper,
			@Value("${petclinic.upstream.inaturalist.url}") String url,
			@Value("${petclinic.upstream.inaturalist.user-agent}") String userAgent,
			@Value("${petclinic.upstream.connect-timeout}") Duration connectTimeout,
			@Value("${petclinic.upstream.read-timeout}") Duration readTimeout) {
		HttpClientSettings timeouts = HttpClientSettings.defaults().withTimeouts(connectTimeout, readTimeout);
		// the User-Agent names the application, so iNaturalist can tell where the calls
		// come from when it looks at its traffic
		RestClient client = builder.requestFactory(ClientHttpRequestFactoryBuilder.detect().build(timeouts))
			.defaultHeader(HttpHeaders.USER_AGENT, userAgent)
			.build();
		RestOperationTable table = RestOperationTable.read(new ClassPathResource("inaturalist/operations.json"),
				mapper);
		return (wiring) -> table.register(wiring, UPSTREAM, url, client, mapper);
	}

}
