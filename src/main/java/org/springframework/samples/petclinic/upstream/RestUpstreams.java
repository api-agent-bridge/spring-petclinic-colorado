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

import java.time.Duration;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;

/**
 * Shared setup of the REST services behind the GraphQL schema. Each service is an HTTP
 * interface, imported with <code>@ImportHttpServices</code> into a group of its own.
 * Spring Boot reads each group's base URL and timeouts from the properties
 * <code>spring.http.serviceclient.&lt;group&gt;.*</code>.
 */
@Configuration(proxyBeanMethods = false)
public class RestUpstreams {

	private static final Log logger = LogFactory.getLog(RestUpstreams.class);

	/**
	 * Calls an external service through its HTTP interface, and turns a failure into an
	 * {@link UpstreamException} that names the service. A failure covers an error status,
	 * a timeout and a connection that could not be opened.
	 * @param upstream the name of the service, in words a client can read
	 * @param call the call to the HTTP interface
	 */
	public static <T> T call(String upstream, Supplier<T> call) {
		try {
			return call.get();
		}
		catch (RestClientException ex) {
			throw new UpstreamException(upstream, ex);
		}
	}

	/**
	 * Logs every request to a REST service with the status of the answer and the time it
	 * took, so a demo can show which calls reached the live services.
	 */
	@Bean
	RestClientHttpServiceGroupConfigurer upstreamRequestLogging() {
		ClientHttpRequestInterceptor logging = (request, body, execution) -> {
			long started = System.nanoTime();
			ClientHttpResponse response = execution.execute(request, body);
			logger.info("REST %s %s answered %d (%d ms)".formatted(request.getMethod(), request.getURI(),
					response.getStatusCode().value(), Duration.ofNanos(System.nanoTime() - started).toMillis()));
			return response;
		};
		return (groups) -> groups.forEachClient((group, builder) -> builder.requestInterceptor(logging));
	}

}
