# Public APIs behind the GraphQL schema

This catalogue lists public APIs that cover the Denver area, so the demo works with local data. [In the GraphQL schema](#in-the-graphql-schema) says how the Petclinic schema reads them. APIs about pets, animals and vets come first: services of the City and County of Denver, of the State of Colorado, and a few national services that cover Colorado. Five more Colorado APIs with live data follow.

Every API here is free, and every one answers in English.

## Overview

### Pets, animals and vets

| API | Owner | Protocol | Auth | Documentation | Pet clinic link |
| --- | --- | --- | --- | --- | --- |
| [Denver dog parks](#denver-dog-parks) | City and County of Denver | ArcGIS REST | Open | Esri REST reference, layer metadata | Where an owner can take a dog off leash |
| [Denver 311 animal cases](#denver-311-animal-cases) | City and County of Denver | ArcGIS REST | Open | Esri REST reference, layer metadata | Barking dogs, loose animals and bites near an owner |
| [Denver kennel licenses](#denver-kennel-licenses) | City and County of Denver | ArcGIS REST | Open | Esri REST reference, layer metadata | Licensed kennels, including clinics that board animals |
| [Colorado veterinary licenses](#colorado-veterinary-licenses) | Department of Regulatory Agencies (DORA) | Socrata SODA and OData v4 | Open | Socrata API page per dataset | Whether a vet holds a valid license, and any discipline |
| [Colorado shelter and rescue statistics](#colorado-shelter-and-rescue-statistics) | Department of Agriculture (PACFA) | Socrata SODA | Open | Socrata API page per dataset | Intake, adoptions and euthanasia per shelter and species |
| [Colorado animal disease](#colorado-animal-disease) | Department of Public Health and Environment (CDPHE) | SOAP and ArcGIS REST | Open | WSDL, Esri reference | Rabies, plague and tularemia in animals, per county |
| [Colorado wildlife ranges](#colorado-wildlife-ranges) | Colorado Parks and Wildlife (CPW) | ArcGIS REST | Open | Layer names | Bears, mountain lions and rattlesnakes at an owner's address |
| [National parks in Colorado](#national-parks-in-colorado) | National Park Service | REST | Free key | Swagger 2.0 | Which park activities allow pets |
| [iNaturalist](#inaturalist) | iNaturalist | REST | Open for reads | Swagger 2.0 | Animal sightings in Colorado |
| [openFDA animal and veterinary events](#openfda-animal-and-veterinary-events) | US Food and Drug Administration | REST | Open, key optional | Field reference in YAML | Adverse reactions to veterinary drugs |

### Other Colorado APIs with live data

| API | Owner | Protocol | Auth | Documentation | Updates |
| --- | --- | --- | --- | --- | --- |
| [National Weather Service](#national-weather-service) | NOAA | REST | Open, `User-Agent` required | OpenAPI 3.1 | Live |
| [Snowpack (SNOTEL)](#snowpack-snotel) | USDA NRCS | SOAP and REST | Open | WSDL, OpenAPI 3.1 | Daily |
| [USGS earthquakes](#usgs-earthquakes) | US Geological Survey | REST (FDSN) | Open | FDSN reference, WADL | Live |
| [Colorado water](#colorado-water) | Division of Water Resources (DWR) | REST | Open, key optional | Help pages | Every 15 minutes |
| [Alternative fuel stations](#alternative-fuel-stations) | National Laboratory of the Rockies (NLR) | REST | Free key | Docs site | Daily |

SOAP comes from two services, CDPHE and NRCS, and both serve the same data over REST as well.

## In the GraphQL schema

The schema reads every API in this catalogue, in 185 root fields of `Query` and the field `Owner.nearestDogParks`. The README lists the fields of each service. Each field name starts with its service, and the fields of each service are declared in a schema file of their own in `src/main/resources/graphql`.

| Service | Root fields | How they were made |
| --- | --- | --- |
| Petclinic | 5 | By hand |
| Denver open data and the Census geocoder | 5, and `Owner.nearestDogParks` | By hand |
| data.colorado.gov: veterinary licenses and shelter reports | 3 | By hand |
| CDPHE animal disease, over SOAP | 2 | By hand |
| Colorado Parks and Wildlife, openFDA, USGS earthquakes, NLR fuel stations | 7 | By hand |
| National Weather Service | 54 | Generated from its OpenAPI document |
| NRCS Air and Water Database (snowpack) | 4 | Generated from its OpenAPI document |
| iNaturalist | 33 | Generated from its Swagger file |
| National Park Service | 11 | Generated from its Swagger file |
| Colorado Division of Water Resources | 61 | Generated from its HTML help pages |

A hand-written REST service is an HTTP interface of Spring Framework (`@HttpExchange`), registered with `@ImportHttpServices` in a group of its own, and Spring Boot reads the base URL and the timeouts of a group from `spring.http.serviceclient.<group>.*`. CDPHE is a SOAP client on Spring Web Services that writes its requests by hand. A generated service has too many operations for an interface method each, so one `RestOperationFetcher` per field reads the request from an operation table, such as `nws/operations.json`.

The generated fields leave out the operations that answer something other than JSON (images, map tiles, PDF and XML downloads) and the ones that need a logged-in user.

The `mock` profile points every base URL at responses recorded from the live services on 12 October 2026, in `src/main/resources/mock-upstreams`. The recordings replace the names of private people, such as license holders and iNaturalist members.

## Denver dog parks

The City and County of Denver publishes its open data on ArcGIS Online. The dog parks layer holds Denver's 18 off-leash dog parks as polygons, with whether each one is fenced, shaded and has water.

- **Endpoint:** `https://services1.arcgis.com/zdB7qR0BtYrg0Xpl/arcgis/rest/services/ODC_PARK_DOGPARKS_A/FeatureServer/84`
- **Docs:** the layer describes itself at the endpoint with `?f=json`. Esri documents the query operation in the [ArcGIS REST API reference](https://developers.arcgis.com/rest/services-reference/enterprise/query-feature-service-layer/), and Denver describes the data on its [open data page](https://opendata-geospatialdenver.hub.arcgis.com/datasets/geospatialDenver::dog-parks).
- **Auth:** open.
- **Last edited:** 7 August 2026, according to the layer's `editingInfo`.

| Field | Meaning | Values seen |
| --- | --- | --- |
| `LOCATION` | Park name | `Railyard Dog Park` |
| `FENCED` | Fenced | `Yes` for all 18 |
| `SHADE` | Shade | `Yes` (14), `No` (2), `null` (2) |
| `WATER` | Drinking water | `Yes` (6), `No` (6), `null` (6) |
| `GIS_ACRES` | Area in acres | `0.56666776` |
| `YEAR_BUILT` | Year built | `2004` to `2025` |
| `ASSET_CONDITION` | Condition | `Good` (13), `Excellent` (1), `100` (2), `80` (2) |

### From an address to the nearest dog parks

The layer does not take an address. The US Census Bureau runs a free geocoder that turns an address into a point, and a distance query on the layer then finds the parks near that point.

- **Geocoder:** `GET https://geocoding.geo.census.gov/geocoder/locations/onelineaddress`
- **Docs:** [Census Geocoder API](https://geocoding.geo.census.gov/geocoder/Geocoding_Services_API.html)
- **Auth:** open.

| Step | Call |
| --- | --- |
| Address to coordinates | Census `onelineaddress` |
| Parks within a distance of a point | `query` with `geometry`, `distance` and `units` |

```
GET https://geocoding.geo.census.gov/geocoder/locations/onelineaddress?address=1437 Bannock St, Denver, CO 80202&benchmark=Public_AR_Current&format=json
```

```json
{
  "result": {
    "addressMatches": [
      {
        "matchedAddress": "1437 BANNOCK ST, DENVER, CO, 80202",
        "coordinates": { "x": -104.990311950936, "y": 39.739040016914 },
        "addressComponents": { "fromAddress": "1401", "toAddress": "1499", "streetName": "BANNOCK", "zip": "80202" }
      }
    ]
  }
}
```

```
GET .../FeatureServer/84/query?geometry=-104.990312,39.739040&geometryType=esriGeometryPoint&inSR=4326&spatialRel=esriSpatialRelIntersects&distance=2&units=esriSRUnit_StatuteMile&outFields=LOCATION,FENCED,SHADE,WATER,GIS_ACRES,YEAR_BUILT,ASSET_CONDITION&returnCentroid=true&outSR=4326&f=json
```

The query returned 9 parks within 2 miles of the City and County Building. This is one of them:

```json
{
  "attributes": {
    "LOCATION": "Railyard Dog Park",
    "FENCED": "Yes",
    "SHADE": "Yes",
    "WATER": "Yes",
    "GIS_ACRES": 0.56666776,
    "YEAR_BUILT": 2010,
    "ASSET_CONDITION": "Good"
  },
  "centroid": { "x": -105.001305, "y": 39.758211 }
}
```

The nearest is Skyline Park, about 1.2 km away, measured to the centre of the park.

### Things to know

- `ASSET_CONDITION` mixes words and scores. Thirteen parks say `Good` and one says `Excellent`, while four parks, among them the ones built in 2023 and 2025, hold a number (`80` or `100`) as a string.
- The layer stores its shapes in Colorado Central State Plane, in US survey feet (EPSG:2877). A request with `outSR=4326` gets latitude and longitude.
- A distance query returns the parks in the server's own order. The client computes the distance to each park and sorts them.
- The Census geocoder places the point by interpolating along an address range (1401 to 1499 Bannock St), so the point is an estimate.
- The geocoder answers an address it cannot find with status `200` and an empty `addressMatches` list.

## Denver 311 animal cases

Denver's 311 table holds every service request of the last 12 months: 395,257 cases created between 7 October 2025 and 6 October 2026. Denver Animal Protection's cases are part of it.

- **Endpoint:** `https://services1.arcgis.com/zdB7qR0BtYrg0Xpl/arcgis/rest/services/ODC_service_requests_311/FeatureServer/53`
- **Docs:** the Esri REST reference above and Denver's [open data page](https://opendata-geospatialdenver.hub.arcgis.com/datasets/geospatialDenver::311-service-requests).
- **Auth:** open.
- **Last edited:** 7 October 2026.

Those 12 months hold 7,013 animal cases. By `Case_Summary`:

| Case summary | Cases |
| --- | --- |
| `Animal Adoption/Hold Inquiry` | 3,686 |
| `Barking Dog` | 1,157 |
| `Loose Animal` | 582 |
| `Holding Stray Animal` | 513 |
| `Denver Animal Protection 720-913-2080` | 386 |
| `Animal Excrement` | 228 |
| `Loose Dog (With owner)` | 153 |
| `Injured Domestic Animal` | 114 |
| `Dead Animal` | 53 |
| `Too many animals`, `Dog Parks` | 34 each |
| `Animal Bite (non human)` | 20 |
| `Breeding Animals / Service Animals` | 19 |
| `Animal in Vehicle`, `Non-Service Animal Complaint` | 12 each |
| `Animal Complaint` | 10 |

```
GET .../FeatureServer/53/query?where=Case_Summary IN ('Barking Dog','Loose Animal','Animal Bite (non human)')&outFields=Case_Summary,Case_Status,Case_Source,Case_Created_Date,Case_Created_dttm,Case_Closed_dttm,Incident_Address_1,Incident_Zip_Code,Latitude,Longitude,Agency,Council_District&orderByFields=Case_Created_Date DESC&resultRecordCount=2&f=json
```

```json
{
  "attributes": {
    "Case_Summary": "Barking Dog",
    "Case_Status": "Closed - Service Completed",
    "Case_Source": "Phone",
    "Case_Created_Date": 1791158400000,
    "Case_Created_dttm": "10/5/2026 9:30:02 AM",
    "Case_Closed_dttm": "10/5/2026 11:02:22 AM",
    "Incident_Address_1": "511 N Lowell Blvd",
    "Incident_Zip_Code": "80204",
    "Latitude": 39.72484106,
    "Longitude": -105.0354088,
    "Agency": "Public Health & Environment",
    "Council_District": 3
  }
}
```

### Things to know

- The service publishes this data as a table without geometry, with the position in the `Latitude` and `Longitude` fields. Cases near an address need a box of coordinates in the `where` clause, because the `geometry` parameter only works on a layer with shapes.
- `Case_Created_Date` holds the day at midnight UTC. The time of day is only in `Case_Created_dttm`, as text in US format.
- 5,376 of the 7,013 animal cases come without coordinates or an address. Of those, 4,064 were closed as `Closed - Answer Provided` and 1,124 were passed to another agency, like the `Loose Animal` case created the same morning as the one above.
- Animal cases are filed under four agencies: `Public Health & Environment` (6,369), `Safety` (386, all of them `Denver Animal Protection 720-913-2080`), `311` (217) and `Parks & Recreation` (41).
- `Division`, `Type` and `Topic` are empty for all but one of the 27,629 cases of `Public Health & Environment`.
- The window moves: a case leaves this table after 12 months. Denver publishes each earlier year, back to 2007, as a dataset of its own.

## Denver kennel licenses

Denver's table of active business licenses holds 74 kennel licenses. Animal hospitals that board animals hold one too, so the table links clinics to boarding.

- **Endpoint:** `https://services1.arcgis.com/zdB7qR0BtYrg0Xpl/arcgis/rest/services/ODC_active_business_licenses/FeatureServer/42`
- **Docs:** the Esri REST reference above and Denver's [open data page](https://opendata-geospatialdenver.hub.arcgis.com/datasets/geospatialDenver::active-business-licenses).
- **Auth:** open.
- **Last edited:** 8 October 2026.

```
GET .../FeatureServer/42/query?where=LICENSE_TYPE='Kennel'&outFields=*&resultRecordCount=2&f=json
```

```json
{
  "attributes": {
    "BFN": "2001-BFN-63777",
    "LICENSE_TYPE": "Kennel",
    "LICENSE_SUB_TYPE": "NA",
    "LICENSE_STATUS": "License Issued - Active",
    "ENTITY_NAME": "TENDER TOUCH ANIMAL HOSPITAL",
    "TRADE_NAME": "TENDER TOUCH",
    "Expiration_Date": 1823644800000,
    "ADDRESS": "350 KALAMATH ST ",
    "ZIP": "80223",
    "X_COORD": 3140732,
    "Y_COORD": 1688199,
    "AddressNoUnit": "350 KALAMATH ST"
  }
}
```

### Things to know

- The position is in Colorado Central State Plane feet (`X_COORD`, `Y_COORD`), without latitude and longitude. The two fields are plain numbers, which `outSR` leaves as they are, so the client converts them itself.
- `LICENSE_SUB_TYPE` holds the text `NA` for all 74 kennels.
- `ADDRESS` ends with a space. `AddressNoUnit` holds the same address without it.
- The same table holds Denver's other license types, among them 2,584 retail food licenses, 243 marijuana stores and 3 natural medicine healing centres.

## Colorado veterinary licenses

The Department of Regulatory Agencies publishes its register of professional licenses on the state's open data portal, data.colorado.gov, which runs on Socrata (now Tyler Data & Insights). The register holds 1,612,188 rows for every regulated profession, including veterinarians and veterinary technicians, with each license's status, expiry date and disciplinary actions.

- **SODA 2.1:** `GET https://data.colorado.gov/resource/7s5z-vewr.json`
- **SODA 3.0:** `GET https://data.colorado.gov/api/v3/views/7s5z-vewr/query.json?query=SELECT ...`
- **OData v4:** `GET https://data.colorado.gov/api/odata/v4/7s5z-vewr`
- **Docs:** the [API page of the dataset](https://dev.socrata.com/foundry/data.colorado.gov/7s5z-vewr), Socrata's [endpoint](https://dev.socrata.com/docs/endpoints.html) and [query](https://dev.socrata.com/docs/queries/) references, and the [dataset page](https://data.colorado.gov/d/7s5z-vewr).
- **Auth:** open. An app token raises the rate limits.
- **License types:** a second dataset, `349y-twqi`, names every type, with its category and renewal cycle.

| Type | Name | Rows | Rows with status `Active` |
| --- | --- | --- | --- |
| `VET` | Veterinarian | 14,043 | 6,294 |
| `VT` | Veterinarian Technician | 4,491 | 4,172 |
| `AV` | Academic Veterinarian | 891 | 265 |
| `PVT` | Veterinarian Technician Provisional | 740 | 350 |
| `VPA` | Veterinary Professional Associate | 0 | 0 |

The other `VET` statuses include `Expired` (7,403), `Revoked` (86) and `Suspended` (11).

### Sample request over SODA

```
GET https://data.colorado.gov/resource/7s5z-vewr.json?$where=licensetype='VET' AND casenumber IS NOT NULL&$order=disciplineeffectivedate DESC&$limit=1
```

```json
[
  {
    "lastname": "(replaced)",
    "firstname": "(replaced)",
    "city": "Superior",
    "state": "CO",
    "licensetype": "VET",
    "licensenumber": "9620",
    "licensefirstissuedate": "2012-06-08T00:00:00.000",
    "licenselastreneweddate": "2024-11-01T00:00:00.000",
    "licenseexpirationdate": "2026-10-31T00:00:00.000",
    "licensestatusdescription": "Active",
    "degrees": "~Doctor of Veterinary Medicine",
    "casenumber": "2019-6811",
    "programaction": "CLS Letter of Admonition",
    "disciplineeffectivedate": "12/31/2019",
    "linktoverifylicense": { "url": "https://www.colorado.gov/dora/licensing/Lookup/PrintLicenseDetails.aspx?cred=907689&contact=964913" }
  }
]
```

### The same register over OData

```
GET https://data.colorado.gov/api/odata/v4/7s5z-vewr?$filter=licensetype eq 'VET' and city eq 'Denver'&$select=city,licensenumber,licensestatusdescription,licenseexpirationdate&$top=1
```

```json
{
  "@odata.context": "https://data.colorado.gov/api/odata/v4/$metadata#7s5z-vewr",
  "value": [
    {
      "@odata.id": "https://data.colorado.gov/api/odata/v4/7s5z-vewr('row-fzug_uu8u~9h7q')",
      "city": "Denver",
      "licensenumber": "9457",
      "licenseexpirationdate": "2028-10-31T00:00:00.000",
      "licensestatusdescription": "Active"
    }
  ]
}
```

### Things to know

- A license appears once for each disciplinary action. The 14,043 `VET` rows hold 13,611 license numbers, and one license appears in 17 rows.
- `disciplineeffectivedate` is text in US format (`12/31/2019`), while the other dates are ISO timestamps.
- `licenselastreneweddate` can lie in the future: in October 2026, one Denver vet's license gave its last renewal as `2026-11-01`.
- Veterinary licenses expire on 31 October of even years, so many live statuses change at the end of October in those years.
- The metadata gives the expected update frequency as `Never`. Every row has the same `:created_at`, `2026-10-08T16:01:34.996Z`, so the state reloaded the whole register that day.
- `degrees` starts with a tilde (`~Doctor of Veterinary Medicine`).
- The SODA 3.0 documentation says a query needs a user or an app token. A query without either still answered with `200`.
- The OData `$metadata` document describes the whole portal: 1.9 MB, with 717 entity types.
- The register holds the names of real people, so the mock fixtures have the names replaced.

## Colorado shelter and rescue statistics

The Pet Animal Care Facilities Act (PACFA) program of the Colorado Department of Agriculture requires every licensed shelter and rescue to report its animals each year. The state publishes one dataset per year, with one row per facility and the number of animals taken in and sent out, by species.

- **Endpoint (2025):** `GET https://data.colorado.gov/resource/wngd-z7w6.json`
- **Docs:** the [API page of the dataset](https://dev.socrata.com/foundry/data.colorado.gov/wngd-z7w6) and the [dataset page](https://data.colorado.gov/d/wngd-z7w6). The column list is in `https://data.colorado.gov/api/views/wngd-z7w6.json`.
- **Auth:** open.
- **Published:** 22 May 2026, for the calendar year 2025.

| Year | Dataset | Columns |
| --- | --- | --- |
| 2025 | `wngd-z7w6` | 154 |
| 2024 | `timh-w9dy` | 154 |
| 2023 | `4crp-zpa9` | 154 |
| 2022 | `trsh-h2mc` | 154 |
| 2021 | `bn35-a8hy` | 154 |
| 2020 | `uwsx-t9fx` | 154 |
| 2019 | `wzw2-jncq` | 154 |
| 2018 | `ii6j-8pkn` | 173 |
| 2017 | `uhi6-hddy` | 188 |
| 2016 | `m8vm-brgw` | 209 |
| 2015 | `773d-wy2e` | 186 |

The 2025 dataset holds 371 facilities. Across all of them, 32,978 adult dogs and 24,371 adult cats were adopted, and 5,830 adult dogs were euthanised. Each of the nine animal groups has the same 17 columns: adult dogs, juvenile dogs, adult cats, juvenile cats, birds, small mammals, reptiles and amphibians, rabbits, and other.

```
GET https://data.colorado.gov/resource/wngd-z7w6.json?facility_name=Denver Animal Protection
```

The answer has 154 fields. These are the first ones:

```json
{
  "facility_name": "Denver Animal Protection",
  "adult_dogs_in_shelter": "64",
  "adult_dogs_in_foster_care": "10",
  "adult_dogs_stray": "2886",
  "adult_dogs_owner_relinquished": "1620",
  "adult_dogs_transfer_from": "14",
  "adult_dogs_transfer_from_1": "0",
  "adult_dogs_other": "321",
  "adult_dogs_adoption": "1928",
  "adult_dogs_returned_to_owner": "1466",
  "adult_dogs_transfer_to_another": "181",
  "adult_dogs_transfer_to_out": "0",
  "adult_dogs_other_1": "0",
  "adult_dogs_deaths": "14",
  "adult_dogs_euthanasia": "1214",
  "adult_dogs_missing_stolen": "1",
  "adult_dogs_in_shelter_1": "101",
  "adult_dogs_in_foster_care_1": "10"
}
```

### Things to know

- A type that mirrors a row has 154 fields, which makes this dataset an example of an oversized response.
- Socrata cut the column names and numbered the repeats. `adult_dogs_transfer_from` counts transfers from another Colorado organisation and `adult_dogs_transfer_from_1` transfers from out of state, as the display names say. The display names of `adult_dogs_in_shelter_1` and `adult_dogs_other_1` only add a `1`. Their position suggests the count at the end of the year and other outcomes, and the dataset does not confirm either.
- The display names contain a line break: `Adult Dogs\nIn Shelter`.
- The columns are typed as numbers, and the JSON carries them as strings (`"2886"`).
- A row names the facility, without an address or a license number, so linking a shelter to a place means matching names.
- Each year is a new dataset with a new id, and the years before 2019 have different columns.

## Colorado animal disease

The Colorado Department of Public Health and Environment runs its own ArcGIS Server, version 11.3, which serves every map over REST and over SOAP. Two of its services cover animal disease.

| Service | What it holds |
| --- | --- |
| `DCEED/ZOONOTIC_SUMMARY_POSITIVE_ANIMALS_2017`, layer `0` | 341 animals and mosquito pools that tested positive in 2017 for rabies, plague, tularemia or West Nile virus, each with species, county and coordinates |
| `OPEN_DATA/cdphe_reportable_disease_dataset`, table `1` | 72,374 rows of yearly case counts from 2016 to 2025, including `Rabies, Animal`, `Plague, Animal` and `Tularemia, Animal` per county |

- **SOAP endpoint:** `https://www.cohealthmaps.dphe.state.co.us/arcgis/services/{folder}/{service}/MapServer`
- **WSDL:** the same URL with `?wsdl`. It is 290 KB and declares 55 operations, in Esri's standard schema.
- **REST endpoint:** `https://www.cohealthmaps.dphe.state.co.us/arcgis/rest/services/{folder}/{service}/MapServer/{id}`
- **Docs:** the WSDL, Esri's archived [ArcObjects reference for `QueryFeatureData`](https://desktop.arcgis.com/en/arcobjects/latest/net/IMapServer_QueryFeatureData.htm), the [ArcGIS REST reference for map layers](https://developers.arcgis.com/rest/services-reference/enterprise/query-map-service-layer/), and the state's [page for the reportable disease dataset](https://geodata.colorado.gov/datasets/CDPHE::cdphe-colorado-reportable-disease-dataset).
- **Auth:** open.

### Useful operations

| Operation | What it does |
| --- | --- |
| `GetDefaultMapName` | Returns the map name every other call needs (`Layers`) |
| `QueryFeatureCount` | Counts the rows of a layer matching a `WhereClause`, such as 341 for the zoonotic layer |
| `QueryFeatureData` | Returns the matching rows of a layer as a `RecordSet` |
| `QueryRowCount` | Counts the rows of a standalone table, such as 72,374 for the disease table |
| `QueryData` | Returns the matching rows of a standalone table |

### Sample SOAP request on the zoonotic layer

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xmlns:e="http://www.esri.com/schemas/ArcGIS/3.3.0">
  <soap:Body>
    <e:QueryFeatureData>
      <MapName>Layers</MapName>
      <LayerID>0</LayerID>
      <QueryFilter xsi:type="e:QueryFilter">
        <SubFields>Species,County,Disease,Specimen_Type</SubFields>
        <WhereClause>Specimen_Type LIKE 'Domestic%'</WhereClause>
      </QueryFilter>
    </e:QueryFeatureData>
  </soap:Body>
</soap:Envelope>
```

The answer lists the fields first, then gives each record as positional values.

```xml
<tns:QueryFeatureDataResponse>
  <Result xsi:type="tns:RecordSet">
    <Fields xsi:type="tns:Fields">
      <FieldArray xsi:type="tns:ArrayOfField">
        <Field xsi:type="tns:Field"><Name>OBJECTID</Name><Type>esriFieldTypeOID</Type>...</Field>
        <Field xsi:type="tns:Field"><Name>Species</Name><Type>esriFieldTypeString</Type>...</Field>
        ...
      </FieldArray>
    </Fields>
    <Records xsi:type="tns:ArrayOfRecord">
      <Record xsi:type="tns:Record">
        <Values xsi:type="tns:ArrayOfValue">
          <Value xsi:type="xsd:int">155</Value>
          <Value xsi:type="xsd:string">Cat</Value>
          <Value xsi:type="xsd:string">Larimer</Value>
          <Value xsi:type="xsd:string">Plague</Value>
          <Value xsi:type="xsd:string">Domestic Animal </Value>
        </Values>
      </Record>
      ...
    </Records>
  </Result>
</tns:QueryFeatureDataResponse>
```

The nine domestic animals were six cats with plague (Larimer, Boulder, La Plata, Weld, Jefferson and San Miguel counties), one cat with tularemia (Garfield) and two dogs with rabies (Weld and Yuma).

### Sample SOAP request on the disease table

A standalone table takes `QueryData`, with the table named by a `StandaloneTableDescription`:

```xml
<e:QueryData>
  <MapName>Layers</MapName>
  <MapTableDescription xsi:type="e:StandaloneTableDescription"><ID>1</ID></MapTableDescription>
  <QueryFilter xsi:type="e:QueryFilter">
    <SubFields>diagnosis,onset_year,category,total,rate_per_100k</SubFields>
    <WhereClause>diagnosis IN ('Rabies, Animal','Plague, Animal','Tularemia, Animal') AND metric = 'county' AND category = 'Denver' AND onset_year &gt;= 2023</WhereClause>
  </QueryFilter>
  <QueryResultOptions xsi:type="e:QueryResultOptions"><Format>esriQueryResultRecordSetAsObject</Format></QueryResultOptions>
</e:QueryData>
```

| Diagnosis | 2023 | 2024 | 2025 |
| --- | --- | --- | --- |
| `Rabies, Animal` | 2 | 6 | 5 |
| `Tularemia, Animal` | 0 | 2 | 0 |
| `Plague, Animal` | 0 | 0 | 0 |

Across Colorado, 69 animals tested positive for rabies in 2025, most of them in Jefferson (11), Larimer (8) and Boulder (7) counties.

### The same table over REST

```
GET .../OPEN_DATA/cdphe_reportable_disease_dataset/MapServer/1/query?where=diagnosis='Rabies, Animal' AND onset_year=2025 AND metric='county' AND category='Denver'&outFields=diagnosis,onset_year,metric,category,total,population,rate_per_100k&f=json
```

```json
{
  "attributes": {
    "diagnosis": "Rabies, Animal",
    "onset_year": 2025,
    "metric": "county",
    "category": "Denver",
    "total": "5",
    "population": "737680",
    "rate_per_100k": "0.677800672378267"
  }
}
```

### Things to know

- The WSDL declares the namespace `http://www.esri.com/schemas/ArcGIS/3.3.0`. The server also accepts `ArcGIS/11.3`, and it answers in whichever namespace the request used.
- `QueryFeatureData` and `QueryFeatureCount` with the table's id as `LayerID` return a SOAP fault, `Error processing server request`, with an empty `detail`. A wrong `MapName` returns the same fault. Both come with HTTP status `200`.
- `Specimen_Type` is `'Domestic Animal '`, with a trailing space, for 7 of the 9 domestic animals, and `'Domestic Animal'` for the other 2. An exact match misses one group or the other.
- `total` and `rate_per_100k` are strings. A suppressed count is `**` and a missing one is `NA`.
- The rate for animal diagnoses is per 100,000 people: Denver's 5 rabid animals against its 737,680 residents.
- Animal diagnoses also have rows for the human metrics, such as `age group`, all with `NA`.
- The zoonotic layer covers 2017 only. The disease table runs to 2025.

## Colorado wildlife ranges

Colorado Parks and Wildlife publishes where its species live as one feature service with 313 layers, one for each species and activity: `Black Bear Human Conflict Area`, `Mountain Lion Human Conflict Area`, `Prairie Rattlesnake And Western Rattlesnake Overall Range`, `Elk Highway Crossings` and so on.

- **Endpoint:** `https://services5.arcgis.com/ttNGmDvKQA7oeDQ3/arcgis/rest/services/CPWSpeciesData/FeatureServer`
- **Docs:** the Esri REST reference, and the state's [page for the service](https://geodata.colorado.gov/maps/CPW::cpwspeciesdata). The layers have names, and their descriptions are empty.
- **Auth:** open.
- **Catalogue date:** 7 May 2026.

One request at the level of the service asks every layer at once whether it contains a point. `layerDefs` lists all 313 layers, so the request goes as a `POST`.

```
POST .../CPWSpeciesData/FeatureServer/query
layerDefs=[{"layerId":0,"where":"1=1","outFields":"ACTIVITYCO"}, ... ,{"layerId":312,...}]
geometry=-105.2705,40.0150&geometryType=esriGeometryPoint&inSR=4326&spatialRel=esriSpatialRelIntersects&returnGeometry=false&f=json
```

| Point | Layers containing it | Among them | Size and time |
| --- | --- | --- | --- |
| Boulder, `-105.2705,40.0150` | 60 | Black bear and mountain lion human conflict areas, prairie rattlesnake range | 81 KB, 7.1 s |
| Denver City and County Building | 58 | Prairie rattlesnake range, Canada geese foraging area, peregrine falcon nesting area | 79 KB, 5.2 s |

```json
{
  "layers": [
    { "id": 20, "features": [ { "attributes": { "ACTIVITYCO": "BBHCA" } } ] },
    { "id": 93, "features": [ { "attributes": { "ACTIVITYCO": "MLHCA" } } ] },
    ...
  ]
}
```

### Things to know

- A feature carries only an activity code, such as `BBHCA`. The meaning is in the name of its layer, so the client needs the layer list as well. `Lynx Habitat` has a `null` code.
- The answer lists all 313 layers, the empty ones included. For the Boulder point, 253 of them were empty, and the answer still came to 81 KB.
- A layer answers up to 10,000 features per request (`maxRecordCount`).

## National parks in Colorado

The National Park Service publishes its parks, alerts, activities, campgrounds and webcams through one API. With `stateCode=CO` it returns 17 units, among them Rocky Mountain, Mesa Verde, Great Sand Dunes and Black Canyon of the Gunnison national parks. The things to do in a park say whether pets are permitted.

- **Endpoint:** `https://developer.nps.gov/api/v1/`
- **Docs:** [API documentation](https://www.nps.gov/subjects/developer/api-documentation.htm), a Swagger UI over a Swagger 2.0 file at `https://www.nps.gov/subjects/developer/customcf/swagger.json`, with 29 GET operations.
- **Auth:** a free key from api.data.gov, sent as `api_key` or in the `X-Api-Key` header. `DEMO_KEY` works for trying it.

```
GET https://developer.nps.gov/api/v1/thingstodo?parkCode=romo&api_key=DEMO_KEY
```

Rocky Mountain National Park lists 12 things to do: pets are permitted for 5 and not for 7.

```json
{
  "total": "12",
  "data": [
    {
      "title": "Enjoy a Scenic Drive on Trail Ridge Road",
      "arePetsPermitted": "true",
      "arePetsPermittedWithRestrictions": "true",
      "petsDescription": "Pets and owners have a special bond. But a national park is not always the best place for pets. Rocky Mountain National Park is a designated natural area, ..."
    }
  ]
}
```

### Things to know

- Booleans and counts are strings: `"true"`, `"12"`.
- `stateCode=CO` also returns national historic trails that cross Colorado, such as the California and Pony Express trails.
- `DEMO_KEY` answered with `x-ratelimit-limit: 10`.
- On 8 October the same request returned `503` with the body `upstream connect error or disconnect/reset before headers. reset reason: connection timeout`. On 9 October it answered normally.

## iNaturalist

iNaturalist collects observations of plants and animals from the public. Its API serves observations, species counts and places, and Colorado is place `34` (Denver County is `205`).

- **Endpoint:** `https://api.inaturalist.org/v1/`
- **Docs:** [API documentation](https://api.inaturalist.org/v1/docs/), a Swagger UI over the Swagger 2.0 file at `https://api.inaturalist.org/v1/swagger.json`, with 54 GET operations. Some of them return map tiles, and some need a logged-in user.
- **Auth:** open for reads. Writes need a JSON Web Token.
- **Limits:** the Swagger file says "we throttle API usage to a max of 100 requests per minute, though we ask that you try to keep it to 60 requests per minute or lower, and to keep under 10,000 requests per day."

With 54 operations described in a Swagger file, iNaturalist suits generated fields.

```
GET https://api.inaturalist.org/v1/observations/species_counts?place_id=34&iconic_taxa=Mammalia&d1=2026-09-01&per_page=5
```

```json
{
  "total_results": 86,
  "results": [
    { "count": 344, "taxon": { "name": "Odocoileus hemionus", "preferred_common_name": "Mule Deer" } },
    { "count": 228, "taxon": { "name": "Cervus canadensis", "preferred_common_name": "Wapiti" } },
    { "count": 219, "taxon": { "name": "Sciurus niger", "preferred_common_name": "Eastern Fox Squirrel" } },
    { "count": 170, "taxon": { "name": "Cynomys ludovicianus", "preferred_common_name": "Black-tailed Prairie Dog" } },
    { "count": 155, "taxon": { "name": "Alces alces", "preferred_common_name": "Moose" } }
  ]
}
```

```
GET https://api.inaturalist.org/v1/observations?place_id=34&taxon_name=Canis familiaris&order_by=observed_on&per_page=1
```

Colorado has 971 observations of domestic dogs. The most recent:

```json
{
  "id": 404030517,
  "observed_on": "2026-09-27",
  "quality_grade": "casual",
  "captive": true,
  "obscured": false,
  "license_code": "cc-by-nc",
  "taxon": { "id": 47144, "name": "Canis familiaris", "preferred_common_name": "Domestic Dog" }
}
```

### Things to know

- iNaturalist grades observations of pets and other captive animals as `casual`, so a search for research-grade observations leaves them out.
- An observation carries the observer's login and, unless the observer obscured it, exact coordinates and a street-level `place_guess`. The mock fixtures need these replaced.
- The counts are live and change between two calls. The mule deer count went from 338 to 344 between 8 and 9 October.

## openFDA animal and veterinary events

The FDA's Center for Veterinary Medicine publishes the adverse event reports it receives for animal drugs. The API covers the United States as a whole.

- **Endpoint:** `GET https://api.fda.gov/animalandveterinary/event.json`
- **Docs:** [the endpoint](https://open.fda.gov/apis/animalandveterinary/event/), its [searchable fields](https://open.fda.gov/apis/animalandveterinary/event/searchable-fields/), and the field reference in YAML at `https://open.fda.gov/fields/animalandveterinaryevent.yaml`.
- **Auth:** open. A free key raises the daily limit.
- **Limits:** [240 requests a minute](https://open.fda.gov/apis/authentication/), and 1,000 a day per IP address without a key or 120,000 a day with one.
- **Last updated:** 5 October 2026, according to `meta.last_updated`.

The data holds 1,001,380 reports about dogs. The `count` parameter turns the search into a ranking:

```
GET https://api.fda.gov/animalandveterinary/event.json?search=animal.species:Dog AND animal.breed.breed_component:"Retriever - Labrador"&count=reaction.veddra_term_name.exact&limit=5
```

```json
{
  "results": [
    { "term": "Vomiting", "count": 21601 },
    { "term": "INEFFECTIVE, HEARTWORM LARVAE", "count": 16342 },
    { "term": "Lack of efficacy (endoparasite) - heartworm", "count": 11291 },
    { "term": "Diarrhoea", "count": 7886 },
    { "term": "Lack of efficacy (endoparasite) - hookworm", "count": 7251 }
  ]
}
```

The most recent dog report, trimmed:

```json
{
  "unique_aer_id_number": "USA-USFDACVM-2026-US-035636",
  "original_receive_date": "20260626",
  "serious_ae": "true",
  "animal": {
    "species": "Dog",
    "gender": "Female",
    "reproductive_status": "Neutered",
    "age": { "min": "1.00", "unit": "Year" },
    "weight": { "min": "24.494", "unit": "Kilogram" },
    "breed": { "is_crossbred": "true", "breed_component": "Retriever - Labrador" }
  },
  "drug": [
    {
      "brand_name": "MSK",
      "manufacturer": { "name": "MSK" },
      "dosage_form": "Tablet, chewable",
      "route": "Oral",
      "atc_vet_code": "QP54AB52",
      "active_ingredients": [ { "name": "Pyrantel Pamoate;Sarolaner" } ]
    }
  ],
  "reaction": [ { "veddra_term_name": "Lack of efficacy (endoparasite) - heartworm" } ],
  "outcome": [ { "medical_status": "Ongoing" } ]
}
```

### Things to know

- Every one of the 1,001,380 dog reports has the brand name and the manufacturer masked as `MSK`. The drug can be identified by its active ingredients and ATC code only.
- The reactions mix two vocabularies: older terms in capitals (`INEFFECTIVE, HEARTWORM LARVAE`) and current VeDDRA terms (`Lack of efficacy (endoparasite) - heartworm`) for the same problem.
- A search with zero matches returns `404` with `"message": "No matches found!"`. A client that treats `404` as an error turns an empty result into a failure.
- Dates are strings without separators (`20260626`), and numbers are strings too.
- `receiver` is the FDA office in Rockville, Maryland. The reports do not say which state the animal lives in, so the API cannot filter on Colorado.

## National Weather Service

The National Weather Service API serves forecasts, observations and alerts. The Boulder office (`BOU`) forecasts for Denver.

- **Endpoint:** `https://api.weather.gov/`
- **Docs:** [API documentation](https://www.weather.gov/documentation/services-web-api) and the OpenAPI 3.1 file at `https://api.weather.gov/openapi.json`, with 69 GET operations.
- **Auth:** open, with a `User-Agent` header that identifies the application. A request without one returns `403`.

A forecast takes two calls: the first turns a point into a grid square, and the second reads the forecast of that square.

```
GET https://api.weather.gov/points/39.7390,-104.9903
```

```json
{
  "properties": {
    "gridId": "BOU",
    "gridX": 63,
    "gridY": 61,
    "forecast": "https://api.weather.gov/gridpoints/BOU/63,61/forecast",
    "forecastZone": "https://api.weather.gov/zones/forecast/COZ040",
    "county": "https://api.weather.gov/zones/county/COC031",
    "timeZone": "America/Denver",
    "relativeLocation": { "properties": { "city": "Denver", "state": "CO" } }
  }
}
```

```
GET https://api.weather.gov/gridpoints/BOU/63,61/forecast
```

The first period of the forecast:

```json
{
  "name": "Tonight",
  "startTime": "2026-10-08T18:00:00-06:00",
  "temperature": 55,
  "temperatureUnit": "F",
  "windSpeed": "7 mph",
  "shortForecast": "Partly Cloudy",
  "probabilityOfPrecipitation": { "unitCode": "wmoUnit:percent", "value": 4 }
}
```

`GET https://api.weather.gov/alerts/active?area=CO` returned one alert, a frost advisory for the San Luis Valley.

## Snowpack (SNOTEL)

The Natural Resources Conservation Service runs automated snow stations in the Colorado mountains (SNOTEL), which report snow depth, snow water equivalent and temperature every day. The Air and Water Database (AWDB) serves the same data over SOAP and over REST.

- **REST endpoint:** `https://wcc.sc.egov.usda.gov/awdbRestApi/services/v1/` with four operations: `stations`, `data`, `forecasts` and `reference-data`.
- **REST docs:** a [Swagger UI](https://wcc.sc.egov.usda.gov/awdbRestApi/swagger-ui/index.html) over the OpenAPI 3.1 file at `https://wcc.sc.egov.usda.gov/awdbRestApi/v3/api-docs`.
- **SOAP endpoint:** `https://wcc.sc.egov.usda.gov/awdbWebService/services`, with the WSDL at `?WSDL`. It is 92 KB and declares 38 operations.
- **SOAP docs:** the WSDL, and the [AWDB page of TSTool](https://opencdss.state.co.us/tstool/latest/doc-user/datastore-ref/NRCS-AWDB/NRCS-AWDB/), the State of Colorado's time series tool, which links the user guide. TSTool says "The SOAP API will be decommissioned by the NRCS in the future."
- **Auth:** open.

| Operation | Protocol |
| --- | --- |
| `getStations` for `CO` and network `SNTL` | SOAP |
| `getData` for one station, element and date range | SOAP |
| `stations?stationTriplets=*:CO:SNTL` | REST |
| `data?stationTriplets=335:CO:SNTL&elements=TOBS&duration=DAILY` | REST |

### The same readings over both protocols

Station `335:CO:SNTL` is Berthoud Summit, at 11,300 ft. Its air temperature over SOAP follows. The `q` prefix stands for the target namespace of the WSDL, which is `www.wcc.nrcs.usda.gov/ns/awdbWebService` with the `http` scheme. The build's nohttp check refuses that URL in the repository, so the sample shows `...` in its place.

```xml
<q:getData xmlns:q="...">
  <stationTriplets>335:CO:SNTL</stationTriplets>
  <elementCd>TOBS</elementCd>
  <ordinal>1</ordinal>
  <duration>DAILY</duration>
  <getFlags>false</getFlags>
  <beginDate>2026-09-28</beginDate>
  <endDate>2026-10-09</endDate>
  <alwaysReturnDailyFeb29>false</alwaysReturnDailyFeb29>
</q:getData>
```

```xml
<return>
  <beginDate>2026-09-28 00:00:00</beginDate>
  <duration>DAILY</duration>
  <endDate>2026-10-08 00:00:00</endDate>
  <stationTriplet>335:CO:SNTL</stationTriplet>
  <values>37.940</values>
  <values>37.940</values>
  <values>39.380</values>
  ...
  <values>37.760</values>
</return>
```

And over REST:

```
GET https://wcc.sc.egov.usda.gov/awdbRestApi/services/v1/data?stationTriplets=335:CO:SNTL&elements=TOBS&duration=DAILY&beginDate=2026-09-28&endDate=2026-10-09
```

```json
[
  {
    "stationTriplet": "335:CO:SNTL",
    "data": [
      {
        "stationElement": { "elementCode": "TOBS", "durationName": "DAILY", "storedUnitCode": "degF", "dataPrecision": 1 },
        "values": [
          { "date": "2026-09-28", "value": 37.9 },
          { "date": "2026-09-29", "value": 39.4 },
          { "date": "2026-09-30", "value": 33.8 }
        ]
      }
    ]
  }
]
```

### Things to know

- By default the two protocols date the same reading one day apart. SOAP, like the NRCS website, dates a reading by the start of the day: 39.38 °F on 30 September. REST defaults to `periodRef=END` and dates the same reading 29 September. With `periodRef=START`, REST matches SOAP. The OpenAPI file documents the parameter, and the WSDL does not mention it.
- SOAP returns the values without dates, as a list that starts at `beginDate`. A client counts days to place each value.
- SOAP returns the stored value (`37.940`), and REST rounds it to the element's `dataPrecision` (`37.9`).
- SOAP `getStations` lists 120 Colorado stations, and REST lists 118, because REST defaults to `activeOnly=true` and leaves out the two stations closed in 2013 and 2022.
- REST ignores parameters it does not know. `stations?stateCds=CO&networkCds=SNTL`, with the parameter names of SOAP, returns all 4,398 stations of every state and network, 1.6 MB, with status `200`.
- All 118 Colorado stations report `dataTimeZone` `-8.0`, Pacific Standard Time.
- An active station has the `endDate` `2100-01-01 00:00`.
- Daily data lags by a day or more: on 9 October, the latest REST value was for 7 October.

## USGS earthquakes

The US Geological Survey publishes earthquakes worldwide through the standard FDSN event service. The National Earthquake Information Center, which runs it, is in Golden, Colorado.

- **Endpoint:** `GET https://earthquake.usgs.gov/fdsnws/event/1/query`, with `count` for the number of matches only
- **Docs:** [FDSN event web service](https://earthquake.usgs.gov/fdsnws/event/1/) and the WADL at `https://earthquake.usgs.gov/fdsnws/event/1/application.wadl`
- **Auth:** open.

```
GET https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=2026-07-01&minlatitude=36.99&maxlatitude=41.0&minlongitude=-109.06&maxlongitude=-102.04&orderby=time
```

The box around Colorado returned 47 earthquakes since 1 July 2026, and `count` for the whole year returned `{"count":113,"maxAllowed":20000}`. The most recent:

```json
{
  "properties": {
    "mag": 1.8,
    "place": "28 km WSW of Yampa, Colorado",
    "time": 1789541213706,
    "status": "reviewed",
    "tsunami": 0,
    "sig": 50,
    "type": "earthquake",
    "title": "M 1.8 - 28 km WSW of Yampa, Colorado"
  },
  "geometry": { "type": "Point", "coordinates": [-107.2273, 40.0738, 5] },
  "id": "us7000thsk"
}
```

### Things to know

- The service filters by a box of latitudes and longitudes. Colorado's borders follow lines of latitude and longitude closely, so the box fits the state.
- A query with zero results returns `200` with an empty list of features, also with `nodata=404` in GeoJSON.
- `time` and `updated` are milliseconds since 1970.

## Colorado water

The Colorado Division of Water Resources publishes water rights, wells, diversions and live river flows through one REST service. Its telemetry stations report every 15 minutes.

- **Endpoint:** `https://dwr.state.co.us/Rest/GET/api/v2/`, with 61 GET operations
- **Docs:** [help pages](https://dwr.state.co.us/Rest/GET/Help), with a page per operation
- **Auth:** open. Anonymous users may make 1,000 calls and fetch 600,000 rows a day. A free key from the DWR portal raises the limits.
- **Formats:** `format=json`, `xml`, `csv`, `tsv` and, for some data, `geojson`. Pages hold up to 50,000 rows by default (`pageSize`, `pageIndex`).

```
GET https://dwr.state.co.us/Rest/GET/api/v2/telemetrystations/telemetrystation/?format=json&county=DENVER
```

```json
{
  "PageNumber": 1,
  "PageCount": 1,
  "ResultCount": 8,
  "ResultDateTime": "2026-10-08T21:35:45.0079541-06:00",
  "ResultList": [
    {
      "abbrev": "PLADENCO",
      "stationName": "SOUTH PLATTE RIVER AT DENVER",
      "parameter": "DISCHRG",
      "measDateTime": "2026-10-08T20:45:00-06:00",
      "measValue": 81.2,
      "units": "CFS",
      "stationStatus": "Active",
      "latitude": 39.759456,
      "longitude": -105.004003
    }
  ]
}
```

```
GET https://dwr.state.co.us/Rest/GET/api/v2/telemetrystations/telemetrytimeseriesraw/?format=json&abbrev=PLADENCO&parameter=DISCHRG&startDate=10/08/2026 18:00
```

```json
{ "abbrev": "PLADENCO", "parameter": "DISCHRG", "measDateTime": "2026-10-08T18:00:00", "measValue": 81.2, "measUnit": "cfs", "flagA": "O", "modified": "2026-10-08T19:00:13.977" }
```

### Things to know

- The station list writes times with an offset (`-06:00`), and the time series writes them without one.
- The station list writes the unit as `CFS`, and the time series as `cfs`.
- `stationStatus` `Active` does not mean the station reports. `EPPERSON DITCH PUMP` is active, and its last measurement is from 31 May 2024.
- Dates in the query take the US format, `10/08/2026 18:00`.

## Alternative fuel stations

The national laboratory in Golden, Colorado, renamed from the National Renewable Energy Laboratory (NREL) to the National Laboratory of the Rockies (NLR), publishes energy APIs. The alternative fuel station locator lists electric chargers and other fuel stations across the United States.

- **Endpoint:** `GET https://developer.nlr.gov/api/alt-fuel-stations/v1/nearest.json`, among others
- **Docs:** [Alternative Fuel Stations API](https://developer.nlr.gov/docs/transportation/alt-fuel-stations-v1/) and the [developer site](https://developer.nlr.gov/docs/).
- **Auth:** a free key from [developer.nlr.gov/signup](https://developer.nlr.gov/signup/). `DEMO_KEY` works for trying it.
- **Limits:** [1,000 requests an hour](https://developer.nlr.gov/docs/rate-limits/) with a key. The page gives `DEMO_KEY` 30 an hour and 50 a day, and the response header said `x-ratelimit-limit: 10`.

```
GET https://developer.nlr.gov/api/alt-fuel-stations/v1/nearest.json?api_key=DEMO_KEY&latitude=39.739040&longitude=-104.990312&radius=1&fuel_type=ELEC&limit=1
```

```json
{
  "total_results": 120,
  "fuel_stations": [
    {
      "station_name": "CCD CHARGING CITY HALL 1",
      "street_address": "201-249 W 14th Ave",
      "city": "Denver",
      "ev_network": "ChargePoint Network",
      "ev_level2_evse_num": 2,
      "ev_dc_fast_num": null,
      "access_days_time": "24 hours daily",
      "distance": 0.03828,
      "date_last_confirmed": "2026-10-09"
    }
  ]
}
```

### Things to know

- The old domain, `developer.nrel.gov`, timed out in October 2026. Code and examples that still use it need the new domain.
- `distance` is in miles.
