package com.groom.e_commerce.global.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

	@Value("${ai.url:http://localhost:8000}")
	private String aiBaseUrl;

	@Bean
	public WebClient aiWebClientInstance(WebClient.Builder builder) {
		return builder
			.baseUrl(aiBaseUrl)
			.build();
	}

	@Bean
	public AiWebClient aiWebClient(WebClient aiWebClientInstance) {
		return new AiWebClient(aiWebClientInstance);
	}
}
