package com.groom.e_commerce.payment.infrastructure.api.toss.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;

@Configuration
public class TossWebClientConfig {

	@Bean
	public WebClient tossWebClient(TossPaymentsProperties props) {
		HttpClient httpClient = HttpClient.create()
			.responseTimeout(Duration.ofMillis(props.readTimeoutMs() == null ? 5000 : props.readTimeoutMs()));

		return WebClient.builder()
			.baseUrl(props.baseUrl())
			.clientConnector(new ReactorClientHttpConnector(httpClient))
			.build();
	}
}
