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

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs GraphQL queries against the sample data of the H2 database.
 */
@SpringBootTest
@AutoConfigureGraphQlTester
class PetclinicGraphQlIntegrationTests {

	@Autowired
	private GraphQlTester graphQl;

	@Value("${petclinic.graphql.max-query-complexity}")
	private int maxQueryComplexity;

	@Test
	void ownerWithPetsAndVisits() {
		this.graphQl.document("""
				{
				  owner(id: 6) {
				    firstName lastName city
				    pets { name birthDate type { name } visits { date description } }
				  }
				}""")
			.execute()
			.path("owner.lastName")
			.entity(String.class)
			.isEqualTo("Coleman")
			.path("owner.pets[*].name")
			.entityList(String.class)
			.containsExactly("Max", "Samantha")
			.path("owner.pets[0].type.name")
			.entity(String.class)
			.isEqualTo("cat")
			.path("owner.pets[0].visits[*].date")
			.entityList(String.class)
			.containsExactly("2013-01-02", "2013-01-03");
	}

	@Test
	void ownersByLastName() {
		this.graphQl.document("{ owners(lastName: \"Davis\") { totalOwners owners { firstName } } }")
			.execute()
			.path("owners.totalOwners")
			.entity(Integer.class)
			.isEqualTo(2)
			.path("owners.owners[*].firstName")
			.entityList(String.class)
			.hasSize(2)
			.contains("Betty", "Harold");
	}

	@Test
	void ownersByFirstAndLastName() {
		this.graphQl.document("""
				{
				  je: owners(firstName: "Je") { owners { firstName } }
				  harold: owners(firstName: "H", lastName: "Davis") { totalOwners owners { firstName } }
				}""")
			.execute()
			.path("je.owners[*].firstName")
			.entityList(String.class)
			.containsExactly("Jean", "Jeff")
			.path("harold.totalOwners")
			.entity(Integer.class)
			.isEqualTo(1)
			.path("harold.owners[*].firstName")
			.entityList(String.class)
			.containsExactly("Harold");
	}

	@Test
	void petsByNameAndType() {
		this.graphQl.document("""
				{
				  lucky: pets(name: "Lu") { pets { id type { name } } }
				  dogs: pets(type: "dog") { totalPets pets { name } }
				  luckyDog: pets(name: "Lu", type: "dog") { pets { id } }
				}""")
			.execute()
			.path("lucky.pets[*].type.name")
			.entityList(String.class)
			.containsExactly("bird", "dog")
			.path("dogs.totalPets")
			.entity(Integer.class)
			.isEqualTo(5)
			.path("dogs.pets[*].name")
			.entityList(String.class)
			.containsExactly("Rosy", "Jewel", "Mulligan", "Lucky", "Woofy")
			.path("luckyDog.pets[*].id")
			.entityList(String.class)
			.containsExactly("12");
	}

	@Test
	void petsWithTheirOwners() {
		this.graphQl.document("{ pets(name: \"Lucky\") { pets { id owner { firstName lastName } } } }")
			.execute()
			.path("pets.pets[*].id")
			.entityList(String.class)
			.containsExactly("9", "12")
			.path("pets.pets[*].owner.lastName")
			.entityList(String.class)
			.containsExactly("Black", "Estaban");
	}

	@Test
	void petsWithTheirLastVisit() {
		this.graphQl.document("""
				{
				  samantha: pets(name: "Samantha") { pets { lastVisit { date description } } }
				  leo: pets(name: "Leo") { pets { lastVisit { date } } }
				}""")
			.execute()
			.path("samantha.pets[0].lastVisit.date")
			.entity(String.class)
			.isEqualTo("2013-01-04")
			.path("samantha.pets[0].lastVisit.description")
			.entity(String.class)
			.isEqualTo("spayed")
			.path("leo.pets[0].lastVisit")
			.valueIsNull();
	}

	@Test
	void nameAndTypeFiltersIgnoreCase() {
		this.graphQl.document("""
				{
				  owners(firstName: "h", lastName: "DAVIS") { owners { firstName } }
				  pets(name: "lU", type: "Dog") { pets { id } }
				}""")
			.execute()
			.path("owners.owners[*].firstName")
			.entityList(String.class)
			.containsExactly("Harold")
			.path("pets.pets[*].id")
			.entityList(String.class)
			.containsExactly("12");
	}

	@Test
	void ownersPagesFollowTheOrderOfTheIds() {
		this.graphQl.document("""
				{
				  first: owners(page: 1, size: 4) { owners { id } }
				  second: owners(page: 2, size: 4) { owners { id } }
				}""")
			.execute()
			.path("first.owners[*].id")
			.entityList(String.class)
			.containsExactly("1", "2", "3", "4")
			.path("second.owners[*].id")
			.entityList(String.class)
			.containsExactly("5", "6", "7", "8");
	}

	@Test
	void vetsWithSpecialties() {
		this.graphQl.document("{ vets { lastName specialties { name } } }")
			.execute()
			.path("vets[?(@.lastName == 'Douglas')].specialties[*].name")
			.entityList(String.class)
			.containsExactly("dentistry", "surgery");
	}

	@Test
	void petTypesSortedByName() {
		this.graphQl.document("{ petTypes { name } }")
			.execute()
			.path("petTypes[*].name")
			.entityList(String.class)
			.containsExactly("bird", "cat", "dog", "hamster", "lizard", "snake");
	}

	@Test
	void refusesAQueryThatRepeatsAFieldPastTheLimit() {
		// each alias selects two fields, so this query selects twice the limit
		String aliases = IntStream.range(0, this.maxQueryComplexity)
			.mapToObj((index) -> "a" + index + ": petTypes { name }")
			.collect(Collectors.joining(" "));

		this.graphQl.document("{ " + aliases + " }")
			.execute()
			.errors()
			.satisfy((errors) -> assertThat(errors).singleElement()
				.satisfies((error) -> assertThat(error.getMessage()).isEqualTo("maximum query complexity exceeded "
						+ (2 * this.maxQueryComplexity) + " > " + this.maxQueryComplexity)));
	}

	@Test
	void acceptsTheQueryThatSelectsEveryFieldOfTheSchema() {
		this.graphQl.document("""
				{
				  owners(size: 50) {
				    page totalPages totalOwners
				    owners {
				      id firstName lastName address city telephone
				      pets { id name birthDate type { id name } visits { id date description } }
				    }
				  }
				  pets(size: 50) {
				    page totalPages totalPets
				    pets {
				      id name birthDate type { id name } owner { id }
				      visits { id date description } lastVisit { id }
				    }
				  }
				  vets { id firstName lastName specialties { id name } }
				  petTypes { id name }
				}""")
			.execute()
			.path("owners.totalOwners")
			.entity(Integer.class)
			.isEqualTo(11)
			.path("pets.totalPets")
			.entity(Integer.class)
			.isEqualTo(14);
	}

}
