package com.groom.e_commerce.payment.infrastructure.api.toss.client;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.groom.e_commerce.payment.infrastructure.api.toss.dto.request.TossCancelRequest;
import com.groom.e_commerce.payment.infrastructure.api.toss.dto.request.TossConfirmRequest;
import com.groom.e_commerce.payment.infrastructure.api.toss.dto.response.TossCancelResponse;
import com.groom.e_commerce.payment.infrastructure.api.toss.dto.response.TossErrorResponse;
import com.groom.e_commerce.payment.infrastructure.api.toss.dto.response.TossPaymentResponse;
import com.groom.e_commerce.payment.presentation.exception.TossApiException;

import reactor.core.publisher.Mono;

@Component
public class TossPaymentsClient {

	private final WebClient tossWebClient;
	private final String secretKey;

	public TossPaymentsClient(
		WebClient tossWebClient,
		@Value("${toss.payments.secret-key}") String secretKey
	) {
		this.tossWebClient = tossWebClient;
		this.secretKey = secretKey;
	}

	public TossPaymentResponse confirm(TossConfirmRequest request) {
		return tossWebClient.post()
			.uri("/v1/payments/confirm")
			.header(HttpHeaders.AUTHORIZATION, basicAuth(secretKey))
			.header("Idempotency-Key", UUID.randomUUID().toString())
			.contentType(MediaType.APPLICATION_JSON)
			.accept(MediaType.APPLICATION_JSON)
			.bodyValue(request)
			.retrieve()
			.onStatus(HttpStatusCode::isError, clientResponse ->
				clientResponse.bodyToMono(TossErrorResponse.class)
					.defaultIfEmpty(new TossErrorResponse("TOSS_ERROR", "토스 결제 승인 실패"))
					.flatMap(err -> Mono.error(new TossApiException(
						clientResponse.statusCode(),
						"TOSS_CONFIRM_FAILED",
						err.message(), // ✅ message
						err.code()     // ✅ tossErrorCode
					)))
			)
			.bodyToMono(TossPaymentResponse.class)
			.block();
	}

	/**
	 * 결제 조회 (PaymentKey 기준)
	 * GET /v1/payments/{paymentKey}
	 */
	public TossPaymentResponse getPayment(String paymentKey) {
		return tossWebClient.get()
			.uri("/v1/payments/{paymentKey}", paymentKey)
			.header(HttpHeaders.AUTHORIZATION, basicAuth(secretKey))
			.accept(MediaType.APPLICATION_JSON)
			.retrieve()
			.onStatus(HttpStatusCode::isError, clientResponse ->
				clientResponse.bodyToMono(TossErrorResponse.class)
					.defaultIfEmpty(new TossErrorResponse("TOSS_ERROR", "토스 결제 조회 실패"))
					.flatMap(err -> Mono.error(new TossApiException(
						clientResponse.statusCode(),
						"TOSS_GET_FAILED",
						err.message(), // ✅ message
						err.code()     // ✅ tossErrorCode
					)))
			)
			.bodyToMono(TossPaymentResponse.class)
			.block();
	}

	public TossCancelResponse cancel(String paymentKey, TossCancelRequest request) {
		return tossWebClient.post()
			.uri("/v1/payments/{paymentKey}/cancel", paymentKey)
			.header(HttpHeaders.AUTHORIZATION, basicAuth(secretKey))
			.header("Idempotency-Key", UUID.randomUUID().toString()) // ✅ 멱등키
			.contentType(MediaType.APPLICATION_JSON)
			.accept(MediaType.APPLICATION_JSON)
			.bodyValue(request)
			.retrieve()
			.onStatus(HttpStatusCode::isError, clientResponse ->
				clientResponse.bodyToMono(TossErrorResponse.class)
					.defaultIfEmpty(new TossErrorResponse("TOSS_ERROR", "토스 결제 취소 실패"))
					.flatMap(err -> Mono.error(new TossApiException(
						clientResponse.statusCode(),
						"TOSS_CANCEL_FAILED",
						err.message(), // ✅ message
						err.code()     // ✅ tossErrorCode
					)))
			)
			.bodyToMono(TossCancelResponse.class)
			.block();
	}

	private String basicAuth(String secretKey) {
		String raw = secretKey + ":";
		String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
		return "Basic " + encoded;
	}
}
