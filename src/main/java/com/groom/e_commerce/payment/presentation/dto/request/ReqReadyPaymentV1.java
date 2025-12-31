package com.groom.e_commerce.payment.presentation.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReqReadyPaymentV1(
	@NotNull UUID orderId,
	@NotNull @Min(1) Long amount
) {
}
