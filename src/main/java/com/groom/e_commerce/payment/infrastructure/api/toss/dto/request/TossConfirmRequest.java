package com.groom.e_commerce.payment.infrastructure.api.toss.dto.request;

public record TossConfirmRequest(
	String paymentKey,
	String orderId,
	Long amount
) {
}
