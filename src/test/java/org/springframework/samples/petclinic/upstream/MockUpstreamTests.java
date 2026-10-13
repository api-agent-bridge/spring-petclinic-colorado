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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.TestSocketUtils;

/**
 * Runs the application with the <code>mock</code> profile, for tests that query the
 * external services through GraphQL. The answers come from the recorded responses in
 * <code>src/main/resources/mock-upstreams</code>. Each service has a test class of its
 * own that extends this one, and they all share one application context.
 */
@SpringBootTest
@AutoConfigureGraphQlTester
@ActiveProfiles("mock")
public abstract class MockUpstreamTests {

	// the port is read twice, by the mock server and by the URLs of the clients, so it is
	// chosen once for every test class
	private static final int PORT = TestSocketUtils.findAvailableTcpPort();

	@Autowired
	protected GraphQlTester graphQl;

	@Autowired
	protected WireMockServer mockUpstreams;

	@DynamicPropertySource
	static void mockPort(DynamicPropertyRegistry registry) {
		registry.add("petclinic.mock.port", () -> PORT);
	}

}
