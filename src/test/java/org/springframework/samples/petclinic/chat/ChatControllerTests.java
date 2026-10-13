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

package org.springframework.samples.petclinic.chat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Posts questions to the chat, with a stub in place of the Ollama model that records each
 * request and answers with a fixed text.
 */
@SpringBootTest(properties = "spring.ai.model.chat=none")
@AutoConfigureMockMvc
class ChatControllerTests {

	private static final String ANSWER = "George Franklin lives at 110 W. Liberty St. in Madison.";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private RecordingChatModel model;

	@Test
	void answersWithTheCuratedOperationsAsTools() throws Exception {
		this.mvc.perform(post("/chat").contentType(MediaType.TEXT_PLAIN).content("Where does George Franklin live?"))
			.andExpect(status().isOk())
			.andExpect(content().string(ANSWER));

		ToolCallingChatOptions options = (ToolCallingChatOptions) this.model.lastPrompt().getOptions();
		assertThat(options.getToolCallbacks()).extracting((tool) -> tool.getToolDefinition().name())
			.containsExactlyInAnyOrder("findOwners", "findPets", "getOwnerDetails", "listPetTypes", "listVets",
					"addOwner", "addPet", "addVisit", "updateOwner", "updatePet");
	}

	@Test
	void keepsOneConversationPerSession() throws Exception {
		MockHttpSession session = new MockHttpSession();
		ask(session, "Where does George Franklin live?");
		ask(session, "Which pets does he have?");
		assertThat(this.model.lastPromptTexts()).contains("Where does George Franklin live?", ANSWER,
				"Which pets does he have?");

		ask(new MockHttpSession(), "Which vets work at the clinic?");
		assertThat(this.model.lastPromptTexts()).doesNotContain("Where does George Franklin live?");
	}

	@Test
	void rejectsAnEmptyQuestion() throws Exception {
		this.mvc.perform(post("/chat").contentType(MediaType.TEXT_PLAIN).content(""))
			.andExpect(status().isBadRequest());
	}

	private void ask(MockHttpSession session, String question) throws Exception {
		this.mvc.perform(post("/chat").session(session).contentType(MediaType.TEXT_PLAIN).content(question))
			.andExpect(status().isOk());
	}

	@TestConfiguration
	static class StubModelConfiguration {

		@Bean
		RecordingChatModel recordingChatModel() {
			return new RecordingChatModel();
		}

	}

	static class RecordingChatModel implements ChatModel {

		private final List<Prompt> prompts = new CopyOnWriteArrayList<>();

		// ChatClient adds the tools only to options that can carry them
		@Override
		public ChatOptions getOptions() {
			return ToolCallingChatOptions.builder().build();
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			this.prompts.add(prompt);
			return new ChatResponse(List.of(new Generation(new AssistantMessage(ANSWER))));
		}

		Prompt lastPrompt() {
			return this.prompts.get(this.prompts.size() - 1);
		}

		List<String> lastPromptTexts() {
			return lastPrompt().getInstructions().stream().map(Message::getText).toList();
		}

	}

}
