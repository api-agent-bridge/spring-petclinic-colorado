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

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.graphql.execution.ErrorType.BAD_REQUEST;

/**
 * Runs GraphQL mutations against the H2 database and reads the result back with a query.
 * The tests change the sample data, so the application context is discarded after this
 * class and other test classes start from the original data.
 */
@SpringBootTest
@AutoConfigureGraphQlTester
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
class PetclinicGraphQlMutationIntegrationTests {

	@Autowired
	private GraphQlTester graphQl;

	@Test
	void addOwnerCanBeReadBack() {
		String id = this.graphQl.document("""
				mutation {
				  addOwner(input: {firstName: "Lotte", lastName: "Peeters", address: "200 E Colfax Ave",
				      city: "Denver", telephone: "7205550001"}) { id }
				}""").execute().path("addOwner.id").entity(String.class).get();

		this.graphQl.document("query($id: ID!) { owner(id: $id) { firstName lastName address city telephone } }")
			.variable("id", id)
			.execute()
			.path("owner.lastName")
			.entity(String.class)
			.isEqualTo("Peeters")
			.path("owner.city")
			.entity(String.class)
			.isEqualTo("Denver");
	}

	@Test
	void updateOwnerReplacesTheDetails() {
		this.graphQl.document("""
				mutation {
				  updateOwner(input: {id: 10, firstName: "Carlos", lastName: "Estaban", address: "1701 Wynkoop St",
				      city: "Denver", telephone: "7205550002"}) { id address }
				}""").execute().path("updateOwner.address").entity(String.class).isEqualTo("1701 Wynkoop St");

		this.graphQl.document("{ owner(id: 10) { address city telephone pets { name } } }")
			.execute()
			.path("owner.city")
			.entity(String.class)
			.isEqualTo("Denver")
			.path("owner.telephone")
			.entity(String.class)
			.isEqualTo("7205550002")
			.path("owner.pets[*].name")
			.entityList(String.class)
			.containsExactly("Lucky", "Sly");
	}

	@Test
	void updateOwnerWithAnInvalidTelephoneLeavesTheOwnerAsItWas() {
		this.graphQl.document("""
				mutation {
				  updateOwner(input: {id: 3, firstName: "Changed", lastName: "Changed", address: "Changed",
				      city: "Changed", telephone: "abc"}) { id }
				}""").execute().errors().satisfy((errors) -> assertThat(errors).singleElement().satisfies((error) -> {
			assertThat(error.getErrorType()).isEqualTo(BAD_REQUEST);
			assertThat(error.getExtensions()).containsEntry("field", "telephone");
		}));

		this.graphQl.document("{ owner(id: 3) { firstName lastName city telephone } }")
			.execute()
			.path("owner.firstName")
			.entity(String.class)
			.isEqualTo("Eduardo")
			.path("owner.telephone")
			.entity(String.class)
			.isEqualTo("6085558763");
	}

	@Test
	void addPetReturnsThePetWithItsIdAndCanBeReadBack() {
		this.graphQl.document("""
				mutation {
				  addPet(input: {ownerId: 2, name: "Bobbie", birthDate: "2020-05-01", typeId: 2}) {
				    id name birthDate type { name } visits { id }
				  }
				}""")
			.execute()
			.path("addPet.id")
			.entity(String.class)
			.satisfies((id) -> assertThat(id).isNotBlank())
			.path("addPet.type.name")
			.entity(String.class)
			.isEqualTo("dog");

		this.graphQl.document("{ owner(id: 2) { pets { name } } }")
			.execute()
			.path("owner.pets[*].name")
			.entityList(String.class)
			.containsExactly("Basil", "Bobbie");
	}

	@Test
	void addPetRefusesANameTheOwnerAlreadyUses() {
		this.graphQl.document("""
				mutation {
				  addPet(input: {ownerId: 1, name: "LEO", birthDate: "2020-05-01", typeId: 1}) { id }
				}""").execute().errors().satisfy((errors) -> assertThat(errors).singleElement().satisfies((error) -> {
			assertThat(error.getErrorType()).isEqualTo(BAD_REQUEST);
			assertThat(error.getExtensions()).containsEntry("field", "name");
		}));

		this.graphQl.document("{ owner(id: 1) { pets { name } } }")
			.execute()
			.path("owner.pets[*].name")
			.entityList(String.class)
			.containsExactly("Leo");
	}

	@Test
	void updatePetReplacesTheDetails() {
		this.graphQl.document("""
				mutation {
				  updatePet(input: {id: 5, name: "Iggy Junior", birthDate: "2011-12-01", typeId: 4}) {
				    id name birthDate type { name }
				  }
				}""").execute().path("updatePet.type.name").entity(String.class).isEqualTo("snake");

		this.graphQl.document("{ owner(id: 4) { pets { id name birthDate type { name } } } }")
			.execute()
			.path("owner.pets[0].id")
			.entity(String.class)
			.isEqualTo("5")
			.path("owner.pets[0].name")
			.entity(String.class)
			.isEqualTo("Iggy Junior")
			.path("owner.pets[0].birthDate")
			.entity(String.class)
			.isEqualTo("2011-12-01");
	}

	@Test
	void addVisitReturnsTheVisitWithItsIdAndCanBeReadBack() {
		String date = LocalDate.now().plusDays(7).toString();

		this.graphQl.document("""
				mutation($date: Date) {
				  addVisit(input: {petId: 6, date: $date, description: "vaccination"}) { id date description }
				}""")
			.variable("date", date)
			.execute()
			.path("addVisit.id")
			.entity(String.class)
			.satisfies((id) -> assertThat(id).isNotBlank())
			.path("addVisit.date")
			.entity(String.class)
			.isEqualTo(date);

		this.graphQl.document("{ owner(id: 5) { pets { name visits { date description } } } }")
			.execute()
			.path("owner.pets[0].name")
			.entity(String.class)
			.isEqualTo("George")
			.path("owner.pets[0].visits[*].description")
			.entityList(String.class)
			.containsExactly("vaccination");
	}

	@Test
	void addVisitRefusesADayThatIsNotAfterToday() {
		this.graphQl.document("""
				mutation($date: Date) {
				  addVisit(input: {petId: 6, date: $date, description: "too late"}) { id }
				}""")
			.variable("date", LocalDate.now().toString())
			.execute()
			.errors()
			.satisfy((errors) -> assertThat(errors).singleElement().satisfies((error) -> {
				assertThat(error.getErrorType()).isEqualTo(BAD_REQUEST);
				assertThat(error.getExtensions()).containsEntry("field", "date");
			}));
	}

}
