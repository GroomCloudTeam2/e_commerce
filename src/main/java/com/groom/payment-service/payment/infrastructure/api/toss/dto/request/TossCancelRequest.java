package com.groom.e_commerce.payment.infrastructure.api.toss.dto.request;

public record TossCancelRequest(
	String cancelReason,
	Long cancelAmount
) {
}
