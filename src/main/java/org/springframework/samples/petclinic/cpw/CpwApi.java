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
package org.springframework.samples.petclinic.cpw;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * The species data of Colorado Parks and Wildlife (CPW), one ArcGIS Online feature
 * service with a layer for each species and activity, such as
 * <code>Black Bear Human Conflict
 * Area</code>. ArcGIS answers an error with HTTP status 200 and an <code>error</code>
 * object in the body.
 */
@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE)
interface CpwApi {

	/**
	 * Returns the description of the service, with the id and name of every layer.
	 */
	@GetExchange("?f=json")
	Service service();

	/**
	 * Asks several layers at once which of their features match a filter. The form holds
	 * <code>layerDefs</code>, which lists the layers, and the spatial filter. The list of
	 * all layers is too long for a URL, so the query goes as a <code>POST</code>.
	 */
	@PostExchange(url = "/query", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	QueryResult query(@RequestBody MultiValueMap<String, String> form);

	/**
	 * The description of the service.
	 *
	 * @param layers its layers
	 * @param error the error, when the service could not answer
	 */
	record Service(List<Layer> layers, Error error) {

	}

	/**
	 * A layer of the service.
	 *
	 * @param id the id, from 0
	 * @param name the name, which says the species and the kind of area
	 */
	record Layer(int id, String name) {

	}

	/**
	 * The answer of a query across layers. It lists every layer that was asked, the empty
	 * ones included.
	 *
	 * @param layers the features of each layer
	 * @param error the error, when the service could not answer
	 */
	record QueryResult(List<LayerFeatures> layers, Error error) {

	}

	/**
	 * The features of one layer that matched.
	 *
	 * @param id the id of the layer
	 * @param features the features, each with the fields the query asked for
	 */
	record LayerFeatures(int id, List<Feature> features) {

	}

	record Feature(Map<String, Object> attributes) {

	}

	record Error(int code, String message) {

	}

	/**
	 * Registers the interface as an HTTP service in the group <code>cpw</code>, whose
	 * base URL and timeouts come from <code>spring.http.serviceclient.cpw.*</code>.
	 */
	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(group = "cpw", types = CpwApi.class)
	class Registration {

	}

}
