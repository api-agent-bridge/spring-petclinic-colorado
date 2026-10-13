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
package org.springframework.samples.petclinic.graphql;

import java.util.Locale;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import graphql.schema.idl.RuntimeWiring;

import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.stereotype.Component;

/**
 * Registers the <code>Long</code> scalar of the schema, a whole number of 64 bits, for
 * the counts and keys of the external services that can pass the 2,147,483,647 where the
 * 32 bits of <code>Int</code> stop. The schema file holds the description of the scalar.
 */
@Component
class LongScalar implements RuntimeWiringConfigurer {

	private static final GraphQLScalarType LONG = GraphQLScalarType.newScalar()
		.name("Long")
		.coercing(new LongCoercing())
		.build();

	@Override
	public void configure(RuntimeWiring.Builder builder) {
		builder.scalar(LONG);
	}

	private static final class LongCoercing implements Coercing<Long, Long> {

		@Override
		public Long serialize(Object dataFetcherResult, GraphQLContext context, Locale locale) {
			if (dataFetcherResult instanceof Number number && number.doubleValue() == number.longValue()) {
				return number.longValue();
			}
			throw new CoercingSerializeException("Expected a whole number but got " + dataFetcherResult);
		}

		@Override
		public Long parseValue(Object input, GraphQLContext context, Locale locale) {
			if (input instanceof Number number && number.doubleValue() == number.longValue()) {
				return number.longValue();
			}
			try {
				return Long.parseLong(String.valueOf(input));
			}
			catch (NumberFormatException ex) {
				throw new CoercingParseValueException("Expected a whole number but got " + input, ex);
			}
		}

		@Override
		public Long parseLiteral(Value<?> input, CoercedVariables variables, GraphQLContext context, Locale locale) {
			try {
				if (input instanceof IntValue number) {
					return number.getValue().longValueExact();
				}
				if (input instanceof StringValue text) {
					return Long.parseLong(text.getValue());
				}
			}
			catch (ArithmeticException | NumberFormatException ex) {
				throw new CoercingParseLiteralException("Expected a whole number of 64 bits but got " + input, ex);
			}
			throw new CoercingParseLiteralException("Expected a whole number but got " + input);
		}

	}

}
