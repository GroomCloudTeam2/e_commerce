package com.groom.e_commerce.global.infrastructure.client;

import com.groom.e_commerce.review.domain.entity.ReviewCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class AiWebClient {

	private final WebClient webClient;

	// WebClientConfig에서 생성된 Bean을 주입받음
	public AiWebClient(WebClient aiWebClientInstance) {
		this.webClient = aiWebClientInstance;
	}

	public AiResponse classifyComment(String comment) {
		try {
			return webClient.post()
				.uri("/infer")
				.bodyValue(new AiRequest(comment))
				.retrieve()
				.bodyToMono(AiResponse.class)
				.block();
		} catch (WebClientResponseException e) {
			// 실제 서비스에서는 로깅 프레임워크(slf4j) 사용을 추천합니다.
			return new AiResponse(ReviewCategory.ERR, 0.0);
		}
	}

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
		private ReviewCategory category; // String 대신 도메인 Entity의 Enum 사용
		private double confidence;
	}
}
