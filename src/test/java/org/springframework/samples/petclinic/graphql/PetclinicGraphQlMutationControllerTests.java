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
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.ResponseError;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.samples.petclinic.owner.PetTypeRepository;
import org.springframework.samples.petclinic.owner.Visit;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.graphql.execution.ErrorType.BAD_REQUEST;
import static org.springframework.graphql.execution.ErrorType.NOT_FOUND;

/**
 * Test class for the {@link PetclinicGraphQlMutationController}. The repositories are
 * mocks, so each test gives a new object its id the way the database would.
 */
@GraphQlTest(PetclinicGraphQlMutationController.class)
@DisabledInNativeImage
@DisabledInAotMode
class PetclinicGraphQlMutationControllerTests {

	private static final String ADD_OWNER = """
			mutation($firstName: String!, $telephone: String!) {
			  addOwner(input: {firstName: $firstName, lastName: "Peeters", address: "200 E Colfax Ave",
			      city: "Denver", telephone: $telephone}) {
			    id firstName lastName address city telephone pets { name }
			  }
			}""";

	private static final String UPDATE_OWNER = """
			mutation($id: ID!) {
			  updateOwner(input: {id: $id, firstName: "George", lastName: "Franklin", address: "1701 Wynkoop St",
			      city: "Denver", telephone: "7205550001"}) {
			    id address city telephone pets { name }
			  }
			}""";

	private static final String ADD_PET = """
			mutation($name: String!, $birthDate: Date!, $typeId: ID!) {
			  addPet(input: {ownerId: 1, name: $name, birthDate: $birthDate, typeId: $typeId}) {
			    id name birthDate type { name } visits { id }
			  }
			}""";

	private static final String UPDATE_PET = """
			mutation($name: String!) {
			  updatePet(input: {id: 1, name: $name, birthDate: "2011-01-02", typeId: 2}) {
			    id name birthDate type { name }
			  }
			}""";

	private static final String ADD_VISIT = """
			mutation($date: Date, $description: String!) {
			  addVisit(input: {petId: 1, date: $date, description: $description}) {
			    id date description
			  }
			}""";

	@Autowired
	private GraphQlTester graphQl;

	@MockitoBean
	private OwnerRepository owners;

	@MockitoBean
	private PetTypeRepository petTypes;

	private Owner george;

	@BeforeEach
	void setup() {
		PetType cat = petType(1, "cat");
		Pet leo = new Pet();
		leo.setId(1);
		leo.setName("Leo");
		leo.setType(cat);
		leo.setBirthDate(LocalDate.of(2010, 9, 7));
		this.george = new Owner();
		this.george.setId(1);
		this.george.setFirstName("George");
		this.george.setLastName("Franklin");
		this.george.setAddress("110 W. Liberty St.");
		this.george.setCity("Madison");
		this.george.setTelephone("6085551023");
		this.george.addPet(leo);

		given(this.owners.findById(1)).willReturn(Optional.of(this.george));
		given(this.owners.findByPetsId(1)).willReturn(Optional.of(this.george));
		given(this.petTypes.findById(1)).willReturn(Optional.of(cat));
		given(this.petTypes.findById(2)).willReturn(Optional.of(petType(2, "dog")));
		given(this.owners.save(any(Owner.class))).willAnswer((invocation) -> {
			Owner owner = invocation.getArgument(0);
			if (owner.isNew()) {
				owner.setId(11);
			}
			return owner;
		});
		willAnswer((invocation) -> {
			for (Pet pet : this.george.getPets()) {
				if (pet.isNew()) {
					pet.setId(14);
				}
				for (Visit visit : pet.getVisits()) {
					if (visit.isNew()) {
						visit.setId(5);
					}
				}
			}
			return null;
		}).given(this.owners).flush();
	}

	private static PetType petType(int id, String name) {
		PetType type = new PetType();
		type.setId(id);
		type.setName(name);
		return type;
	}

	private static void assertBadRequestFor(List<ResponseError> errors, String... fields) {
		assertThat(errors).allSatisfy((error) -> assertThat(error.getErrorType()).isEqualTo(BAD_REQUEST));
		assertThat(errors).extracting((error) -> error.getExtensions().get("field"))
			.containsExactlyInAnyOrder((Object[]) fields);
		// each message starts with the name of its field
		assertThat(errors).allSatisfy(
				(error) -> assertThat(error.getMessage()).startsWith(error.getExtensions().get("field") + ": "));
	}

	private static void assertNotFound(List<ResponseError> errors, String message) {
		assertThat(errors).singleElement().satisfies((error) -> {
			assertThat(error.getErrorType()).isEqualTo(NOT_FOUND);
			assertThat(error.getMessage()).isEqualTo(message);
		});
	}

	@Test
	void addOwnerSavesTheOwnerAndReturnsItWithItsId() {
		this.graphQl.document(ADD_OWNER)
			.variable("firstName", "Lotte")
			.variable("telephone", "7205550001")
			.execute()
			.path("addOwner.id")
			.entity(String.class)
			.isEqualTo("11")
			.path("addOwner.firstName")
			.entity(String.class)
			.isEqualTo("Lotte")
			.path("addOwner.city")
			.entity(String.class)
			.isEqualTo("Denver")
			.path("addOwner.pets")
			.entityList(Object.class)
			.hasSize(0);
	}

	@Test
	void addOwnerReportsEveryInvalidField() {
		this.graphQl.document(ADD_OWNER)
			.variable("firstName", " ")
			.variable("telephone", "123")
			.execute()
			.errors()
			.satisfy((errors) -> assertBadRequestFor(errors, "firstName", "telephone"));

		verify(this.owners, never()).save(any(Owner.class));
	}

