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

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * The animal and veterinary adverse event endpoint of openFDA, which publishes the
 * reports that the FDA's Center for Veterinary Medicine receives about animal drugs. The
 * same endpoint searches the reports and, with the <code>count</code> parameter, ranks
 * the values of one field across the matching reports. A search without a match answers
 * <code>404 Not Found</code>.
 */
@HttpExchange(url = "/animalandveterinary/event.json", accept = MediaType.APPLICATION_JSON_VALUE)
interface OpenFdaApi {

	/**
	 * Searches the reports.
	 * @param query the parameters <code>search</code>, <code>sort</code>,
	 * <code>limit</code> and <code>skip</code>
	 */
	@GetExchange
	EventSearch events(@RequestParam MultiValueMap<String, String> query);

	/**
	 * Counts the values of one field across the matching reports, most frequent first.
	 * @param query the parameters <code>search</code>, <code>count</code> and
	 * <code>limit</code>
	 */
	@GetExchange
	CountResult counts(@RequestParam MultiValueMap<String, String> query);

	/**
	 * The answer of a search.
	 *
	 * @param meta the number of matches and the day of the last update
	 * @param results the reports
	 */
	record EventSearch(Meta meta, List<OpenFdaAnimalAdverseEvent> results) {

	}

	record Meta(Results results) {

	}

	/**
	 * @param total the number of reports that match, across every page
	 */
	record Results(int total) {

	}

	record CountResult(List<OpenFdaCount> results) {

	}

	/**
	 * Registers the interface as an HTTP service in the group <code>openfda</code>, whose
	 * base URL and timeouts come from <code>spring.http.serviceclient.openfda.*</code>.
	 */
	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(group = "openfda", types = OpenFdaApi.class)
	class Registration {

		/**
		 * Sends the API key, when one is set, as the user name of basic authentication,
		 * which openFDA accepts in place of the <code>api_key</code> parameter. The key
		 * then stays out of the URLs that the request log shows. Without a key, openFDA
		 * allows 1,000 requests a day for each IP address.
		 */
		@Bean
		RestClientHttpServiceGroupConfigurer openFdaApiKey(
				@Value("${petclinic.upstream.openfda.api-key:}") String key) {
			return (groups) -> groups.filterByName("openfda").forEachClient((group, builder) -> {
				if (!key.isBlank()) {
					builder.defaultHeaders((headers) -> headers.setBasicAuth(key, ""));
				}
			});
		}

	}

}
