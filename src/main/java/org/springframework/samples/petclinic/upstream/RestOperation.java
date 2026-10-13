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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One read operation of a REST service, which the schema exposes as a root field of the
 * same shape. The arguments of the field become the path variables and the query
 * parameters of the operation, and the answer of the service goes to the client as the
 * service sent it. A generator writes these operations from the OpenAPI document of the
 * service, together with the root fields and their types in the schema.
 *
 * @param field the root field on <code>Query</code>
 * @param path the path of the operation, with its path variables in braces
 * @param arguments the arguments of the field that the operation takes
 * @param paging how the <code>page</code> and <code>size</code> arguments reach the
 * service. Null for a field without them.
 * @param result how the answer is reshaped for the schema, or null when the schema takes
 * it as it is
 */
public record RestOperation(String field, String path, List<Argument> arguments, Paging paging, Result result) {

	public RestOperation {
		arguments = (arguments != null) ? arguments : List.of();
	}

	/**
	 * An argument of the field, and the parameter of the operation it fills.
	 *
	 * @param name the name of the argument in the schema
	 * @param parameter the name of the parameter in the operation, which can differ when
	 * the service uses a name that GraphQL does not allow, such as
	 * <code>nucleotideSequence.targetGene</code>
	 * @param in whether the parameter is a path variable or a query parameter
	 * @param list whether the argument is a list, which the query repeats for each value
	 */
	public record Argument(String name, String parameter, Location in, boolean list) {

	}

	/**
	 * Where a parameter goes in the request.
	 */
	public enum Location {

		@JsonProperty("path")
		PATH,

		@JsonProperty("query")
		QUERY

	}

	/**
	 * How the <code>page</code> and <code>size</code> arguments of the field reach the
	 * service, which names its paging parameters in its own way.
	 *
	 * @param style how the service counts where a page starts
	 * @param start the query parameter that takes where the page starts, for example
	 * <code>offset</code> or <code>pageIndex</code>. Null for {@link Style#LIMIT}.
	 * @param size the query parameter that takes the number of results on a page, for
	 * example <code>limit</code> or <code>per_page</code>
	 */
	public record Paging(Style style, String start, String size) {

		/**
		 * How a service counts where a page starts.
		 */
		public enum Style {

			/**
			 * The service takes the number of results to skip, which the page and size
			 * arguments give.
			 */
			@JsonProperty("offset")
			OFFSET,

			/**
			 * The service takes the number of the page, starting at 1, as the page
			 * argument does.
			 */
			@JsonProperty("page")
			PAGE,

			/**
			 * The service takes a limit only, which the size argument fills.
			 */
			@JsonProperty("limit")
			LIMIT

		}

	}

	/**
	 * How an answer is reshaped for the schema.
	 */
	public enum Result {

		/**
		 * The service answers with an object whose keys are data, such as counts by year,
		 * and the schema has a list of entries with a key and a value.
		 */
		@JsonProperty("entries")
		ENTRIES

	}

}
