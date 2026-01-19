package com.groom.e_commerce.payment.infrastructure.api.toss.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toss.payments")
public record TossPaymentsProperties(
	String baseUrl,
	String secretKey,
	String clientKey,
	String successUrl,
	String failUrl,
	Integer connectTimeoutMs,
	Integer readTimeoutMs
) {
}
