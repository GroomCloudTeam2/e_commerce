package com.groom.e_commerce.payment.presentation.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record ReqConfirmPaymentV1(
	@NotBlank String paymentKey,
	@NotNull UUID orderId,
	@NotNull @Min(1) Long amount
) {}
