package com.groom.e_commerce.payment.infrastructure.api.toss.dto.response;

import java.time.OffsetDateTime;

public record TossCancelResponse(
	String paymentKey,
	String status,
	OffsetDateTime canceledAt
) {
}
