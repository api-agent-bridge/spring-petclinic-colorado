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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import io.gatool.boot.inprocess.GAToolCallbacks;
import jakarta.servlet.http.HttpSession;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Answers the chat in the web pages. The model gets GATool's tools in-process, so it
 * works with the clinic's data through the same tools that the MCP server serves. Each
 * HTTP session keeps its own conversation.
 * <p>
 * The tools stay with this client. A {@code ToolCallbackProvider} bean would also reach
 * the MCP server, where GATool already serves tools of the same names.
 */
@RestController
class ChatController {

	private static final DateTimeFormatter TODAY = DateTimeFormatter.ofPattern("EEEE, yyyy-MM-dd", Locale.ENGLISH);

	private final ChatClient chatClient;

	ChatController(ChatClient.Builder builder, ChatMemory memory, GAToolCallbacks tools) {
		this.chatClient = builder.defaultTools(tools.toolCallbackProvider())
			.defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
			.build();
	}

	@PostMapping("/chat")
	String chat(@RequestBody String question, HttpSession session) {
		// the system prompt of the evals in evals/mcp-agent.mjs, with a request for plain
		// text, because the chat shows the answer as plain text
		String system = "You are the assistant of a veterinary clinic. You can use the tools to read and change "
				+ "the clinic's data. Today is " + LocalDate.now().format(TODAY) + ". Answer in plain text, "
				+ "without Markdown.";
		return this.chatClient.prompt()
			.system(system)
			.user(question)
			.advisors((advisor) -> advisor.param(ChatMemory.CONVERSATION_ID, session.getId()))
			.call()
			.content();
	}

}
