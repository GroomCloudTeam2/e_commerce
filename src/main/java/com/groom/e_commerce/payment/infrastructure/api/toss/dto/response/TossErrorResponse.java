package com.groom.e_commerce.payment.infrastructure.api.toss.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossErrorResponse(
	String code,
	String message
) {
}
