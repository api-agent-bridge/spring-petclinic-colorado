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

/**
 * Thrown when an external service fails to answer, or answers with an error. The GraphQL
 * response then carries an error for the affected field and keeps the rest of the data.
 */
public class UpstreamException extends RuntimeException {

	private final String upstream;

	public UpstreamException(String upstream, String message) {
		super(message);
		this.upstream = upstream;
	}

	public UpstreamException(String upstream, Throwable cause) {
		super(cause.getMessage(), cause);
		this.upstream = upstream;
	}

	/**
	 * The name of the external service, in words a client can read.
	 */
	public String getUpstream() {
		return this.upstream;
	}

}
