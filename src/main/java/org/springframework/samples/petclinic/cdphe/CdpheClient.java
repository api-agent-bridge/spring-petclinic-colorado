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
package org.springframework.samples.petclinic.cdphe;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.webservices.client.WebServiceMessageSenderFactory;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.samples.petclinic.cdphe.AnimalDiseaseCount.AnimalDisease;
import org.springframework.samples.petclinic.cdphe.AnimalDiseaseCount.CaseGrouping;
import org.springframework.samples.petclinic.cdphe.PositiveAnimal.SpecimenType;
import org.springframework.samples.petclinic.cdphe.PositiveAnimal.ZoonoticDisease;
import org.springframework.samples.petclinic.upstream.UpstreamException;
import org.springframework.stereotype.Component;
import org.springframework.ws.WebServiceException;
import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * Reads the animal disease data of the Colorado Department of Public Health and
 * Environment (CDPHE) from the department's ArcGIS Server, over SOAP:
 * <ul>
 * <li><code>QueryFeatureData</code> on the layer of the animals that tested positive in
 * 2017 for rabies, plague, tularemia or West Nile virus.</li>
 * <li><code>QueryData</code> on the reportable disease dataset, a standalone table, for
 * the yearly number of animals with rabies, plague or tularemia. A table takes a
 * <code>StandaloneTableDescription</code> where a layer takes a <code>LayerID</code>, and
 * the server answers <code>QueryFeatureData</code> on a table with a fault.</li>
 * </ul>
 * Each map's WSDL declares 55 operations in 290 KB, and this client needs one of each, so
 * it writes the requests by hand and reads the answers with DOM.
 */
@Component
class CdpheClient {

	static final String ANIMALS = "The CDPHE map of animals that tested positive";

	static final String CASES = "The CDPHE reportable disease dataset";

	private static final Log logger = LogFactory.getLog(CdpheClient.class);

	/**
	 * The namespace the WSDL declares. The server, at version 11.3, also accepts
	 * <code>ArcGIS/11.3</code>, and answers in whichever namespace the request used.
	 */
	private static final String NAMESPACE = "http://www.esri.com/schemas/ArcGIS/3.3.0";

	private static final String ANIMAL_FIELDS = "OBJECTID,Specimen_Type,Species,Collected,County,Zipcode,Longitude,"
			+ "Latitude,Disease";

	private static final String CASE_FIELDS = "diagnosis,onset_year,metric,category,total,population,rate_per_100k";

	private final WebServiceTemplate soap;

	private final String animalsUrl;

	private final int animalsLayerId;

	private final String casesUrl;

	private final int casesTableId;

	private final String mapName;

	/**
	 * Builds the template with the builder Spring Boot configures, which picks the HTTP
	 * client from the classpath.
	 */
	CdpheClient(WebServiceTemplateBuilder builder, @Value("${petclinic.upstream.cdphe.animals-url}") String animalsUrl,
			@Value("${petclinic.upstream.cdphe.animals-layer-id}") int animalsLayerId,
			@Value("${petclinic.upstream.cdphe.cases-url}") String casesUrl,
			@Value("${petclinic.upstream.cdphe.cases-table-id}") int casesTableId,
			@Value("${petclinic.upstream.cdphe.map-name}") String mapName,
			@Value("${petclinic.upstream.connect-timeout}") Duration connectTimeout,
			@Value("${petclinic.upstream.read-timeout}") Duration readTimeout) {
		this.animalsUrl = animalsUrl;
		this.animalsLayerId = animalsLayerId;
		this.casesUrl = casesUrl;
		this.casesTableId = casesTableId;
		this.mapName = mapName;
		HttpClientSettings timeouts = HttpClientSettings.defaults()
			.withConnectTimeout(connectTimeout)
			.withReadTimeout(readTimeout);
		// ArcGIS Server sends SOAP faults with HTTP status 200. By default the template
		// looks for a fault only when the status is 500, and would hand the fault back as
		// if it were a result. With checkConnectionForFault off, it inspects every
		// response body.
		this.soap = builder.httpMessageSenderFactory(WebServiceMessageSenderFactory.http(timeouts))
			.setCheckConnectionForFault(false)
			.build();
	}

