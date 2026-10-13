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
package org.springframework.samples.petclinic.openfda;

/**
 * One value of a field and the number of reports that have it, as the
 * <code>OpenFdaCount</code> type of the GraphQL schema.
 *
 * @param term the value, for example <code>Vomiting</code>
 * @param count the number of matching reports with that value
 */
record OpenFdaCount(String term, long count) {

}
