# Spring PetClinic Sample Application [![Build Status](https://github.com/spring-projects/spring-petclinic/actions/workflows/maven-build.yml/badge.svg)](https://github.com/spring-projects/spring-petclinic/actions/workflows/maven-build.yml)[![Build Status](https://github.com/spring-projects/spring-petclinic/actions/workflows/gradle-build.yml/badge.svg)](https://github.com/spring-projects/spring-petclinic/actions/workflows/gradle-build.yml)

[![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/#https://github.com/spring-projects/spring-petclinic) [![Open in GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://github.com/codespaces/new?hide_repo_select=true&ref=main&repo=7517918)

## Understanding the Spring Petclinic application with a few diagrams

See the presentation here:  
[Spring Petclinic Sample Application (legacy slides)](https://speakerdeck.com/michaelisvy/spring-petclinic-sample-application?slide=20)

> **Note:** These slides refer to a legacy, pre–Spring Boot version of Petclinic and may not reflect the current Spring Boot–based implementation.  
> For up-to-date information, please refer to this repository and its documentation.


## Run Petclinic locally

Spring Petclinic is a [Spring Boot](https://spring.io/guides/gs/spring-boot) application built using [Maven](https://spring.io/guides/gs/maven/) or [Gradle](https://spring.io/guides/gs/gradle/).
Java 17 or later is required for the build, and the application can run with Java 17 or newer.

You first need to clone the project locally:

```bash
git clone https://github.com/spring-projects/spring-petclinic.git
cd spring-petclinic
```
If you are using Maven, you can start the application on the command-line as follows:

```bash
./mvnw spring-boot:run
```
With Gradle, the command is as follows:

```bash
./gradlew bootRun
```

You can then access the Petclinic at <http://localhost:8080/>.

<img width="1042" alt="petclinic-screenshot" src="https://cloud.githubusercontent.com/assets/838318/19727082/2aee6d6c-9b8e-11e6-81fe-e889a5ddfded.png">

You can, of course, run Petclinic in your favorite IDE.
See below for more details.

## GraphQL API

The data shown on the web pages is also available through a GraphQL API at <http://localhost:8080/graphql>. Its queries read the owners, pets, visits, vets and pet types, and its mutations do what the web forms do. The schema is in `src/main/resources/graphql/schema.graphqls`.

GraphiQL, a browser client for trying queries, runs at <http://localhost:8080/graphiql>. This query returns one owner with their pets and visits:

```graphql
{
  owner(id: 6) {
    firstName
    lastName
    pets {
      name
      type { name }
      visits { date description }
    }
  }
}
```

The same query from the command line:

```bash
curl -s http://localhost:8080/graphql -H 'Content-Type: application/json' \
  -d '{"query":"{ owner(id: 6) { firstName lastName pets { name visits { date description } } } }"}'
```

The `owners` query finds owners by the start of their first and last name and returns them one page at a time. For example, `owners(firstName: "H", lastName: "Davis")` returns Harold Davis. The `pets` query works the same way with the start of a pet's name and the name of its type, so `pets(name: "Lu", type: "dog")` returns the dog called Lucky. Both queries match names in upper or lower case.

The five mutations are `addOwner`, `updateOwner`, `addPet`, `updatePet` and `addVisit`. Each one applies the rules of the matching web form and returns the object it saved. This mutation registers a pet for an owner:

```graphql
mutation {
  addPet(input: {ownerId: 6, name: "Bobbie", birthDate: "2020-05-01", typeId: 2}) {
    id
    name
    type { name }
  }
}
```

A value that breaks a rule returns a `BAD_REQUEST` error that names the field, and an id that does not exist returns `NOT_FOUND`. Sending the mutation above a second time returns `name: is already in use`, because an owner cannot have two pets with the same name.

A query or mutation may select up to 200 fields. The limit is the property `petclinic.graphql.max-query-complexity`.

### External services

The schema also reaches services outside Petclinic. Each one appears as ordinary fields, so a client cannot tell from a query which protocol sits behind it. `docs/external-apis.md` describes the services.

| Fields | Service | Protocol | How the fields were made |
| --- | --- | --- | --- |
| `Owner.nearestDogParks`, `denverDogParks`, `denverAnimalServiceRequests`, `denverAnimalServiceRequestCounts`, `denverKennelLicenses` | Open data of the City and County of Denver: dog parks, 311 animal cases, kennel licenses | ArcGIS REST | By hand |
| `censusGeocodeAddress` | US Census geocoder, also used by `nearestDogParks` | REST | By hand |
| `coloradoVeterinaryLicenses`, `coloradoShelterReports`, `coloradoShelterTotals` | data.colorado.gov: the veterinary license register and the yearly shelter reports | Socrata REST | By hand |
| `cdpheAnimalsTestedPositive`, `cdpheAnimalDiseaseCases` | Colorado Department of Public Health and Environment: animals with rabies, plague or tularemia | SOAP | By hand |
| `cpwWildlifeAt`, `cpwWildlifeLayers` | Colorado Parks and Wildlife: species ranges and human conflict areas | ArcGIS REST | By hand |
| `openFdaAnimalAdverseEvents`, `openFdaAnimalAdverseEventCounts` | openFDA: adverse drug events in animals | REST | By hand |
| `usgsEarthquakes`, `usgsEarthquakeCount` | USGS earthquake catalogue | REST | By hand |
| `nlrFuelStationsNear` | National Laboratory of the Rockies: alternative fuel stations | REST | By hand |
| 54 `nws*` fields | National Weather Service: forecasts, observations, alerts | REST | Generated from its OpenAPI document |
| 4 `awdb*` fields | NRCS Air and Water Database: snowpack and water supply forecasts | REST | Generated from its OpenAPI document |
| 33 `inat*` fields | iNaturalist: sightings of species | REST | Generated from its Swagger file |
| 11 `nps*` fields | National Park Service: parks, alerts, things to do | REST | Generated from its Swagger file |
| 61 `dwr*` fields | Colorado Division of Water Resources: river flows, water rights, wells, dams, climate stations | REST | Generated from its HTML help pages |

Each field name starts with its service. The fields of each service are declared in a schema file of their own in `src/main/resources/graphql`. A generated field is served by one fetcher that reads the request from an operation table, such as `nws/operations.json`.

`Owner.nearestDogParks` finds the owner's address with the Census geocoder, asks Denver's dog park layer for the parks around it, and returns the nearest first. The sample owner Dev2 Next lives in Denver:

```graphql
{
  owners(lastName: "Next") {
    owners {
      firstName
      address
      nearestDogParks { name distanceInMetres fenced shade water }
    }
  }
}
```

The other sample owners live in Wisconsin, so the field returns an empty list for them without calling a service.

Four services take a key from an environment variable. Each one also works without a key, up to a lower limit:

| Variable | Service | Without it |
| --- | --- | --- |
| `NPS_API_KEY` | National Park Service, a free key from api.data.gov | `DEMO_KEY`, about ten calls |
| `NLR_API_KEY` | Alternative fuel stations, a free key from developer.nlr.gov | `DEMO_KEY`, about ten calls an hour |
| `OPENFDA_API_KEY` | openFDA | 1,000 calls a day |
| `DWR_API_KEY` | Colorado Division of Water Resources | 1,000 calls a day |

The application runs in two modes:

- **Live** is the default. The fields call the real services.
- **Mock** answers from responses recorded from the real services. Start it with the `mock` profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=mock
```

In mock mode a [WireMock](https://wiremock.org/) server starts on port 9091 inside the application, and the clients call it in place of the real URLs. The client code is the same in both modes. The recordings are in `src/main/resources/mock-upstreams`: `mappings` decides which request gets which answer, and `__files` holds the answers. Edit a file there to try a different answer, for example a slow one or a SOAP fault.

<http://localhost:9091/__admin/requests> lists the calls the mock server received.

## Building a Container

There is no `Dockerfile` in this project. You can build a container image (if you have a docker daemon) using the Spring Boot build plugin:

```bash
./mvnw spring-boot:build-image
```

## Running the Container Image

```bash
docker images | grep petclinic
docker run -p 8080:8080 docker.io/library/spring-petclinic:latest
```

## In case you find a bug/suggested improvement for Spring Petclinic

Our issue tracker is available [here](https://github.com/spring-projects/spring-petclinic/issues).

## Database configuration

In its default configuration, Petclinic uses an in-memory database (H2) which
gets populated at startup with data. The h2 console is exposed at `http://localhost:8080/h2-console`,
and it is possible to inspect the content of the database using the `jdbc:h2:mem:<uuid>` URL. The UUID is printed at startup to the console.

A similar setup is provided for MySQL and PostgreSQL if a persistent database configuration is needed. Note that whenever the database type changes, the app needs to run with a different profile: `spring.profiles.active=mysql` for MySQL or `spring.profiles.active=postgres` for PostgreSQL. See the [Spring Boot documentation](https://docs.spring.io/spring-boot/how-to/properties-and-configuration.html#howto.properties-and-configuration.set-active-spring-profiles) for more detail on how to set the active profile.

You can start MySQL or PostgreSQL locally with whatever installer works for your OS or use docker:

```bash
docker run -e MYSQL_USER=petclinic -e MYSQL_PASSWORD=petclinic -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=petclinic -p 3306:3306 mysql:9.7
```

or

```bash
docker run -e POSTGRES_USER=petclinic -e POSTGRES_PASSWORD=petclinic -e POSTGRES_DB=petclinic -p 5432:5432 postgres:18.4
```

Further documentation is provided for [MySQL](https://github.com/spring-projects/spring-petclinic/blob/main/src/main/resources/db/mysql/petclinic_db_setup_mysql.txt)
and [PostgreSQL](https://github.com/spring-projects/spring-petclinic/blob/main/src/main/resources/db/postgres/petclinic_db_setup_postgres.txt).

Instead of vanilla `docker` you can also use the provided `docker-compose.yml` file to start the database containers. Each one has a service named after the Spring profile:

```bash
docker compose up mysql
```

or

```bash
docker compose up postgres
```

## Test Applications

At development time we recommend you use the test applications set up as `main()` methods in `PetClinicIntegrationTests` (using the default H2 database and also adding Spring Boot Devtools), `MySqlTestApplication` and `PostgresIntegrationTests`. These are set up so that you can run the apps in your IDE to get fast feedback and also run the same classes as integration tests against the respective database. The MySql integration tests use Testcontainers to start the database in a Docker container, and the Postgres tests use Docker Compose to do the same thing.

## Compiling the CSS

There is a `petclinic.css` in `src/main/resources/static/resources/css`. It was generated from the `petclinic.scss` source, combined with the [Bootstrap](https://getbootstrap.com/) library. If you make changes to the `scss`, or upgrade Bootstrap, you will need to re-compile the CSS resources using the Maven profile "css", i.e. `./mvnw package -P css`. There is no build profile for Gradle to compile the CSS.

## Working with Petclinic in your IDE

### Prerequisites

The following items should be installed in your system:

- Java 17 or newer (full JDK, not a JRE)
- [Git command line tool](https://help.github.com/articles/set-up-git)
- Your preferred IDE
  - Eclipse with the m2e plugin. Note: when m2e is available, there is a m2 icon in `Help -> About` dialog. If m2e is
  not there, follow the installation process [here](https://www.eclipse.org/m2e/)
  - [Spring Tools Suite](https://spring.io/tools) (STS)
  - [IntelliJ IDEA](https://www.jetbrains.com/idea/)
  - [VS Code](https://code.visualstudio.com)

### Steps

1. On the command line run:

    ```bash
    git clone https://github.com/spring-projects/spring-petclinic.git
    ```

1. Inside Eclipse or STS:

    Open the project via `File -> Import -> Maven -> Existing Maven project`, then select the root directory of the cloned repo.

    Then either build on the command line `./mvnw generate-resources` or use the Eclipse launcher (right-click on project and `Run As -> Maven install`) to generate the CSS. Run the application's main method by right-clicking on it and choosing `Run As -> Java Application`.

1. Inside IntelliJ IDEA:

    In the main menu, choose `File -> Open` and select the Petclinic [pom.xml](pom.xml). Click on the `Open` button.

    - CSS files are generated from the Maven build. You can build them on the command line `./mvnw generate-resources` or right-click on the `spring-petclinic` project then `Maven -> Generates sources and Update Folders`.

    - A run configuration named `PetClinicApplication` should have been created for you if you're using a recent Ultimate version. Otherwise, run the application by right-clicking on the `PetClinicApplication` main class and choosing `Run 'PetClinicApplication'`.

1. Navigate to the Petclinic

    Visit [http://localhost:8080](http://localhost:8080) in your browser.

## Looking for something in particular?

|Spring Boot Configuration | Class or Java property files  |
|--------------------------|---|
|The Main Class | [PetClinicApplication](https://github.com/spring-projects/spring-petclinic/blob/main/src/main/java/org/springframework/samples/petclinic/PetClinicApplication.java) |
|Properties Files | [application.properties](https://github.com/spring-projects/spring-petclinic/blob/main/src/main/resources) |
|Caching | [CacheConfiguration](https://github.com/spring-projects/spring-petclinic/blob/main/src/main/java/org/springframework/samples/petclinic/system/CacheConfiguration.java) |

## Interesting Spring Petclinic branches and forks

The Spring Petclinic "main" branch in the [spring-projects](https://github.com/spring-projects/spring-petclinic)
GitHub org is the "canonical" implementation based on Spring Boot and Thymeleaf. There are
[quite a few forks](https://spring-petclinic.github.io/docs/forks.html) in the GitHub org
[spring-petclinic](https://github.com/spring-petclinic). If you are interested in using a different technology stack to implement the Pet Clinic, please join the community there.

## Interaction with other open-source projects

One of the best parts about working on the Spring Petclinic application is that we have the opportunity to work in direct contact with many Open Source projects. We found bugs/suggested improvements on various topics such as Spring, Spring Data, Bean Validation and even Eclipse! In many cases, they've been fixed/implemented in just a few days.
Here is a list of them:

| Name | Issue |
|------|-------|
| Spring JDBC: simplify usage of NamedParameterJdbcTemplate | [SPR-10256](https://github.com/spring-projects/spring-framework/issues/14889) and [SPR-10257](https://github.com/spring-projects/spring-framework/issues/14890) |
| Bean Validation / Hibernate Validator: simplify Maven dependencies and backward compatibility |[HV-790](https://hibernate.atlassian.net/browse/HV-790) and [HV-792](https://hibernate.atlassian.net/browse/HV-792) |
| Spring Data: provide more flexibility when working with JPQL queries | [DATAJPA-292](https://github.com/spring-projects/spring-data-jpa/issues/704) |

## Contributing

The [issue tracker](https://github.com/spring-projects/spring-petclinic/issues) is the preferred channel for bug reports, feature requests and submitting pull requests.

For pull requests, editor preferences are available in the [editor config](.editorconfig) for easy use in common text editors. Read more and download plugins at <https://editorconfig.org>. All commits must include a __Signed-off-by__ trailer at the end of each commit message to indicate that the contributor agrees to the Developer Certificate of Origin.
For additional details, please refer to the blog post [Hello DCO, Goodbye CLA: Simplifying Contributions to Spring](https://spring.io/blog/2025/01/06/hello-dco-goodbye-cla-simplifying-contributions-to-spring).

## License

The Spring PetClinic sample application is released under version 2.0 of the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).
