package com.groom.e_commerce.global.infrastructure.client.OpenAi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenAiRestClientConfig {

	@Bean
	public RestClient openAiHttpClient(
		@Value("${ai.openai.base-url}") String baseUrl,
		@Value("${ai.openai.api-key}") String apiKey
	) {
		return RestClient.builder()
			.baseUrl(baseUrl)
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.build();
	}
}
