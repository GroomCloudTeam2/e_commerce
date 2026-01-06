package com.groom.e_commerce.global.infrastructure.client.OpenAi;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Component
public class OpenAiRestClient {

	private final RestClient restClient;
	private final String model;

	public OpenAiRestClient(
		@Qualifier("openAiHttpClient") RestClient restClient,
		@Value("${ai.openai.model}") String model
	) {
		this.restClient = restClient;
		this.model = model;
	}

	public String summarizeReviews(String prompt) {
		ChatCompletionResponse response = restClient.post()
			.uri("/v1/chat/completions")
			.body(new ChatCompletionRequest(
				model,
				List.of(
					new Message("system", "너는 이커머스 상품 리뷰 요약 전문가야."),
					new Message("user", prompt)
				),
				0.3
			))
			.retrieve()
			.body(ChatCompletionResponse.class);

		return response.getContent();
	}

	// ===== DTO =====
	@Getter
	@AllArgsConstructor
	static class ChatCompletionRequest {
		private String model;
		private List<Message> messages;
		private double temperature;
	}

	@Getter
	@AllArgsConstructor
	static class Message {
		private String role;
		private String content;
	}

	@Getter
	@NoArgsConstructor
	static class ChatCompletionResponse {
		private List<Choice> choices;

		String getContent() {
			return choices.get(0).message.content;
		}
	}

	@Getter
	@NoArgsConstructor
	static class Choice {
		private Message message;
	}
}