	/**
	 * Returns the animals that tested positive and match every filter given, in the
	 * server's order.
	 * @param disease only animals with this disease, or null for every disease
	 * @param specimenType only animals of this group, or null for every group
	 * @param species text the species contains, in upper or lower case, or blank for
	 * every species
	 * @param county the county, in upper or lower case, or blank for every county
	 */
	List<PositiveAnimal> animalsTestedPositive(ZoonoticDisease disease, SpecimenType specimenType, String species,
			String county) {
		List<String> conditions = new ArrayList<>();
		if (disease != null) {
			conditions.add("Disease = " + literal(disease.value()));
		}
		if (specimenType != null) {
			// the source writes 'Domestic Animal ' with a trailing space on 7 of its 9
			// domestic animals, so an exact match would miss them
			conditions.add("Specimen_Type LIKE " + literal(likePattern(specimenType.value()) + "%") + " ESCAPE '\\'");
		}
		if (isGiven(species)) {
			conditions.add("UPPER(Species) LIKE " + literal("%" + likePattern(upper(species)) + "%") + " ESCAPE '\\'");
		}
		if (isGiven(county)) {
			conditions.add("UPPER(County) = " + literal(upper(county)));
		}
		String where = conditions.isEmpty() ? "1=1" : String.join(" AND ", conditions);
		long started = System.nanoTime();
		List<Map<String, String>> records = readRecords(ANIMALS,
				call(ANIMALS, this.animalsUrl, queryFeatureData(where)));
		logger.info("SOAP QueryFeatureData at %s returned %d animals for %s (%d ms)".formatted(this.animalsUrl,
				records.size(), where, millisSince(started)));
		return records.stream().map(PositiveAnimal::fromRecord).toList();
	}

	/**
	 * Returns the yearly numbers of animals that tested positive, in the server's order.
	 * @param disease only this disease, or null for rabies, plague and tularemia
	 * @param groupedBy whether the counts are per county, for the state or per month
	 * @param county the county, in upper or lower case, or blank for every county
	 * @param fromYear the first year, or null for the first year of the dataset
	 * @param toYear the last year, or null for the last year of the dataset
	 */
	List<AnimalDiseaseCount> animalDiseaseCounts(AnimalDisease disease, CaseGrouping groupedBy, String county,
			Integer fromYear, Integer toYear) {
		List<String> conditions = new ArrayList<>();
		List<AnimalDisease> diseases = (disease != null) ? List.of(disease) : List.of(AnimalDisease.values());
		conditions.add("diagnosis IN ("
				+ String.join(",", diseases.stream().map((each) -> literal(each.value())).toList()) + ")");
		conditions.add("metric = " + literal(groupedBy.value()));
		if (isGiven(county)) {
			conditions.add("UPPER(category) = " + literal(upper(county)));
		}
		if (fromYear != null) {
			conditions.add("onset_year >= " + fromYear);
		}
		if (toYear != null) {
			conditions.add("onset_year <= " + toYear);
		}
		String where = String.join(" AND ", conditions);
		long started = System.nanoTime();
		List<Map<String, String>> records = readRecords(CASES, call(CASES, this.casesUrl, queryData(where)));
		logger.info("SOAP QueryData at %s returned %d counts for %s (%d ms)".formatted(this.casesUrl, records.size(),
				where, millisSince(started)));
		return records.stream().map(AnimalDiseaseCount::fromRecord).toList();
	}

	private static boolean isGiven(String text) {
		return text != null && !text.isBlank();
	}

	private static String upper(String text) {
		return text.strip().toUpperCase(Locale.ROOT);
	}

	/**
	 * Writes a text as an SQL string literal, with each quote doubled, so the text cannot
	 * end the literal and add to the WHERE clause.
	 */
	static String literal(String text) {
		return "'" + text.replace("'", "''") + "'";
	}

