package com.groom.e_commerce.global.infrastructure.client.Classification;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.e_commerce.review.domain.entity.ReviewCategory;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Component
public class AiRestClient {

	private final RestClient restClient;
	private final ObjectMapper objectMapper;

	public AiRestClient(
		@Qualifier("classificationHttpClient") RestClient restClient,
		ObjectMapper objectMapper
	) {
		this.restClient = restClient;
		this.objectMapper = objectMapper;
	}

	public AiResponse classifyComment(String comment) {
		try {
			String response = restClient.post()
				.uri("/infer")
				.body(new AiRequest(comment))
				.retrieve()
				.body(String.class);

			var jsonNode = objectMapper.readTree(response);

			String aiCategory = jsonNode.get("category").asText();
			double confidence = jsonNode.get("confidence").asDouble();

			return new AiResponse(aiCategory, confidence);

		} catch (RestClientResponseException e) {
			return new AiResponse(ReviewCategory.ERR, 0.0);
		} catch (Exception e) {
			return new AiResponse(ReviewCategory.ERR, 0.0);
		}
	}

	// ===== DTO =====
	@Getter
	@Setter
	@AllArgsConstructor
	@NoArgsConstructor
	public static class AiRequest {
		private String comment;
	}

	@Getter
	@Setter
	@AllArgsConstructor
	@NoArgsConstructor
	public static class AiResponse {
		private ReviewCategory category;
		private double confidence;

		public AiResponse(String aiCategory, double confidence) {
			this.category = switch (aiCategory) {
				case "디자인/외형" -> ReviewCategory.DESIGN;
				case "성능/기능" -> ReviewCategory.PERFORMANCE;
				case "편의성/사용감" -> ReviewCategory.CONVENIENCE;
				case "가격/구성" -> ReviewCategory.PRICE;
				case "품질/내구성" -> ReviewCategory.QUALITY;
				default -> ReviewCategory.ERR;
			};
			this.confidence = confidence;
		}
	}
}