	@Test
	void updateOwnerReplacesTheDetailsAndKeepsThePets() {
		this.graphQl.document(UPDATE_OWNER)
			.variable("id", "1")
			.execute()
			.path("updateOwner.address")
			.entity(String.class)
			.isEqualTo("1701 Wynkoop St")
			.path("updateOwner.telephone")
			.entity(String.class)
			.isEqualTo("7205550001")
			.path("updateOwner.pets[*].name")
			.entityList(String.class)
			.containsExactly("Leo");

		verify(this.owners).save(this.george);
	}

	@Test
	void updateOwnerReportsAnUnknownId() {
		given(this.owners.findById(99)).willReturn(Optional.empty());

		this.graphQl.document(UPDATE_OWNER)
			.variable("id", "99")
			.execute()
			.errors()
			.satisfy((errors) -> assertNotFound(errors, "Owner 99 does not exist"));
		this.graphQl.document(UPDATE_OWNER)
			.variable("id", "abc")
			.execute()
			.errors()
			.satisfy((errors) -> assertNotFound(errors, "Owner abc does not exist"));

		verify(this.owners, never()).save(any(Owner.class));
	}

	@Test
	void addPetAddsThePetToItsOwner() {
		this.graphQl.document(ADD_PET)
			.variable("name", "Bobbie")
			.variable("birthDate", "2020-05-01")
			.variable("typeId", "2")
			.execute()
			.path("addPet.id")
			.entity(String.class)
			.isEqualTo("14")
			.path("addPet.birthDate")
			.entity(String.class)
			.isEqualTo("2020-05-01")
			.path("addPet.type.name")
			.entity(String.class)
			.isEqualTo("dog");

		assertThat(this.george.getPets()).extracting(Pet::getName).containsExactly("Leo", "Bobbie");
		verify(this.owners).flush();
	}

	@Test
	void addPetRefusesANameTheOwnerAlreadyUsesAndABirthDateAfterToday() {
		this.graphQl.document(ADD_PET)
			.variable("name", "leo")
			.variable("birthDate", LocalDate.now().plusDays(1).toString())
			.variable("typeId", "2")
			.execute()
			.errors()
			.satisfy((errors) -> assertBadRequestFor(errors, "name", "birthDate"));

		assertThat(this.george.getPets()).hasSize(1);
		verify(this.owners, never()).flush();
	}

	@Test
	void addPetRefusesABlankNameAndANameOverThirtyCharacters() {
		for (String name : List.of(" ", "a".repeat(31))) {
			this.graphQl.document(ADD_PET)
				.variable("name", name)
				.variable("birthDate", "2020-05-01")
				.variable("typeId", "2")
				.execute()
				.errors()
				.satisfy((errors) -> assertBadRequestFor(errors, "name"));
		}

		verify(this.owners, never()).flush();
	}

	@Test
	void addPetReportsAnUnknownPetType() {
		given(this.petTypes.findById(77)).willReturn(Optional.empty());

		this.graphQl.document(ADD_PET)
			.variable("name", "Bobbie")
			.variable("birthDate", "2020-05-01")
			.variable("typeId", "77")
			.execute()
			.errors()
			.satisfy((errors) -> assertNotFound(errors, "Pet type 77 does not exist"));

		verify(this.owners, never()).flush();
	}

	@Test
	void updatePetReplacesTheDetailsAndMayKeepItsOwnName() {
		this.graphQl.document(UPDATE_PET)
			.variable("name", "Leo")
			.execute()
			.path("updatePet.id")
			.entity(String.class)
			.isEqualTo("1")
			.path("updatePet.name")
			.entity(String.class)
			.isEqualTo("Leo")
			.path("updatePet.birthDate")
			.entity(String.class)
			.isEqualTo("2011-01-02")
			.path("updatePet.type.name")
			.entity(String.class)
			.isEqualTo("dog");

		verify(this.owners).flush();
	}

	@Test
	void updatePetRefusesTheNameOfAnotherPetOfTheOwner() {
		Pet max = new Pet();
		max.setId(2);
		max.setName("Max");
		this.george.addPet(max);

		this.graphQl.document(UPDATE_PET)
			.variable("name", "max")
			.execute()
			.errors()
			.satisfy((errors) -> assertBadRequestFor(errors, "name"));

		verify(this.owners, never()).flush();
	}

	@Test
	void updatePetReportsAnUnknownId() {
		given(this.owners.findByPetsId(1)).willReturn(Optional.empty());

		this.graphQl.document(UPDATE_PET)
			.variable("name", "Leo")
			.execute()
			.errors()
			.satisfy((errors) -> assertNotFound(errors, "Pet 1 does not exist"));
	}

	@Test
	void addVisitBooksTomorrowWhenTheDateIsLeftOut() {
		this.graphQl.document(ADD_VISIT)
			.variable("description", "vaccination")
			.execute()
			.path("addVisit.id")
			.entity(String.class)
			.isEqualTo("5")
			.path("addVisit.date")
			.entity(String.class)
			.isEqualTo(LocalDate.now().plusDays(1).toString())
			.path("addVisit.description")
			.entity(String.class)
			.isEqualTo("vaccination");

		assertThat(this.george.getPet(1).getVisits()).hasSize(1);
	}

	@Test
	void addVisitRefusesTodayAndABlankDescription() {
		this.graphQl.document(ADD_VISIT)
			.variable("date", LocalDate.now().toString())
			.variable("description", " ")
			.execute()
			.errors()
			.satisfy((errors) -> assertBadRequestFor(errors, "date", "description"));

		assertThat(this.george.getPet(1).getVisits()).isEmpty();
		verify(this.owners, never()).flush();
	}

}