	/**
	 * Escapes the characters that LIKE reads as wildcards, with the escape character the
	 * conditions declare.
	 */
	static String likePattern(String text) {
		return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static long millisSince(long startedNanos) {
		return Duration.ofNanos(System.nanoTime() - startedNanos).toMillis();
	}

	private Document call(String upstream, String url, Document payload) {
		DOMResult response = new DOMResult();
		try {
			this.soap.sendSourceAndReceiveToResult(url, new DOMSource(payload), response);
		}
		catch (WebServiceException ex) {
			// covers SOAP faults, timeouts and connection failures
			throw new UpstreamException(upstream, ex);
		}
		return (Document) response.getNode();
	}

	/**
	 * Builds the payload of a <code>QueryFeatureData</code> request for the rows of the
	 * layer that match a WHERE clause.
	 */
	private Document queryFeatureData(String where) {
		Document document = newDocument();
		Element request = root(document, "QueryFeatureData");
		appendText(request, "MapName", this.mapName);
		appendText(request, "LayerID", String.valueOf(this.animalsLayerId));
		Element filter = typed(request, "QueryFilter", "e:QueryFilter");
		appendText(filter, "SubFields", ANIMAL_FIELDS);
		appendText(filter, "WhereClause", where);
		return document;
	}

	/**
	 * Builds the payload of a <code>QueryData</code> request for the rows of the
	 * standalone table that match a WHERE clause.
	 */
	private Document queryData(String where) {
		Document document = newDocument();
		Element request = root(document, "QueryData");
		appendText(request, "MapName", this.mapName);
		Element table = typed(request, "MapTableDescription", "e:StandaloneTableDescription");
		appendText(table, "ID", String.valueOf(this.casesTableId));
		Element filter = typed(request, "QueryFilter", "e:QueryFilter");
		appendText(filter, "SubFields", CASE_FIELDS);
		appendText(filter, "WhereClause", where);
		appendText(typed(request, "QueryResultOptions", "e:QueryResultOptions"), "Format",
				"esriQueryResultRecordSetAsObject");
		return document;
	}

	/**
	 * Reads the <code>RecordSet</code> of an answer. It lists the fields first and then
	 * gives each record as values in the same order. A value the server marks with
	 * <code>xsi:nil</code>, or sends empty, becomes null.
	 */
	private static List<Map<String, String>> readRecords(String upstream, Document response) {
		List<String> fieldNames = new ArrayList<>();
		NodeList fields = response.getElementsByTagName("Field");
		for (int i = 0; i < fields.getLength(); i++) {
			fieldNames.add(childElements(fields.item(i), "Name").get(0).getTextContent());
		}
		List<Map<String, String>> records = new ArrayList<>();
		NodeList recordElements = response.getElementsByTagName("Record");
		for (int i = 0; i < recordElements.getLength(); i++) {
			List<Element> values = childElements(childElements(recordElements.item(i), "Values").get(0), "Value");
			if (values.size() != fieldNames.size()) {
				throw new UpstreamException(upstream,
						"a record has " + values.size() + " values for " + fieldNames.size() + " fields");
			}
			Map<String, String> record = new LinkedHashMap<>();
			for (int field = 0; field < values.size(); field++) {
				String text = values.get(field).getTextContent();
				record.put(fieldNames.get(field), text.isEmpty() ? null : text);
			}
			records.add(record);
		}
		return records;
	}

	private static Element root(Document document, String operation) {
		Element request = document.createElementNS(NAMESPACE, "e:" + operation);
		// the xsi:type values below refer to the "e" prefix, so both prefixes are
		// declared here explicitly
		request.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:e", NAMESPACE);
		request.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:xsi",
				XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
		document.appendChild(request);
		return request;
	}

	private static Element typed(Element parent, String name, String type) {
		Element child = parent.getOwnerDocument().createElement(name);
		child.setAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi:type", type);
		parent.appendChild(child);
		return child;
	}

	private static List<Element> childElements(Node parent, String name) {
		List<Element> children = new ArrayList<>();
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element element && name.equals(element.getTagName())) {
				children.add(element);
			}
		}
		return children;
	}

	private static Element appendText(Element parent, String name, String text) {
		Element child = parent.getOwnerDocument().createElement(name);
		child.setTextContent(text);
		parent.appendChild(child);
		return child;
	}

	private static Document newDocument() {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			return factory.newDocumentBuilder().newDocument();
		}
		catch (ParserConfigurationException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
