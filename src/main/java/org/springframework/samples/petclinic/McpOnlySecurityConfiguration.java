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
package org.springframework.samples.petclinic;

import io.gatool.boot.mcp.security.McpServerSecurityConfigurer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Secures the MCP endpoint alone.
 * <p>
 * Once Spring Security is on the classpath, GATool secures the MCP endpoint and every
 * other path with a bearer token. With this chain of the application's own in place,
 * GATool's chains step back. Every path outside the MCP endpoint, such as the web pages,
 * the chat and /graphql, then belongs to no chain and stays open, the way Petclinic ran
 * before.
 */
@Configuration(proxyBeanMethods = false)
public class McpOnlySecurityConfiguration {

	@Bean
	SecurityFilterChain mcpEndpoint(HttpSecurity http) throws Exception {
		return http.securityMatcher("/mcp", "/.well-known/oauth-protected-resource/mcp")
			.with(McpServerSecurityConfigurer.mcpServer(), withDefaults())
			.build();
	}

}
