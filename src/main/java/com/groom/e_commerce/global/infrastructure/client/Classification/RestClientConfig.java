package com.groom.e_commerce.global.infrastructure.client.Classification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

	@Value("${ai.url:http://localhost:8000}")
	private String aiBaseUrl;

	@Bean
	public RestClient classificationHttpClient() {
		return RestClient.builder()
			.baseUrl(aiBaseUrl)
			.build();
	}
}
