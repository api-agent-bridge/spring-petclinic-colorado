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

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Starts a local server that answers in place of the external services when the
 * <code>mock</code> profile is active. The answers are responses recorded from the real
 * services, kept in <code>src/main/resources/mock-upstreams</code>.
 * <p>
 * The clients are the same in both modes. Only the URLs they call differ, see
 * <code>application-mock.properties</code>.
 */
@Configuration(proxyBeanMethods = false)
@Profile("mock")
class MockUpstreamConfiguration {

	private static final Log logger = LogFactory.getLog(MockUpstreamConfiguration.class);

	@Bean(initMethod = "start", destroyMethod = "stop")
	WireMockServer mockUpstreams(@Value("${petclinic.mock.port}") int port) {
		logger.info("External services are mocked with recorded responses on port " + port);
		return new WireMockServer(
				WireMockConfiguration.options().port(port).usingFilesUnderClasspath("mock-upstreams"));
	}

}
