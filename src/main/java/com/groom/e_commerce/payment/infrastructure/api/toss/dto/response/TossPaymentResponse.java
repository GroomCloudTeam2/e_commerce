package com.groom.e_commerce.payment.infrastructure.api.toss.dto.response;

import java.time.OffsetDateTime;

public record TossPaymentResponse(
	String paymentKey,
	String orderId,
	String orderName,
	String customerName,
	String method,
	String currency,
	Long totalAmount,
	String status,
	OffsetDateTime requestedAt,
	OffsetDateTime approvedAt
) {
}
